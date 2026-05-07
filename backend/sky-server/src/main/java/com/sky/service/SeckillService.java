package com.sky.service;

import com.sky.dto.SeckillDishDTO;
import com.sky.entity.Seckill;
import com.sky.entity.SeckillVoucher;

import java.util.List;
import java.util.Map;

public interface SeckillService {
    /**
     * 新增秒杀活动 (前端弹窗点击确定)
     */
    void addSeckill(SeckillDishDTO dto);

    /**
     * 核销券码 (前端左侧菜单点击核销)
     */
    Map<String, Object> verifyVoucher(String code);

    /**
     * C端用户秒杀抢购
     * @param seckillId 秒杀活动ID
     * @return 这里的返回值可以是生成的 核销码 或者 抢购结果
     */
    String grabSeckill(Long seckillId);

    /**
     * 查询秒杀列表
     * @return
     */
    List<Seckill> findList();

    /**
     * 获取用户订单信息
     * @param userId
     * @param seckillId
     * @return
     */
    SeckillVoucher getOrder(Long userId, Long seckillId);

    /**
     * 查询用户秒杀记录
     * @param userId
     * @return
     */
    List<Map<String, Object>> findHistory(Long userId);
}
