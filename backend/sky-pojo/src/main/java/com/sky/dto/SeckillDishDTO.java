package com.sky.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillDishDTO {
    private Long dishId;       // 原菜品ID
    private BigDecimal seckillPrice; // 秒杀价
    private Integer stockCount;      // 库存

    // 前端传过来的是字符串，这里用注解自动转成时间对象
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;
}