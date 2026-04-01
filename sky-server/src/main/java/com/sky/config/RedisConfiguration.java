package com.sky.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 配置类，用于创建Redis对象
 */
@Configuration
@Slf4j
public class RedisConfiguration {

    /**
     * 配置RedisTemplate用于对象缓存（支持多级缓存）
     * 使用Primary标记，优先使用此配置
     * @return
     */
    @Bean
    @Primary
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        log.info("开始创建Redis对象缓存模板...");
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

        // 设置redis的连接厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        //  创建JSON序列化器
        ObjectMapper objectMapper = new ObjectMapper();
        //  注册java 8日期时间模块
        objectMapper.registerModule(new JavaTimeModule());
        //  禁用日期时间戳写入（用ISO格式）
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance, // 允许任何类都能被 序列化/反序列化
                ObjectMapper.DefaultTyping.NON_FINAL,   //  对非final对象加标识，自创对象需要标识
                JsonTypeInfo.As.PROPERTY  //  把类名作为属性封装进JSON
        );
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 设置key的序列化器
        redisTemplate.setKeySerializer((new StringRedisSerializer()));
        redisTemplate.setHashKeySerializer((new StringRedisSerializer()));

        // 设置value的序列化器
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);

        return redisTemplate;
    }
}

