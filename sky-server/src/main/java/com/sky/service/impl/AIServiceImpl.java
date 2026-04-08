package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.sky.dto.AIAnalysisRequestDTO;
import com.sky.properties.AliyunDashscopeProperties;
import com.sky.service.AIService;
import com.sky.vo.AIAnalysisResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * AI服务实现类
 */
@Slf4j
@Service
public class AIServiceImpl implements AIService {

    @Autowired
    private AliyunDashscopeProperties aliyunDashscopeProperties;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public AIAnalysisResponseVO analyzeOrderData(AIAnalysisRequestDTO requestDTO, String formattedData) {
        log.info("开始分析订单数据: {}", requestDTO);
        long startTime = System.currentTimeMillis();

        try {
            // 构建请求参数，包含实际的订单数据
            String prompt = buildPrompt(requestDTO, formattedData);
            String analysisData = callDashscopeAPI(prompt);

            // 构建响应
            AIAnalysisResponseVO responseVO = new AIAnalysisResponseVO();
            responseVO.setAnalysisResult(analysisData);
            responseVO.setAnalysisTime(LocalDateTime.now());
            responseVO.setAnalysisType(requestDTO.getAnalysisType());
            responseVO.setDuration(System.currentTimeMillis() - startTime);

            log.info("订单数据分析完成，耗时: {}ms", responseVO.getDuration());
            return responseVO;
        } catch (Exception e) {
            log.error("分析订单数据失败: {}", e.getMessage(), e);
            // 降级策略：返回默认分析结果
            AIAnalysisResponseVO fallbackVO = new AIAnalysisResponseVO();
            fallbackVO.setAnalysisResult("分析服务暂时不可用，请稍后重试");
            fallbackVO.setAnalysisTime(LocalDateTime.now());
            fallbackVO.setAnalysisType(requestDTO.getAnalysisType());
            fallbackVO.setDuration(System.currentTimeMillis() - startTime);
            return fallbackVO;
        }
    }

    @Override
    public String generateAnalysisReport(String analysisData) {
        log.info("开始生成分析报告");
        try {
            String prompt = "请基于以下分析数据，生成一份详细的分析报告，包括数据概览、趋势分析、问题发现和建议：\n" + analysisData;
            return callDashscopeAPI(prompt);
        } catch (Exception e) {
            log.error("生成分析报告失败: {}", e.getMessage(), e);
            return "报告生成失败，请稍后重试";
        }
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(AIAnalysisRequestDTO requestDTO, String formattedData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个智能订单分析助手，负责分析餐厅的订单数据。");
        prompt.append("\n分析时间范围：").append(requestDTO.getStartDate()).append(" 至 " ).append(requestDTO.getEndDate());
        
        // 添加实际的订单数据
        prompt.append("\n\n以下是实际的订单数据：");
        prompt.append("\n").append(formattedData);

        switch (requestDTO.getAnalysisType()) {
            case 1:
                prompt.append("\n\n请分析销售趋势，包括：");
                prompt.append("\n1. 每日销售额变化趋势");
                prompt.append("\n2. 销售高峰期分析");
                prompt.append("\n3. 环比增长情况");
                break;
            case 2:
                prompt.append("\n\n请分析菜品销售情况，包括：");
                prompt.append("\n1. 热销菜品TOP10");
                prompt.append("\n2. 滞销菜品分析");
                prompt.append("\n3. 菜品类别销售分布");
                break;
            case 3:
                prompt.append("\n\n请分析用户行为，包括：");
                prompt.append("\n1. 用户下单时间分布");
                prompt.append("\n2. 平均订单金额");
                prompt.append("\n3. 复购率分析");
                break;
            case 4:
                prompt.append("\n\n").append(requestDTO.getCustomPrompt());
                break;
            default:
                prompt.append("\n\n请进行全面分析，包括销售趋势、菜品销售和用户行为");
        }

        prompt.append("\n\n请基于以上实际数据提供详细的分析结果，包括数据支持和具体建议。");
        return prompt.toString();
    }

    /**
     * 调用通义千问API
     */
    private String callDashscopeAPI(String prompt) throws Exception {
        String apiKey = aliyunDashscopeProperties.getApiKey();
        String model = aliyunDashscopeProperties.getModel();

        // 使用正确的通义千问API endpoint（阿里云官方推荐格式）
        String endpoint = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        // 构建请求体（符合通义千问API格式）
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        
        Map<String, Object> input = new HashMap<>();
        input.put("prompt", prompt);
        requestBody.put("input", input);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("max_tokens", 1024);
        parameters.put("temperature", 0.7);
        requestBody.put("parameters", parameters);

        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(requestBody), headers);

        // 重试机制
        int retryCount = 3;
        for (int i = 0; i < retryCount; i++) {
            try {
                String response = restTemplate.postForObject(endpoint, entity, String.class);
                return parseResponse(response);
            } catch (Exception e) {
                log.warn("API调用失败，正在重试 ({}/{})：{}", i + 1, retryCount, e.getMessage());
                if (i == retryCount - 1) {
                    throw e;
                }
                Thread.sleep(1000); // 等待1秒后重试
            }
        }

        throw new Exception("API调用失败");
    }

    /**
     * 解析API响应
     */
    private String parseResponse(String response) {
        try {
            Map<String, Object> responseMap = JSON.parseObject(response, Map.class);
            Map<String, Object> output = (Map<String, Object>) responseMap.get("output");
            if (output != null) {
                return (String) output.get("text");
            }
            return "分析结果解析失败";
        } catch (Exception e) {
            log.error("解析API响应失败: {}", e.getMessage(), e);
            return "分析结果解析失败";
        }
    }

}
