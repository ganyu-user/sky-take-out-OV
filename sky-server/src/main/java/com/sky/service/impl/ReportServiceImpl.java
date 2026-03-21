package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService  workspaceService;

    /**
     * 营业额统计接口
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //  1:存放从begin到end之间的日期集合
        List<LocalDate> dateList = getDateList(begin, end);

        //  存放从begin到end之间的每个日期对应的营业额的集合
        List<Double> turnoverList=new ArrayList<>();

        for (LocalDate date : dateList) {
            //  LocalDate是年月日，LocalDateTime是时分秒
            LocalDateTime dateBeginTime = LocalDateTime.of(date, LocalTime.MIN);  //  获取一天开始的时间
            LocalDateTime dateEndTime = LocalDateTime.of(date, LocalTime.MAX);  //  获取一天结束的时间

            //  把三个参数封装成 map
            Map map = new HashMap();
            map.put("begin", dateBeginTime);
            map.put("end", dateEndTime);
            map.put("status",Orders.COMPLETED); //状态为5（已完成）

            Double turnover = orderMapper.getSumByMap(map);

            //  判断当天营业额是否为null，如果是null转换成0
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ",")) //  把列表的元素用 “，” 隔开
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //  1:存放从begin到end之间的日期集合
        List<LocalDate> dateList = getDateList(begin, end);

        //  2:存放从begin到end之间的每天对应的用户总数量的集合
        List<Integer> totalUserList=new ArrayList<>();
        //  3:存放从begin到end之间的每天对应的新增用户数量的集合
        List<Integer> newUserLIst = new ArrayList<>();

        for (LocalDate date : dateList) {
            //  LocalDate是年月日，LocalDateTime是时分秒
            LocalDateTime dateBeginTime = LocalDateTime.of(date, LocalTime.MIN);  //  获取一天开始的时间
            LocalDateTime dateEndTime = LocalDateTime.of(date, LocalTime.MAX);  //  获取一天结束的时间

            //  封装成 map
            Map map = new HashMap();
            map.put("end", dateEndTime);

            //  总用户数量
            Integer totalUser = userMapper.countByMap(map);

            map.put("begin", dateBeginTime);
            //  新增用户数量
            Integer newUser = userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserLIst.add(newUser);
        }

        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ",")) //  把列表的元素用 “，” 隔开
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserLIst, ","))
                .build();
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        //  存放从begin到end之间的日期集合
        List<LocalDate> dateList = getDateList(begin, end);

        //  存放从begin到end之间的每天对应的 有效订单数量 的集合
        List<Integer> validOrderCountList =new ArrayList<>();
        //  存放从begin到end之间的每天对应的 总订单数量 的集合
        List<Integer> orderCountList =new ArrayList<>();

        //  统计每天的订单数量
        for (LocalDate date : dateList) {
            LocalDateTime dateBeginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dateEndTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", dateBeginTime);
            map.put("end", dateEndTime);

            //  每天的总订单数量
            Integer dailyOrderCount = orderMapper.getCountByMap(map);

            map.put("status", Orders.COMPLETED);

            //  每天的有效订单数量
            Integer daliyValidOrderCount = orderMapper.getCountByMap(map);

            orderCountList.add(dailyOrderCount);
            validOrderCountList.add(daliyValidOrderCount);
        }

        //  总的订单数量
        Integer totalOrderCount=orderCountList.stream().reduce(Integer::sum).get();
        
        //  总的有效订单数量
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        //  订单完成率
        Double orderCompletionRate = totalOrderCount != 0 ? validOrderCount.doubleValue() / totalOrderCount : 0.0;

        //  封装 返回结果
        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 销量排名top10统计
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        //  转换为年月日时分秒的时间格式后，获取精确时间
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
        List<String> nameList = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO
                .builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }

    /**
     * 根据开始、结束日期获取时间段列表
     * @param begin
     * @param end
     * @return
     */
    private List<LocalDate> getDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList();
        dateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);//  begin加一天
            dateList.add(begin);
        }
        return dateList;
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        // 1、查询数据库，获取营业数据----查询最近30天运营数据
        //  获取30天前的日期
        LocalDate begin = LocalDate.now().minusDays(30);
        //  获取昨天的日期
        LocalDate end = LocalDate.now().minusDays(1);

        //  查询概览数据----从年月日转换为年月日时分秒格式作为参数传入
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(
                LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX)
        );

        // 2、通过POI把数据写入Excel文件中
        InputStream in= this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //  基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);
            //  获取Excel文件中的Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            //  填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间："+begin+"至"+end);

            //  填充第四行
            XSSFRow row4 = sheet.getRow(3);
            //  第三列--营业额
            row4.getCell(2).setCellValue(businessDataVO.getTurnover());
            //  第五列--订单完成率
            row4.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            //  第七行--新增用户数
            row4.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //  填充第五行
            XSSFRow row5 = sheet.getRow(4);
            //  第三列--有效订单
            row5.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            //  第五列--平均客房价
            row5.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //  填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                // 查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(
                        LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX)
                );

                XSSFRow row = sheet.getRow(i + 7);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            // 3、通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //  关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
