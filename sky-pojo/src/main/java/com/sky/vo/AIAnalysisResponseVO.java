package com.sky.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI分析响应结果
 */
@Data
public class AIAnalysisResponseVO {

    /**
     * 分析结果
     */
    private String analysisResult;

    /**
     * 分析时间
     */
    private LocalDateTime analysisTime;

    /**
     * 分析类型
     */
    private Integer analysisType;

    /**
     * 耗时（毫秒）
     */
    private Long duration;

}
