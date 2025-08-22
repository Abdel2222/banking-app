package com.banking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO pour les requêtes nécessitant uniquement un email
 * Utilisé pour : vérification d'email, mot de passe oublié, etc.
 */
public class EmailRequest {

    @Email(message = "L'email doit être valide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;

    // Constructeur par défaut
    public EmailRequest() {}

    // Constructeur avec paramètre
    public EmailRequest(String email) {
        this.email = email;
    }

    // Getter
    public String getEmail() {
        return email;
    }

    // Setter
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "EmailRequest{" +
                "email='" + email + '\'' +
                '}';
    }
}