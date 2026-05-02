package com.banking.dto.response;

import com.banking.entities.CompteBancaire;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountResponse {

    private Long id;
    private String numCompte;
    private BigDecimal balance;
    private String status;
    private String clientName;
    private boolean hasCard;
    private boolean savingsAccount;
    private LocalDateTime createdAt;

    // Constructeur depuis l’entité
    public AccountResponse(CompteBancaire compte) {
        this.id = compte.getId();
        this.numCompte = compte.getNumCompte();
        this.balance = compte.getBalance();
        this.status = compte.getStatus() != null ? compte.getStatus().name() : null;
        this.clientName = compte.getClient() != null ? compte.getClient().getNomComplet() : null;
        this.hasCard = compte.getCarteBancaire() != null;
        this.savingsAccount = compte.getCompteEpargne() != null;
        this.createdAt = compte.getCreatedAt();
    }

    // ✅ Méthode statique utilitaire
    public static AccountResponse fromEntity(CompteBancaire compte) {
        return new AccountResponse(compte);
    }

    // Getters
    public Long getId() { return id; }
    public String getNumCompte() { return numCompte; }
    public BigDecimal getBalance() { return balance; }
    public String getStatus() { return status; }
    public String getClientName() { return clientName; }
    public boolean isHasCard() { return hasCard; }
    public boolean isSavingsAccount() { return savingsAccount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

