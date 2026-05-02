package com.banking.exceptions;

import java.time.LocalDateTime;
import java.util.Map;

public class ApiError {
    private int status;                // Code HTTP (400, 403, 500…)
    private String error;              // Type d'erreur ("Validation Error", "Business Error"…)
    private String message;            // Message principal
    private Map<String, String> details; // Détails champ par champ (utile pour @Valid)
    private String path;               // L’URL appelée
    private LocalDateTime timestamp;   // Date/heure de l’erreur

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public ApiError(int status, String error, String message, Map<String, String> details, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.details = details;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }

    // 👉 Mets les getters/setters (ou utilise Lombok @Data si tu l’as)
}
