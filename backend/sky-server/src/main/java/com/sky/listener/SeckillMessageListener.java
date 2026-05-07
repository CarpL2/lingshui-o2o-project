package com.sky.listener;

import com.alibaba.fastjson.JSON;
import com.sky.config.RabbitMQConfig;
import com.sky.entity.SeckillVoucher;
import com.sky.mapper.SeckillMapper;
import com.sky.mapper.SeckillVoucherMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException; // 👈 必须引入这个异常
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport; // 👈 必须引入这个回滚支持

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SeckillMessageListener {

    @Autowired
    private SeckillMapper seckillMapper;
    @Autowired
    private SeckillVoucherMapper seckillVoucherMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void processMessage(String message) {
        log.info("收到抢购消息：{}", message);

        // 1. 解析消息
        Map map = JSON.parseObject(message, Map.class);
        Long userId = Long.valueOf(map.get("userId").toString());
        Long seckillId = Long.valueOf(map.get("seckillId").toString());

        // 利用 Redis 做第一层幂等校验 (防止数据库压力)
        // key eg: seckill:order:lock:1001:50
        String orderLockKey = "seckill:order:lock:" + seckillId + ":" + userId;

        // 加过期时间 防止服务宕机导致 Key 永久存在，无法重试
        Boolean isFirstTime = stringRedisTemplate.opsForValue()
                .setIfAbsent(orderLockKey, "1", 24, TimeUnit.HOURS);

        if (Boolean.FALSE.equals(isFirstTime)) {
            // Redis 中已有记录，说明是重复消息，直接 Ack
            log.info("消息重复，Redis校验拦截。userId={}", userId);
            return;
        }

        try {
            // 2. 扣减数据库库存
            seckillMapper.deductStock(seckillId);

            // 3. 准备订单对象
            SeckillVoucher voucher = new SeckillVoucher();
            voucher.setSeckillDishId(seckillId);
            voucher.setUserId(userId);
            voucher.setVoucherCode(String.valueOf((int)((Math.random() * 9 + 1) * 100000)));
            voucher.setStatus(0);
            voucher.setOrderTime(LocalDateTime.now());

            // 4. 插入订单
            seckillVoucherMapper.insert(voucher);

            // 5. 存入结果 Redis (给前端看的)
            stringRedisTemplate.opsForValue().set("seckill:success:" + seckillId + ":" + userId, voucher.getVoucherCode());

            // 6. 删掉列表缓存 (数据一致性)
            stringRedisTemplate.delete("seckill::list");

        } catch (DuplicateKeyException e) {
            // 兜底方案
            log.warn("检测到重复下单(数据库拦截)，触发事务回滚。");
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

        } catch (Exception e) {
            // 异常处理真正的业务异常
            log.error("处理异常，准备重试");

            // 因为处理失败了，MQ 会重试把 Redis 里的 LockKey 删掉 防止重试的时候进不来！
            stringRedisTemplate.delete(orderLockKey);

            throw new RuntimeException(e);
        }
    }
}