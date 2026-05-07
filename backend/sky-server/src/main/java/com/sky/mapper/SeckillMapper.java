package com.sky.mapper;

import com.sky.dto.SeckillDishDTO;
import com.sky.entity.Seckill;
import com.sky.entity.SeckillVoucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface SeckillMapper {
    /**
     * 新增秒杀活动 (前端弹窗点击确定)
     */
    void addSeckill(Seckill seckill);

    /**
     * 查询所有秒杀活动
     */
    @Select("select sd.*, d.name, d.price, d.image " +
            "from seckill_dish sd " +
            "left join dish d on sd.dish_id = d.id " +
            "where sd.status = 1 " +
            "and sd.end_time > now() " +
            "order by sd.start_time asc")
    List<Seckill> list();

    /**
     * 根据ID查询秒杀活动
     */
    @Select("select sd.*, d.name name from seckill_dish sd " +
            "left join dish d on sd.dish_id = d.id " +
            "where sd.id = #{seckillId} " )
    Seckill getById(Long seckillId);

    // 减库存：只有库存大于0才能减
    @Update("update seckill_dish set stock_count = stock_count - 1 " +
            "where id = #{id} and stock_count > 0")
    void deductStock(Long id);

    /**
     *
     */
    @Select("select * from seckill_voucher where user_id = #{userId} and seckill_dish_id = #{seckillId}")
    SeckillVoucher getOrder(Long userId, Long seckillId);

    // 查当前用户的所有秒杀记录（关联一下菜品表，顺便把菜名查出来，方便展示）
    @Select("select v.*, d.name as dishName " +
            "from seckill_voucher v " +
            "left join seckill_dish sd on v.seckill_dish_id = sd.id " + // 1. 先连秒杀商品表拿到 dish_id
            "left join dish d on sd.dish_id = d.id " +                  // 2. 再连菜品表拿到 name
            "where v.user_id = #{userId} " +
            "order by v.order_time desc")
    List<Map<String, Object>> findHistory(Long userId);
}
