package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import io.swagger.models.auth.In;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 订单分页查询
     * @param pageQuery
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO pageQuery);

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 根据状态统计订单数量
     * @param toBeConfirmed
     * @return
     */
    @Select("select count(*) from orders where status = #{status}")
    Integer countStatus(Integer toBeConfirmed);


    /**
     * 查询超时订单
     * @param status
     * @param time
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{time}")
    List<Orders> updateStatusWithTimeOut(Integer status, LocalDateTime time);

    /**
     * 根据日期统计营业额
     * @param status
     * @param beginTime
     * @param endTime
     * @return
     */
    Double selectTurnoverByDate(Integer status, LocalDateTime beginTime, LocalDateTime endTime);

    /**
     * 根据状态统计订单数量
     * @param map
     */
    Integer countByMap(Map map);

    /**
     * 统计一定时间内的销量前10
     * @param begin
     * @param end
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end, Integer status);
}
