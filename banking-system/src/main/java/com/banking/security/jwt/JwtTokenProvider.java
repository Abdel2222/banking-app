package com.banking.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    @Value("${app.jwtSecret:mySecretKeyForBankingApp123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ}")
    private String jwtSecret;

    @Value("${app.jwtExpirationInMs:86400000}")
    private long jwtExpirationInMs;

    private Key getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** Token avec rôle unique (compat rétro). */
    public String generateToken(Long clientId, String email, String nomComplet, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(clientId.toString())
                .claim("email", email)
                .claim("nomComplet", nomComplet)
                .claim("role", role)                 // champ simple
                .claim("type", "access_token")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /** Variante optionnelle : token avec plusieurs rôles. */
    public String generateToken(Long clientId, String email, String nomComplet, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(clientId.toString())
                .claim("email", email)
                .claim("nomComplet", nomComplet)
                .claim("roles", roles)               // tableau de rôles
                .claim("type", "access_token")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getClientIdFromToken(String token) {
        return Long.parseLong(getAllClaims(token).getSubject());
    }

    public String getEmailFromToken(String token) {
        return getAllClaims(token).get("email", String.class);
    }

    public String getNomCompletFromToken(String token) {
        return getAllClaims(token).get("nomComplet", String.class);
    }

    /** Rôle unique (compat) */
    public String getRoleFromToken(String token) {
        return getAllClaims(token).get("role", String.class);
    }

    /** Nouvel utilitaire : lit la claim "roles" (liste). Retourne liste vide si absente. */
    public List<String> getRolesFromToken(String token) {
        Claims claims = getAllClaims(token);
        Object val = claims.get("roles");
        if (val instanceof List<?> raw) {
            // mappe en List<String> prudemment
            return raw.stream()
                    .filter(o -> o != null)
                    .map(Object::toString)
                    .toList();
        }
        // pas de claim "roles" -> liste vide (fallback sur getRoleFromToken dans le filtre)
        return List.of();
    }

    public boolean isTokenExpired(String token) {
        return getAllClaims(token).getExpiration().before(new Date());
    }

    private Claims getAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}


