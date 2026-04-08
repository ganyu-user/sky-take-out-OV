package com.sky.service;

import com.sky.dto.AIAnalysisRequestDTO;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.SalesTop10ReportVO;

import java.time.LocalDate;

/**
 * 订单数据服务接口
 */
public interface OrderDataService {

    /**
     * 提取指定时间范围内的订单数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 格式化的订单数据
     */
    String extractOrderData(LocalDate startDate, LocalDate endDate);

    /**
     * 获取业务数据概览
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 业务数据VO
     */
    BusinessDataVO getBusinessData(LocalDate startDate, LocalDate endDate);

    /**
     * 获取销量Top10数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 销量Top10数据
     */
    SalesTop10ReportVO getSalesTop10(LocalDate startDate, LocalDate endDate);

    /**
     * 格式化数据为AI可处理的格式
     * @param analysisRequestDTO 分析请求参数
     * @return 格式化后的数据
     */
    String formatDataForAI(AIAnalysisRequestDTO analysisRequestDTO);

}
