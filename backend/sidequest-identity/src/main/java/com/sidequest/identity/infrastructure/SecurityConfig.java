package com.sidequest.identity.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. 禁用 CSRF
            .csrf(AbstractHttpConfigurer::disable)
            // 2. 配置请求授权规则
            .authorizeHttpRequests(auth -> auth
                // 放行登录、注册接口
                .requestMatchers("/api/identity/login", "/api/identity/register").permitAll()
                // 放行获取公开用户信息和内部调用的接口
                .requestMatchers("/api/identity/users/*/public", "/api/identity/users/*").permitAll()
                // 放行 Actuator 监控端点
                .requestMatchers("/actuator/**").permitAll()
                // 其余请求需要认证
                .anyRequest().authenticated()
            )
            // 3. 添加自定义过滤器处理网关传来的 Header
            .addFilterBefore(new HeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
