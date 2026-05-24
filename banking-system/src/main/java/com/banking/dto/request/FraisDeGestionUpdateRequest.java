package com.banking.dto.request;

import com.banking.entity.enums.Periodicite;  // ✅ corrigé
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FraisDeGestionUpdateRequest {

    @DecimalMin(value = "0.01", message = "Le montant doit être positif")
    private BigDecimal montant;

    private String description;
    private LocalDate dateFin;
    private Periodicite periodicite;  // ✅ corrigé
    private Boolean estActif;

    public FraisDeGestionUpdateRequest() {}

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public Periodicite getPeriodicite() { return periodicite; }          // ✅ corrigé
    public void setPeriodicite(Periodicite periodicite) { this.periodicite = periodicite; }  // ✅ corrigé

    public Boolean getEstActif() { return estActif; }
    public void setEstActif(Boolean estActif) { this.estActif = estActif; }
}