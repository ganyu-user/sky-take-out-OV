package com.sky.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * AI分析请求参数
 */
@Data
public class AIAnalysisRequestDTO {

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 分析类型：1-销售趋势 2-菜品分析 3-用户行为 4-自定义
     */
    private Integer analysisType;

    /**
     * 自定义分析提示词
     */
    private String customPrompt;

}
