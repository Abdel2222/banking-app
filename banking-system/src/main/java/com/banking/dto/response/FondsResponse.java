package com.banking.dto.response;



import com.banking.entities.Fonds;
import com.banking.entity.enums.NiveauRisque;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FondsResponse {

    private Long id;
    private String nomFonds;
    private String codeIdentification;
    private BigDecimal rendement;
    private NiveauRisque niveauRisque;
    private BigDecimal montantMinimum;
    private Boolean estActif;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FondsResponse() {
    }

    public FondsResponse(Fonds fonds) {
        this.id = fonds.getId();
        this.nomFonds = fonds.getNomFonds();
        this.codeIdentification = fonds.getCodeIdentification();
        this.rendement = fonds.getRendement();
        this.niveauRisque = fonds.getNiveauRisque();
        this.montantMinimum = fonds.getMontantMinimum();
        this.estActif = fonds.getEstActif();
        this.createdAt = fonds.getCreatedAt();
        this.updatedAt = fonds.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomFonds() {
        return nomFonds;
    }

    public void setNomFonds(String nomFonds) {
        this.nomFonds = nomFonds;
    }

    public String getCodeIdentification() {
        return codeIdentification;
    }

    public void setCodeIdentification(String codeIdentification) {
        this.codeIdentification = codeIdentification;
    }

    public BigDecimal getRendement() {
        return rendement;
    }

    public void setRendement(BigDecimal rendement) {
        this.rendement = rendement;
    }

    public NiveauRisque getNiveauRisque() {
        return niveauRisque;
    }

    public void setNiveauRisque(NiveauRisque niveauRisque) {
        this.niveauRisque = niveauRisque;
    }

    public BigDecimal getMontantMinimum() {
        return montantMinimum;
    }

    public void setMontantMinimum(BigDecimal montantMinimum) {
        this.montantMinimum = montantMinimum;
    }

    public Boolean getEstActif() {
        return estActif;
    }

    public void setEstActif(Boolean estActif) {
        this.estActif = estActif;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}