package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 营业额统计接口
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //  存放从begin到end之间的日期集合
        List<LocalDate> dateList = new ArrayList();

        dateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }


        //  存放从begin到end之间的每个日期对应的营业额的集合
        List<Double> turnoverList=new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime dateBeginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dateEndTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", dateBeginTime);
            map.put("end", dateEndTime);
            map.put("status",Orders.COMPLETED);

            Double turnover = orderMapper.getSumByMap(map);

            //  判断当天营业额是否为null，如果是null转换成0
            turnover=turnover==null?0.0:turnover;
            turnoverList.add(turnover);
        }
        StringUtils.join(turnoverList, ",");

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }
}
