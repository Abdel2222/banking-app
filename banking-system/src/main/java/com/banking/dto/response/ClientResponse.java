package com.banking.dto.response;

import com.banking.entities.Client;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ClientResponse {
    private Long id;
    private String prenom;
    private String nom;
    private String email;
    private String nomComplet;
    private String role;
    private int nombreComptes;
    private BigDecimal soldeTotal;
    private LocalDateTime createdAt;

    public ClientResponse(Client client) {
        this.id = client.getId();
        this.prenom = client.getPrenom();
        this.nom = client.getNom();
        this.email = client.getEmail();
        this.nomComplet = client.getNomComplet();
        this.role = client.getRole() != null ? client.getRole().name() : null; // ✅ enum vers String
        this.nombreComptes = client.getNombreComptes();
        this.soldeTotal = client.getSoldeTotal();
        this.createdAt = client.getCreatedAt();
    }

    // Getters
    public Long getId() { return id; }
    public String getPrenom() { return prenom; }
    public String getNom() { return nom; }
    public String getEmail() { return email; }
    public String getNomComplet() { return nomComplet; }
    public String getRole() { return role; }
    public int getNombreComptes() { return nombreComptes; }
    public BigDecimal getSoldeTotal() { return soldeTotal; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
