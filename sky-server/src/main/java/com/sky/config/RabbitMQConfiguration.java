package com.sky.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * 负责配置交换机、队列、绑定关系以及消息序列化方式
 */
@Configuration
public class RabbitMQConfiguration {

    /**
     * 交换机名称 - 订单通知交换机
     * 交换机用于接收生产者发送的消息，并根据路由键将消息分发到对应的队列
     */
    public static final String ORDER_EXCHANGE = "order.exchange";

    /**
     * 队列名称 - 订单通知队列
     * 队列用于存储待消费的消息，消费者从队列中获取消息进行处理
     */
    public static final String ORDER_QUEUE = "order.queue";

    /**
     * 路由键 - 订单创建路由键
     * 路由键用于交换机将消息路由到指定的队列
     * 生产者发送消息时指定路由键，只有队列绑定到该路由键才能收到消息
     */
    public static final String ORDER_ROUTING_KEY = "order.create";

    /**
     * 创建订单通知交换机
     * DirectExchange：直连交换机，根据路由键精确匹配队列
     *
     * @return 订单交换机实例
     */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    /**
     * 创建订单通知队列
     * durable(true)：队列持久化，RabbitMQ重启后队列依然存在
     *
     * @return 订单队列实例
     */
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE).build();
    }

    /**
     * 绑定交换机和队列
     * 将orderQueue绑定到orderExchange，并使用orderRoutingKey作为路由键
     * 只有路由键为"order.create"的消息才会被路由到该队列
     *
     * @param orderQueue 订单队列
     * @param orderExchange 订单交换机
     * @return 绑定关系
     */
    @Bean
    public Binding orderBinding(Queue orderQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ORDER_ROUTING_KEY);
    }

    /**
     * 配置ObjectMapper
     * 用于Spring AMQP的消息序列化/反序列化
     * 注册JavaTimeModule支持LocalDateTime等Java8时间类型
     *
     * @return 配置好的ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    /**
     * 配置RabbitTemplate（消息发送模板）
     * RabbitTemplate用于发送消息到RabbitMQ
     * 配置Jackson2JsonMessageConverter实现对象与JSON的自动转换
     *
     * @param connectionFactory RabbitMQ连接工厂
     * @param objectMapper JSON序列化工具
     * @return 配置好的RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        rabbitTemplate.setMessageConverter(converter);
        return rabbitTemplate;
    }

    /**
     * 配置消息监听容器工厂
     * 用于@RabbitListener注解的消息监听
     * 配置Jackson2JsonMessageConverter实现消息的反序列化
     *
     * @param connectionFactory RabbitMQ连接工厂
     * @param objectMapper JSON反序列化工具
     * @return 配置好的监听容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        factory.setMessageConverter(converter);
        return factory;
    }
}