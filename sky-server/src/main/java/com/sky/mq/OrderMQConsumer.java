package com.sky.mq;

import com.alibaba.fastjson.JSON;
import com.sky.config.RabbitMQConfiguration;
import com.sky.pojo.OrderMessage;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单MQ消息消费者
 * 负责监听订单队列，接收订单消息并通过WebSocket通知商家端
 */
@Slf4j
@Component
public class OrderMQConsumer {

    /**
     * WebSocket服务器，用于向商家端推送实时通知
     */
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 处理订单消息
     * 当有订单消息到达队列时，自动调用此方法进行处理
     * 通过WebSocket将订单通知推送给商家端
     *
     * @param orderMessage 订单消息内容，包含订单的关键信息
     */
    @RabbitListener(queues = RabbitMQConfiguration.ORDER_QUEUE)
    public void processOrder(OrderMessage orderMessage) {
        log.info("【MQ消息】收到订单通知消息，订单号：{}", orderMessage.getOrderNumber());

        try {
            // 构建WebSocket推送消息
            // type=1 表示来单提醒，商家端收到后可以播放提示音或震动
            Map<String, Object> map = new HashMap<>();
            map.put("type", 1);                           // 1表示来单提醒
            map.put("orderId", orderMessage.getOrderId()); // 订单ID
            map.put("content", "订单号：" + orderMessage.getOrderNumber()); // 订单号内容

            // 将消息转换为JSON格式
            String json = JSON.toJSONString(map);

            // 通过WebSocket推送给商家端
            webSocketServer.sendToAllClient(json);

            log.info("【MQ消息】已通过WebSocket推送商家端通知，订单号：{}", orderMessage.getOrderNumber());
        } catch (Exception e) {
            log.error("【MQ消息】推送商家通知失败：{}", e.getMessage(), e);
        }
    }
}