package com.sidequest.common.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Enumeration;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor feignRequestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                Enumeration<String> headerNames = request.getHeaderNames();
                if (headerNames != null) {
                    while (headerNames.hasMoreElements()) {
                        String name = headerNames.nextElement();
                        // 转发鉴权相关的 Header
                        if ("x-user-id".equalsIgnoreCase(name) || 
                            "x-user-role".equalsIgnoreCase(name) || 
                            "authorization".equalsIgnoreCase(name)) {
                            requestTemplate.header(name, request.getHeader(name));
                        }
                    }
                }
            }
        };
    }
}




