package com.sidequest.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Value("${auth.jwt.secret}")
    private String secret;

    // 白名单路径
    private static final List<String> WHITELIST = Arrays.asList(
            "/api/identity/login",
            "/api/identity/register"
    );

    // 公开访问的 GET 接口前缀
    private static final List<String> PUBLIC_GET_PREFIXES = Arrays.asList(
            "/api/core/posts",
            "/api/core/sections",
            "/api/core/tags",
            "/api/identity/users"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        // 1. 检查绝对白名单
        if (WHITELIST.stream().anyMatch(path::contains) || path.contains("/api/public")) {
            return chain.filter(exchange);
        }

        // 2. 检查公开的 GET 接口
        if (method == HttpMethod.GET && PUBLIC_GET_PREFIXES.stream().anyMatch(path::startsWith)) {
            // 特殊处理：如果是获取个人信息的 /api/identity/me，还是需要鉴权
            if (!path.equals("/api/identity/me")) {
                return chain.filter(exchange);
            }
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
            
            String userId = claims.getSubject();
            
            // RBAC 权限校验
            if (path.contains("/api/admin")) {
                String role = (String) claims.get("role");
                if (!"ADMIN".equals(role)) {
                    return forbidden(exchange);
                }
            }
            
            ServerHttpRequest mutableRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", (String) claims.get("role"))
                    .build();
            
            return chain.filter(exchange.mutate().request(mutableRequest).build());
        } catch (Exception e) {
            return unauthorized(exchange);
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
