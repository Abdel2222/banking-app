package com.banking.config;

import com.banking.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Court-circuite JwtAuthenticationFilter pour /actuator/**.
 * Utiliser seulement si nécessaire (la SecurityConfig peut suffire).
 */
@RequiredArgsConstructor
public class SkipActuatorFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private final JwtAuthenticationFilter delegate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (MATCHER.match("/actuator/**", uri)) {
            chain.doFilter(request, response);
            return;
        }
        delegate.doFilter(request, response, chain);
    }
}

