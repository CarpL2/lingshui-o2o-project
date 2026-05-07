package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.entity.Seckill;
import com.sky.entity.SeckillVoucher;
import com.sky.result.Result;
import com.sky.service.SeckillService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController("userSeckillController")
@RequestMapping("/user/seckill")
@Api(tags = "C端-秒杀接口")
@Slf4j
public class SeckillController {

    @Autowired
    private SeckillService seckillService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/grab/{seckillId}")  // <--- 1. URL 后面挖个坑
    @ApiOperation("秒杀抢购")
// 👇 2. 告诉 Spring Boot 从 URL 的坑里把数字取出来
    public Result<String> grab(@PathVariable Long seckillId) {

        log.info("用户发起抢购，活动ID：{}", seckillId);

        // 调用业务逻辑
        String voucherCode = seckillService.grabSeckill(seckillId);

        return Result.success(voucherCode);
    }


    /**
     * 获取秒杀活动列表
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("获取秒杀活动列表")
    public Result<List<Seckill>> list() {
        List<Seckill> list = seckillService.findList();
        return Result.success(list);
    }

    @GetMapping("/result/{seckillId}")
    @ApiOperation("查询秒杀结果")
    public Result<String> getSeckillResult(@PathVariable Long seckillId) {
        Long userId = BaseContext.getCurrentId();

        String code = stringRedisTemplate.opsForValue().get("seckill:success:" + seckillId + ":" + userId);

        if (code != null) {
            return Result.success(code); // 查到了，返回核销码
        } else {
            return Result.success(null); // 没查到，继续排队
        }
    }

    @GetMapping("/history")
    @ApiOperation("查询我的秒杀记录")
    public Result<List<Map<String, Object>>> getHistory() {
        Long userId = BaseContext.getCurrentId();
        List<Map<String, Object>> list = seckillService.findHistory(userId);
        return Result.success(list);
    }
}