package com.sky.controller.admin;

import com.sky.dto.SeckillDishDTO;
import com.sky.result.Result;
import com.sky.service.SeckillService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController("adminSeckillController")
@RequestMapping("/admin/seckill")
@Slf4j
@Api(tags = "秒杀活动管理")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    /**
     * 新增秒杀活动 (前端弹窗点击确定)
     */
    @PostMapping("/add")
    @ApiOperation("新增秒杀活动")
    @Transactional
    public Result addSeckill(@RequestBody SeckillDishDTO dto) {
        log.info("开始新增秒杀活动：{}", dto);
        seckillService.addSeckill(dto);
        return Result.success();
    }

    /**
     * 核销券码 (前端左侧菜单点击核销)
     */
    @PostMapping("/verify")
    @ApiOperation("核销券码")
    public Result verify(@RequestBody Map<String, String> map) {
        String code = map.get("voucherCode");
        log.info("开始核销券码：{}", code);

        // 调用Service层去查表并核销
        // 返回一个 Map 给前端展示（菜品名、价格等）
        Map<String, Object> verifyInfo = seckillService.verifyVoucher(code);
        return Result.success(verifyInfo);
    }
}