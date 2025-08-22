package com.banking.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;

import java.util.Optional;

public class JwtUtils {

    /** ✅ Récupère l’email de l’utilisateur connecté */
    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) return null;
        return authentication.getPrincipal().toString();
    }

    /** ✅ Récupère le rôle (sans le prefixe ROLE_) */
    public static String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return null;

        Optional<? extends GrantedAuthority> role = authentication.getAuthorities()
                .stream()
                .findFirst();

        if (role.isEmpty()) return null;

        String fullRole = role.get().getAuthority(); // ex: "ROLE_CLIENT"
        return fullRole.replace("ROLE_", "");
    }

    /** ✅ Vérifie si l'utilisateur est authentifié */
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated();
    }

    /** ✅ Vérifie si l'utilisateur est un client */
    public static boolean isClient() {
        return "CLIENT".equalsIgnoreCase(getCurrentUserRole());
    }

    /** ✅ Vérifie si l'utilisateur est admin */
    public static boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(getCurrentUserRole());
    }

    /** ✅ Vérifie si l'utilisateur est super admin */
    public static boolean isSuperAdmin() {
        return "SUPER_ADMIN".equalsIgnoreCase(getCurrentUserRole());
    }
}
