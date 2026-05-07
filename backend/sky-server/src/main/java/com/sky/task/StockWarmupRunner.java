package com.sky.task;

import com.sky.entity.Seckill;
import com.sky.mapper.SeckillMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner; // 👈 核心接口
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class StockWarmupRunner implements CommandLineRunner {

    @Autowired
    private SeckillMapper seckillMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 这个方法会在 Spring Boot 启动完成后自动执行
     */
    @Override
    public void run(String... args) throws Exception {
        log.info("开始进行缓存预热...");

        // 1. 查询所有开启状态的秒杀商品
        List<Seckill> list = seckillMapper.list();

        if (list == null || list.isEmpty()) {
            log.info("没有需要预热的秒杀商品");
            return;
        }

        // 2. 把库存同步到 Redis
        for (Seckill seckill : list) {
            String key = "seckill:stock:" + seckill.getId();
            // 如果 Redis 里没有这个 key，才设置（防止覆盖掉运行中的库存）
            // 或者直接覆盖，保证跟数据库一致
            stringRedisTemplate.opsForValue().set(key, String.valueOf(seckill.getStockCount()));
        }

        // 顺便删掉之前的异常 Key，防止死锁
        // stringRedisTemplate.delete(stringRedisTemplate.keys("seckill:order:lock:*"));

        log.info("缓存预热完成！共加载 {} 条秒杀库存信息。", list.size());
    }
}