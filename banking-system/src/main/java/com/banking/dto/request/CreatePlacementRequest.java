package com.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CreatePlacementRequest {

    @NotNull(message = "L'identifiant du compte bancaire est obligatoire")
    private Long compteBancaireId;

    @NotNull(message = "L'identifiant du fonds est obligatoire")
    private Long fondsId;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "1.00", message = "Le montant doit être supérieur à 0")
    private BigDecimal montant;

    public Long getCompteBancaireId() {
        return compteBancaireId;
    }

    public void setCompteBancaireId(Long compteBancaireId) {
        this.compteBancaireId = compteBancaireId;
    }

    public Long getFondsId() {
        return fondsId;
    }

    public void setFondsId(Long fondsId) {
        this.fondsId = fondsId;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }
}