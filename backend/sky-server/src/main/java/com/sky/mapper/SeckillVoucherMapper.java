package com.sky.mapper;

import com.sky.entity.SeckillVoucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SeckillVoucherMapper {
    /**
     * 根据code查询秒杀券
     * @param code
     * @return
     */
    @Select("select * from seckill_voucher where voucher_code = #{code}")
    SeckillVoucher getByCode(String code);

    /**
     * 更新秒杀券信息
     * @param voucher
     */
    void update(SeckillVoucher voucher);

    /**
     * 新增秒杀券
     * @param voucher
     */
    void insert(SeckillVoucher voucher);
}
