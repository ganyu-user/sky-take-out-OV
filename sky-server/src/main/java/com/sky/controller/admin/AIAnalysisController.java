package com.sky.controller.admin;

import com.sky.dto.AIAnalysisRequestDTO;
import com.sky.result.Result;
import com.sky.service.AIService;
import com.sky.service.OrderDataService;
import com.sky.vo.AIAnalysisResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI分析控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/ai-analysis")
public class AIAnalysisController {

    @Autowired
    private AIService aiService;

    @Autowired
    private OrderDataService orderDataService;

    /**
     * 分析订单数据
     * @param requestDTO 分析请求参数
     * @return 分析结果
     */
    @PostMapping("/analyze")
    public Result<AIAnalysisResponseVO> analyzeOrderData(@RequestBody AIAnalysisRequestDTO requestDTO) {
        log.info("接收AI分析请求: {}", requestDTO);

        try {
            // 格式化数据为AI可处理的格式
            String formattedData = orderDataService.formatDataForAI(requestDTO);
            log.info("格式化订单数据完成，数据长度: {}", formattedData.length());
            
            // 调用AI服务进行分析，传递实际的订单数据
            AIAnalysisResponseVO responseVO = aiService.analyzeOrderData(requestDTO, formattedData);
            
            log.info("AI分析完成: {}", responseVO);
            return Result.success(responseVO);
        } catch (Exception e) {
            log.error("AI分析失败: {}", e.getMessage(), e);
            return Result.error("分析失败，请稍后重试");
        }
    }

    /**
     * 生成分析报告
     * @param analysisData 分析数据
     * @return 分析报告
     */
    @PostMapping("/generate-report")
    public Result<String> generateAnalysisReport(@RequestBody String analysisData) {
        log.info("开始生成分析报告");

        try {
            String report = aiService.generateAnalysisReport(analysisData);
            log.info("分析报告生成完成");
            return Result.success(report);
        } catch (Exception e) {
            log.error("生成分析报告失败: {}", e.getMessage(), e);
            return Result.error("报告生成失败，请稍后重试");
        }
    }

    /**
     * 获取订单数据概览
     * @param requestDTO 分析请求参数
     * @return 订单数据概览
     */
    @PostMapping("/data-overview")
    public Result<String> getDataOverview(@RequestBody AIAnalysisRequestDTO requestDTO) {
        log.info("获取订单数据概览: {}", requestDTO);

        try {
            String dataOverview = orderDataService.extractOrderData(requestDTO.getStartDate(), requestDTO.getEndDate());
            log.info("订单数据概览获取完成");
            return Result.success(dataOverview);
        } catch (Exception e) {
            log.error("获取订单数据概览失败: {}", e.getMessage(), e);
            return Result.error("数据获取失败，请稍后重试");
        }
    }

}
