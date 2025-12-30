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
            "/api/core/interactions/comments",
            "/api/identity/users",
            "/api/search/user/posts"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        String authHeader = request.getHeaders().getFirst("Authorization");
        boolean hasToken = StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ");

        // 1. 检查绝对白名单 (完全不需要 Token，即使有也不解析)
        if (WHITELIST.stream().anyMatch(path::contains) || path.contains("/api/public")) {
            return chain.filter(exchange);
        }

        // 2. 如果没有 Token，检查是否是公开接口
        if (!hasToken) {
            if (method == HttpMethod.GET && PUBLIC_GET_PREFIXES.stream().anyMatch(path::startsWith)) {
                if (!path.equals("/api/identity/me")) {
                    return chain.filter(exchange);
                }
            }
            return unauthorized(exchange);
        }

        // 3. 有 Token，解析并注入用户信息
        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
            
            String userId = claims.getSubject();
            String role = (String) claims.get("role");
            String roles = (String) claims.get("roles");
            String permissions = (String) claims.get("perms");

            boolean isAdmin = "ADMIN".equals(role) || hasRole(roles, "ADMIN");
            if (!StringUtils.hasText(role)) {
                role = resolvePrimaryRole(roles);
            }
            
            // RBAC 权限校验 (针对特定路径)
            if (path.contains("/api/admin") && !isAdmin) {
                return forbidden(exchange);
            }
            
            ServerHttpRequest mutableRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .header("X-User-Roles", roles == null ? "" : roles)
                    .header("X-User-Permissions", permissions == null ? "" : permissions)
                    .build();
            
            return chain.filter(exchange.mutate().request(mutableRequest).build());
        } catch (Exception e) {
            // 如果是公开接口且 Token 解析失败，仍然放行 (但没有用户信息)
            if (method == HttpMethod.GET && PUBLIC_GET_PREFIXES.stream().anyMatch(path::startsWith) && !path.equals("/api/identity/me")) {
                return chain.filter(exchange);
            }
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

    private boolean hasRole(String roles, String role) {
        if (!StringUtils.hasText(roles)) {
            return false;
        }
        return Arrays.asList(roles.split(",")).contains(role);
    }

    private String resolvePrimaryRole(String roles) {
        if (!StringUtils.hasText(roles)) {
            return "USER";
        }
        List<String> list = Arrays.asList(roles.split(","));
        if (list.contains("ADMIN")) {
            return "ADMIN";
        }
        return list.get(0);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
