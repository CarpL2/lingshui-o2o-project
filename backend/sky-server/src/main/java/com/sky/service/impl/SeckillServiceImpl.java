package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.SeckillDishDTO;
import com.sky.entity.Seckill;
import com.sky.entity.SeckillVoucher;
import com.sky.mapper.SeckillMapper;
import com.sky.mapper.SeckillVoucherMapper;
import com.sky.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.sky.config.RabbitMQConfig;
import com.alibaba.fastjson.JSON;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class SeckillServiceImpl implements SeckillService {

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    // 必须用 StringRedisTemplate，防止序列化乱码导致 Lua 读取失败
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SeckillMapper seckillMapper;
    @Autowired
    private SeckillVoucherMapper seckillVoucherMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void addSeckill(SeckillDishDTO dto) {
        Seckill seckill = new Seckill();
        BeanUtils.copyProperties(dto, seckill);
        seckill.setStatus(Seckill.SECKILL_ON);
        seckill.setCreateTime(LocalDateTime.now());
        seckill.setUpdateTime(LocalDateTime.now());
        seckillMapper.addSeckill(seckill);

        // 存入预热库存
        String key = "seckill:stock:" + seckill.getId();
        stringRedisTemplate.opsForValue().set(key, String.valueOf(dto.getStockCount()));

        // 手动删除列表缓存，确保 consistency
        stringRedisTemplate.delete("seckill::list");
    }

    @Override
    public Map<String, Object> verifyVoucher(String code) {
        SeckillVoucher voucher = seckillVoucherMapper.getByCode(code);

        if (voucher == null) {
            throw new RuntimeException("核销码不存在！");
        }
        if (voucher.getStatus() == 1) {
            throw new RuntimeException("该券码已被使用，请勿重复核销！");
        }

        voucher.setStatus(1);
        voucher.setUseTime(LocalDateTime.now());
        seckillVoucherMapper.update(voucher);

        // 3. 拿着 dishId 去查具体的菜品信息 (复用之前的 getById)
        Long seckillId = voucher.getSeckillDishId();
        Seckill seckill = seckillMapper.getById(seckillId);

        // 4. 组装返回数据
        Map<String, Object> map = new HashMap<>();
        map.put("dishName", seckill != null ? seckill.getName() : "未知菜品");
        map.put("price", seckill != null ? seckill.getSeckillPrice() : "0.00");
        map.put("userId", voucher.getUserId()); // 这里返回的就是真实的 UserID 了
        map.put("useTime", LocalDateTime.now());

        return map;
    }

    @Override
    public String grabSeckill(Long seckillId) {
        Long userId = BaseContext.getCurrentId();
        Seckill seckill = seckillMapper.getById(seckillId); // 需要在 Mapper 里加个 getById

        // 如果没查到，或者状态不对
        if (seckill == null || seckill.getStatus() != 1) {
            throw new RuntimeException("活动不存在或已下架");
        }

        // 校验时间
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(seckill.getStartTime())) {
            throw new RuntimeException("活动还没开始，请耐心等待");
        }
        if (now.isAfter(seckill.getEndTime())) {
            throw new RuntimeException("活动已结束，下次早点来");
        }
        // 必须手动拼接 Key 传给 Lua 脚本
        // 否则 Lua 里的 KEYS[1] 和 KEYS[2] 是空的，脚本会报错
        String stockKey = "seckill:stock:" + seckillId;
        String orderKey = "seckill:order:" + seckillId;
        List<String> keys = Arrays.asList(stockKey, orderKey);

        // 执行 Lua 脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                keys, // 这里要把 keys 列表传进去
                seckillId.toString(),
                userId.toString()
        );

        // 判断结果
        // 注意：execute 返回可能为空，最好加个判空
        if (result == null) {
            throw new RuntimeException("抢购失败，系统异常");
        }

        int r = result.intValue();
        if (r == 1) {
            throw new RuntimeException("手慢了，库存不足！");
        }
        if (r == 2) {
            throw new RuntimeException("您已经抢过了，不能贪心哦！");
        }

        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("userId", userId);
        msgMap.put("seckillId", seckillId);

        // 5. 发送消息 (转成 JSON 字符串发)
        String jsonMsg = JSON.toJSONString(msgMap);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECKILL_EXCHANGE,    // 交换机
                RabbitMQConfig.SECKILL_ROUTING_KEY, // 路由键
                jsonMsg                             // 消息内容
        );
        log.info("抢购成功，已发送消息到队列：{}", jsonMsg);

        // 订单还没生成，我们拿不到核销码。
        // 前端收到 code=1 就代表“排队中/抢购成功”，
        return "排队中...";
    }

    /**
     * 获取秒杀活动列表
     * @return
     */

    @Override
    public List<Seckill> findList() {
        String key = "seckill::list";

        // 1. 先查 Redis
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.hasLength(json)) {
            // 如果有缓存，把 JSON 转回 List 对象返回
            return JSON.parseArray(json, Seckill.class);
        }

        // 2. Redis 没有，查数据库
        List<Seckill> list = seckillMapper.list();

        // 3. 存入 Redis (加入随机过期时间)
        if (list != null && list.size() > 0) {
            // 随机时间 1~5 分钟 + 固定 30 分钟
            int randomTime = (int) (Math.random() * 5) + 1;

            stringRedisTemplate.opsForValue().set(
                    key,
                    JSON.toJSONString(list), // 转成 JSON 字符串存
                    30 + randomTime,         // 过期时间
                    TimeUnit.MINUTES         // 单位
            );
        }

        return list;
    }


    @Override
    public SeckillVoucher getOrder(Long userId, Long seckillId) {
        return seckillMapper.getOrder(userId, seckillId);
    }

    @Override
    public List<Map<String, Object>> findHistory(Long userId) {
        return seckillMapper.findHistory(userId);
    }

    private String createVoucherInDB(Long seckillId, Long userId) {
        SeckillVoucher voucher = new SeckillVoucher();
        voucher.setSeckillDishId(seckillId);
        voucher.setUserId(userId);

        // 简单生成6位随机码
        String code = String.valueOf((int)((Math.random() * 9 + 1) * 100000));
        voucher.setVoucherCode(code);
        voucher.setStatus(0);
        voucher.setOrderTime(LocalDateTime.now());

        seckillVoucherMapper.insert(voucher);
        return code;
    }

    /**
     * 查询单个商品详情
     * 缓存空对象，防止缓存穿透
     */
    public Seckill getById(Long id) {
        String key = "seckill:dish:" + id;

        // 1. 查 Redis
        String json = stringRedisTemplate.opsForValue().get(key);

        // 1.1 如果查到了，但是是空标识 (我们约定存 "" 或 "null" 代表空)
        if ("".equals(json)) {
            return null; // 直接返回 null，不去骚扰数据库
        }
        // 1.2 查到了真实数据
        if (StringUtils.hasLength(json)) {
            return JSON.parseObject(json, Seckill.class);
        }

        // 2. 查数据库
        Seckill seckill = seckillMapper.getById(id);

        if (seckill != null) {
            // 3.1 查到了，正常缓存 (30分钟 + 随机)
            int randomTime = (int) (Math.random() * 5) + 1;
            stringRedisTemplate.opsForValue().set(
                    key,
                    JSON.toJSONString(seckill),
                    30 + randomTime,
                    TimeUnit.MINUTES
            );
        } else {
            // 没查到(可能是恶意攻击)
            // 往 Redis 存一个空字符串，有效期设置短一点 (比如 2 分钟)
            stringRedisTemplate.opsForValue().set(key, "", 2, TimeUnit.MINUTES);
        }

        return seckill;
    }
}