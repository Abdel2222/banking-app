package com.banking.dto.request;

import com.banking.entities.FraisDeGestion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO pour la création d'un frais de gestion
 */
public class FraisDeGestionCreateRequest {

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être positif")
    private BigDecimal montant;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    @NotNull(message = "Le type de frais est obligatoire")
    private FraisDeGestion.TypeFrais typeFrais;

    @NotNull(message = "La périodicité est obligatoire")
    private FraisDeGestion.Periodicite periodicite;

    private Boolean estActif = true;

    // Constructeurs
    public FraisDeGestionCreateRequest() {}

    public FraisDeGestionCreateRequest(BigDecimal montant, String description,
                                       LocalDate dateDebut, LocalDate dateFin,
                                       FraisDeGestion.TypeFrais typeFrais,
                                       FraisDeGestion.Periodicite periodicite) {
        this.montant = montant;
        this.description = description;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.typeFrais = typeFrais;
        this.periodicite = periodicite;
    }

    // Getters et Setters
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public FraisDeGestion.TypeFrais getTypeFrais() { return typeFrais; }
    public void setTypeFrais(FraisDeGestion.TypeFrais typeFrais) { this.typeFrais = typeFrais; }

    public FraisDeGestion.Periodicite getPeriodicite() { return periodicite; }
    public void setPeriodicite(FraisDeGestion.Periodicite periodicite) { this.periodicite = periodicite; }

    public Boolean getEstActif() { return estActif; }
    public void setEstActif(Boolean estActif) { this.estActif = estActif; }
}
