package com.banking.dto.request;

import com.banking.entities.FraisDeGestion;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO pour la mise à jour d'un frais de gestion
 */
public class FraisDeGestionUpdateRequest {

    @DecimalMin(value = "0.01", message = "Le montant doit être positif")
    private BigDecimal montant;

    private String description;
    private LocalDate dateFin;
    private FraisDeGestion.Periodicite periodicite;
    private Boolean estActif;

    // Constructeurs
    public FraisDeGestionUpdateRequest() {}

    // Getters et Setters
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public FraisDeGestion.Periodicite getPeriodicite() { return periodicite; }
    public void setPeriodicite(FraisDeGestion.Periodicite periodicite) { this.periodicite = periodicite; }

    public Boolean getEstActif() { return estActif; }
    public void setEstActif(Boolean estActif) { this.estActif = estActif; }
}