package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

public interface ReportService {
    /**
     * 统计指定时间区间内的营业额信息
     * @param begin
     * @param end
     * @return
     */
    TurnoverReportVO turnoverReport(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间区间内的用户信息
     * @param begin
     * @param end
     * @return
     */
    UserReportVO userStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间区间内的订单信息
     *
     * @param begin
     * @param end
     * @return
     */
    OrderReportVO orderStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间区间内的销量排名前10商品
     * @param begin
     * @param end
     * @return
     */
    SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end);

    /**
     * 导出营业数据报表
     * @param response
     */
    void exportBusinessData(HttpServletResponse response);
}
