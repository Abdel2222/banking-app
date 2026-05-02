package com.banking.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new IllegalStateException("Utilisateur non authentifié");
        Object principal = auth.getPrincipal();
        if (principal == null) throw new IllegalStateException("Principal absent");

        try {
            Method m = principal.getClass().getMethod("getId");
            Object val = m.invoke(principal);
            if (val instanceof Long l) return l;
        } catch (Exception ignored) {}

        try {
            Method m = principal.getClass().getMethod("getUserId");
            Object val = m.invoke(principal);
            if (val instanceof Long l) return l;
        } catch (Exception ignored) {}

        throw new IllegalStateException("Impossible de déterminer l'id utilisateur courant.");
    }

    // Alias si, dans ton modèle, userId == clientId
    public static Long currentClientId() {
        return currentUserId();
    }
}

