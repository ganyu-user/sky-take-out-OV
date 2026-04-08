package com.sky.config;

import com.sky.properties.AliyunDashscopeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 配置类：用于管理AI相关的配置
 */
@Slf4j
@Configuration
public class AIConfig {

    @Bean
    public RestTemplate restTemplate() {
        log.info("开始创建RestTemplate对象");
        return new RestTemplate();
    }

}
