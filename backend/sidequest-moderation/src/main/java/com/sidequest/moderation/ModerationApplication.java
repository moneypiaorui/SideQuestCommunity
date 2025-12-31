package com.sidequest.moderation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication(scanBasePackages = {"com.sidequest"})
@EnableDiscoveryClient
@MapperScan("com.sidequest.moderation.infrastructure.mapper")
public class ModerationApplication {
    public static void main(String[] args) {
        SpringApplication.run(ModerationApplication.class, args);
    }
}

