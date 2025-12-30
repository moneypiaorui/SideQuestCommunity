package com.sidequest.identity.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");
        String roles = request.getHeader("X-User-Roles");
        String permissions = request.getHeader("X-User-Permissions");

        if (userId != null) {
            List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
            if (role != null && !role.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            if (roles != null && !roles.isBlank()) {
                for (String r : roles.split(",")) {
                    if (!r.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + r.trim()));
                    }
                }
            }
            if (permissions != null && !permissions.isBlank()) {
                for (String p : permissions.split(",")) {
                    if (!p.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority(p.trim()));
                    }
                }
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
