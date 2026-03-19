package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务类，定时处理订单状态
 */
@Slf4j
@Component
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 定时处理付款超时订单
     */
    @Scheduled(cron = "0 0/3 * * * ?") // 每分钟执行一次
    public void processTimeOrder(){
        log.info("定时处理付款超时订单：{}", LocalDateTime.now());

        //  订单状态为1（未支付）且 下单时间 < （当前时间-15分钟）
        List<Orders> ordersList = orderMapper
                .getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(-15));

        if(!ordersList.isEmpty()&&ordersList.size()>0){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 处理一直处于派送中的订单
     */
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨处理一次
    public void processDeliveryOrder(){
        log.info("定时处理一直处于派送中的订单{}", LocalDateTime.now());

        //  订单状态为4（派送中） 且 派送时间 < （当前时间-1小时） 即 派送时间在凌晨十二点之前的订单
        List<Orders> ordersList = orderMapper
                .getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().plusMinutes(-60));

        if(!ordersList.isEmpty()&&ordersList.size()>0){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }
}
