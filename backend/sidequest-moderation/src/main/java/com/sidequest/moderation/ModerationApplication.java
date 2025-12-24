package com.sidequest.moderation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.sidequest"})
@EnableDiscoveryClient
public class ModerationApplication {
    public static void main(String[] args) {
        SpringApplication.run(ModerationApplication.class, args);
    }
}

