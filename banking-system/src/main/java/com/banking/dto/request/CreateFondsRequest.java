package com.banking.dto.request;

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

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.00")
    private BigDecimal montant;

    public String getNomFonds() { return nomFonds; }
    public void setNomFonds(String nomFonds) { this.nomFonds = nomFonds; }

    public String getCodeIdentification() { return codeIdentification; }
    public void setCodeIdentification(String codeIdentification) { this.codeIdentification = codeIdentification; }

    public BigDecimal getRendement() { return rendement; }
    public void setRendement(BigDecimal rendement) { this.rendement = rendement; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
}