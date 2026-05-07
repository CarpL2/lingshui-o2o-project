package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Seckill implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Integer SECKILL_DOWN = 0;
    public static final Integer SECKILL_ON = 1;

    private Long id;  // 主键ID
    private Long dishId;       // 原菜品ID
    private BigDecimal seckillPrice; // 秒杀价
    private Integer stockCount;      // 库存
    private Integer status;  // 状态

    private String name;   // 菜品名称
    private String image;  // 图片路径
    private Double price;  // 原价

    // 前端传过来的是字符串，这里用注解自动转成时间对象
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
