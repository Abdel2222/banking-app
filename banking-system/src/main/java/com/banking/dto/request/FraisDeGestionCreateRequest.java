package com.banking.dto.request;

import com.banking.entity.enums.Periodicite;
import com.banking.entity.enums.TypeFrais;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FraisDeGestionCreateRequest {

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    private BigDecimal montant;

    @Size(max = 255, message = "La description ne peut pas dépasser 255 caractères")
    private String description;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    private LocalDate dateFin;

    private TypeFrais typeFrais;

    private Periodicite periodicite;

    private Boolean estActif;

    /** ID du compte bancaire sur lequel les frais seront appliqués */
    private Long compteBancaireId;

    // ===== Constructeurs =====

    public FraisDeGestionCreateRequest() {}

    public FraisDeGestionCreateRequest(BigDecimal montant,
                                       String description,
                                       LocalDate dateDebut,
                                       LocalDate dateFin,
                                       TypeFrais typeFrais,
                                       Periodicite periodicite,
                                       Boolean estActif,
                                       Long compteBancaireId) {
        this.montant = montant;
        this.description = description;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.typeFrais = typeFrais;
        this.periodicite = periodicite;
        this.estActif = estActif;
        this.compteBancaireId = compteBancaireId;
    }

    // ===== Getters / Setters =====

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public TypeFrais getTypeFrais() { return typeFrais; }
    public void setTypeFrais(TypeFrais typeFrais) { this.typeFrais = typeFrais; }

    public Periodicite getPeriodicite() { return periodicite; }
    public void setPeriodicite(Periodicite periodicite) { this.periodicite = periodicite; }

    public Boolean getEstActif() { return estActif; }
    public void setEstActif(Boolean estActif) { this.estActif = estActif; }

    public Long getCompteBancaireId() { return compteBancaireId; }
    public void setCompteBancaireId(Long compteBancaireId) { this.compteBancaireId = compteBancaireId; }

    @Override
    public String toString() {
        return "FraisDeGestionCreateRequest{" +
                "montant=" + montant +
                ", description='" + description + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", typeFrais=" + typeFrais +
                ", periodicite=" + periodicite +
                ", estActif=" + estActif +
                ", compteBancaireId=" + compteBancaireId +
                '}';
    }
}