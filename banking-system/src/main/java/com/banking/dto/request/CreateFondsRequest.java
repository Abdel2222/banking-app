package com.banking.dto.request;

import com.banking.entity.enums.NiveauRisque;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateFondsRequest {

    @NotBlank(message = "Le nom du fonds est obligatoire")
    @Size(max = 100)
    private String nomFonds;

    @NotBlank(message = "Le code d'identification est obligatoire")
    @Size(max = 50)
    private String codeIdentification;

    @NotNull(message = "Le rendement est obligatoire")
    @DecimalMin(value = "0.00")
    private BigDecimal rendement;

    @NotNull(message = "Le niveau de risque est obligatoire")
    private NiveauRisque niveauRisque;

    @NotNull(message = "Le montant minimum est obligatoire")
    @DecimalMin(value = "0.00")
    private BigDecimal montantMinimum;

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
}