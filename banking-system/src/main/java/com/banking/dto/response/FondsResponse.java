package com.banking.dto.response;

import com.banking.entities.Fonds;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FondsResponse {

    private Long id;
    private String nomFonds;
    private String codeIdentification;
    private BigDecimal rendement;
    private BigDecimal montant;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FondsResponse() {
    }

    public FondsResponse(Fonds fonds) {
        this.id = fonds.getId();
        this.nomFonds = fonds.getNomFonds();
        this.codeIdentification = fonds.getCodeIdentification();
        this.rendement = fonds.getRendement();
        this.montant = fonds.getMontant();
        this.createdAt = fonds.getCreatedAt();
        this.updatedAt = fonds.getUpdatedAt();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNomFonds() { return nomFonds; }
    public void setNomFonds(String nomFonds) { this.nomFonds = nomFonds; }

    public String getCodeIdentification() { return codeIdentification; }
    public void setCodeIdentification(String codeIdentification) { this.codeIdentification = codeIdentification; }

    public BigDecimal getRendement() { return rendement; }
    public void setRendement(BigDecimal rendement) { this.rendement = rendement; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}