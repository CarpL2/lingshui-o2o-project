package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 秒杀核销券实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillVoucher implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;  // 主键
    private Long userId;  // 抢购的用户ID
    private Long seckillDishId;  // 关联的秒杀活动ID (seckill_dish表的id)
    private String voucherCode;  // 6位数字核销码
    private Integer status;  // 核销状态 0:待核销 1:已核销

    private LocalDateTime orderTime;  // 抢购时间 (下单时间)
    private LocalDateTime useTime;  // 实际核销时间
}