package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.aliyun.dashscope")
@Data
public class AliyunDashscopeProperties {

    private String apiKey;
    private String endpoint;
    private String model;

}