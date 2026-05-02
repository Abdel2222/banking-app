package com.banking.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Filtre JWT :
 *  - Bypass pour /api/auth/** et /actuator/**
 *  - Extrait/valide le token, peuple le SecurityContext avec les autorités.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** Endpoints publics (pas d'auth requise) */
    private static final List<String> PUBLIC_PATTERNS = List.of(
            "/api/auth/**",
            "/actuator/**"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String uri = request.getRequestURI();

        // 1) Bypass pour endpoints publics
        if (isPublic(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2) Extraction du token
            final String jwt = getJwtFromRequest(request);

            // 3) Validation + alimentation du SecurityContext
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                final String email = safe(tokenProvider.getEmailFromToken(jwt));
                // Support multi-rôles en priorité
                List<String> roles = null;
                try {
                    roles = tokenProvider.getRolesFromToken(jwt); // implémente-la côté provider si pas déjà fait
                } catch (Throwable ignored) {
                    // méthode absente ou non implémentée : on ignore
                }
                // Fallback sur "role" simple si pas de "roles"
                String singleRole = null;
                try {
                    singleRole = tokenProvider.getRoleFromToken(jwt);
                } catch (Throwable ignored) {}

                final List<GrantedAuthority> authorities = buildAuthorities(roles, singleRole);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                StringUtils.hasText(email) ? email : "anonymous",
                                null,
                                authorities
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // (Optionnel) logger en DEBUG
                // log.debug("Authenticated '{}' with authorities {}", email, authorities);
            }
        } catch (Exception ex) {
            // En cas d'erreur JWT, nettoyer le contexte et continuer (anonyme)
            SecurityContextHolder.clearContext();
            // (Optionnel) logger en WARN/DEBUG
            // log.warn("JWT filter error on {}: {}", uri, ex.getMessage());
        }

        // 4) Continuer la chaîne
        filterChain.doFilter(request, response);
    }

    private boolean isPublic(String uri) {
        for (String pattern : PUBLIC_PATTERNS) {
            if (PATH_MATCHER.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        final String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    /** Construit la liste d'autorisations à partir d'un tableau "roles" ou d'un "role" simple. */
    private List<GrantedAuthority> buildAuthorities(List<String> roles, String singleRole) {
        if (roles != null && !roles.isEmpty()) {
            List<GrantedAuthority> list = new ArrayList<>(roles.size());
            for (String r : roles) {
                if (StringUtils.hasText(r)) {
                    list.add(new SimpleGrantedAuthority(normalizeRole(r)));
                }
            }
            return list;
        }
        if (StringUtils.hasText(singleRole)) {
            return List.of(new SimpleGrantedAuthority(normalizeRole(singleRole)));
        }
        return Collections.emptyList();
    }

    /** Ajoute le préfixe ROLE_ si manquant. */
    private String normalizeRole(String role) {
        String r = role.trim();
        return r.startsWith("ROLE_") ? r : "ROLE_" + r;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
