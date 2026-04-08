package com.sky.service;

import com.sky.dto.AIAnalysisRequestDTO;
import com.sky.vo.AIAnalysisResponseVO;

/**
 * AI服务接口
 */
public interface AIService {

    /**
     * 分析订单数据
     * @param requestDTO 分析请求参数
     * @param formattedData 格式化的订单数据
     * @return 分析结果
     */
    AIAnalysisResponseVO analyzeOrderData(AIAnalysisRequestDTO requestDTO, String formattedData);

    /**
     * 生成分析报告
     * @param analysisData 分析数据
     * @return 分析报告
     */
    String generateAnalysisReport(String analysisData);

}
