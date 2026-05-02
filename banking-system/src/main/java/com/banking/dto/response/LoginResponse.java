package com.banking.dto.response;

/**
 * DTO pour la réponse après connexion réussie
 * Contient le token JWT et les informations de base du client
 */
public class LoginResponse {
    private String token;
    private String tokenType = "Bearer";
    private Long clientId;
    private String email;
    private String nomComplet;
    private String role;

    // Constructeur par défaut
    public LoginResponse() {}

    // Constructeur complet
    public LoginResponse(String token, Long clientId, String email, String nomComplet, String role) {
        this.token = token;
        this.tokenType = "Bearer";
        this.clientId = clientId;
        this.email = email;
        this.nomComplet = nomComplet;
        this.role = role;
    }

    // Getters et Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNomComplet() {
        return nomComplet;
    }

    public void setNomComplet(String nomComplet) {
        this.nomComplet = nomComplet;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "LoginResponse{" +
                "token='" + token + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", clientId=" + clientId +
                ", email='" + email + '\'' +
                ", nomComplet='" + nomComplet + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}