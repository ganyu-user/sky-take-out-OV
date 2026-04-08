package com.sky.service.impl;

import com.sky.dto.AIAnalysisRequestDTO;
import com.sky.mapper.OrderMapper;
import com.sky.service.OrderDataService;
import com.sky.service.ReportService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.SalesTop10ReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单数据服务实现类
 */
@Slf4j
@Service
public class OrderDataServiceImpl implements OrderDataService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ReportService reportService;

    @Override
    public String extractOrderData(LocalDate startDate, LocalDate endDate) {
        log.info("开始提取订单数据，时间范围：{} 至 {}", startDate, endDate);

        StringBuilder data = new StringBuilder();
        data.append("订单数据统计\n");
        data.append("时间范围：").append(startDate).append(" 至 " ).append(endDate).append("\n\n");

        // 1. 业务数据概览
        BusinessDataVO businessData = getBusinessData(startDate, endDate);
        data.append("1. 业务数据概览\n");
        data.append("   营业额：").append(businessData.getTurnover()).append(" 元\n");
        data.append("   有效订单数：").append(businessData.getValidOrderCount()).append(" 单\n");
        data.append("   平均客单价：").append(businessData.getUnitPrice()).append(" 元\n");
        data.append("   订单完成率：").append(businessData.getOrderCompletionRate()).append("%\n\n");

        // 2. 销量Top10
        SalesTop10ReportVO salesTop10 = getSalesTop10(startDate, endDate);
        data.append("2. 销量Top10\n");
        if (salesTop10 != null && salesTop10.getNameList() != null && salesTop10.getNumberList() != null) {
            String[] names = salesTop10.getNameList().split(",");
            String[] numbers = salesTop10.getNumberList().split(",");
            for (int i = 0; i < names.length && i < numbers.length; i++) {
                data.append("   ").append(names[i]).append("：").append(numbers[i]).append(" 份\n");
            }
        }
        data.append("\n");

        // 3. 每日销售趋势
        data.append("3. 每日销售趋势\n");
        for (LocalDate date = startDate; date.isBefore(endDate.plusDays(1)); date = date.plusDays(1)) {
            LocalDateTime begin = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime end = LocalDateTime.of(date, LocalTime.MAX);

            Map<String, Object> map = new HashMap<>();
            map.put("begin", begin);
            map.put("end", end);
            map.put("status", 5); // 已完成订单

            Double turnover = orderMapper.getSumByMap(map);
            Integer orderCount = orderMapper.getCountByMap(map);

            data.append("   " ).append(date).append("：" );
            data.append("营业额=").append(turnover != null ? turnover : 0).append("元, ");
            data.append("订单数=").append(orderCount != null ? orderCount : 0).append("单\n");
        }

        log.info("订单数据提取完成");
        return data.toString();
    }

    @Override
    public BusinessDataVO getBusinessData(LocalDate startDate, LocalDate endDate) {
        LocalDateTime begin = LocalDateTime.of(startDate, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(endDate, LocalTime.MAX);

        // 计算营业额
        Map<String, Object> turnoverMap = new HashMap<>();
        turnoverMap.put("begin", begin);
        turnoverMap.put("end", end);
        turnoverMap.put("status", 5); // 已完成订单
        Double turnover = orderMapper.getSumByMap(turnoverMap);
        turnover = turnover != null ? turnover : 0.0;

        // 计算订单数
        Map<String, Object> orderCountMap = new HashMap<>();
        orderCountMap.put("begin", begin);
        orderCountMap.put("end", end);
        Integer orderCount = orderMapper.getCountByMap(orderCountMap);

        // 计算有效订单数
        Map<String, Object> validOrderCountMap = new HashMap<>();
        validOrderCountMap.put("begin", begin);
        validOrderCountMap.put("end", end);
        validOrderCountMap.put("status", 5); // 已完成订单
        Integer validOrderCount = orderMapper.getCountByMap(validOrderCountMap);

        // 计算订单完成率
        Double orderCompletionRate = 0.0;
        if (orderCount != null && orderCount > 0) {
            orderCompletionRate = validOrderCount * 100.0 / orderCount;
        }

        // 计算平均客单价
        Double unitPrice = 0.0;
        if (validOrderCount != null && validOrderCount > 0) {
            unitPrice = turnover / validOrderCount;
        }

        // 计算新增用户数（暂时设为0，实际项目中需要从用户表查询）
        Integer newUsers = 0;

        BusinessDataVO businessDataVO = new BusinessDataVO();
        businessDataVO.setTurnover(turnover);
        businessDataVO.setValidOrderCount(validOrderCount);
        businessDataVO.setOrderCompletionRate(orderCompletionRate);
        businessDataVO.setUnitPrice(unitPrice);
        businessDataVO.setNewUsers(newUsers);

        return businessDataVO;
    }

    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate startDate, LocalDate endDate) {
        // 直接复用ReportService的方法
        return reportService.getSalesTop10(startDate, endDate);
    }

    @Override
    public String formatDataForAI(AIAnalysisRequestDTO analysisRequestDTO) {
        log.info("开始格式化数据为AI可处理的格式: {}", analysisRequestDTO);

        LocalDate startDate = analysisRequestDTO.getStartDate();
        LocalDate endDate = analysisRequestDTO.getEndDate();
        Integer analysisType = analysisRequestDTO.getAnalysisType();

        StringBuilder formattedData = new StringBuilder();
        formattedData.append("# 智能订单分析数据\n\n");
        formattedData.append("## 分析参数\n");
        formattedData.append("- 分析时间范围：").append(startDate).append(" 至 " ).append(endDate).append("\n");
        
        switch (analysisType) {
            case 1:
                formattedData.append("- 分析类型：销售趋势分析\n\n");
                break;
            case 2:
                formattedData.append("- 分析类型：菜品分析\n\n");
                break;
            case 3:
                formattedData.append("- 分析类型：用户行为分析\n\n");
                break;
            case 4:
                formattedData.append("- 分析类型：自定义分析\n");
                formattedData.append("- 自定义提示词：").append(analysisRequestDTO.getCustomPrompt()).append("\n\n");
                break;
        }

        // 添加详细数据
        formattedData.append("## 详细数据\n");
        formattedData.append(extractOrderData(startDate, endDate));

        log.info("数据格式化完成");
        return formattedData.toString();
    }

}
