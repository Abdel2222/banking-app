package com.banking.entities;

import com.banking.entity.enums.NiveauRisque;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "fonds")
public class Fonds {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom du fonds est obligatoire")
    @Size(max = 100)
    @Column(name = "nom_fonds", nullable = false, length = 100)
    private String nomFonds;

    @NotBlank(message = "Le code d'identification est obligatoire")
    @Size(max = 50)
    @Column(name = "code_identification", nullable = false, unique = true, length = 50)
    private String codeIdentification;

    @NotNull(message = "Le rendement est obligatoire")
    @DecimalMin(value = "0.00", message = "Le rendement doit être positif ou nul")
    @Column(name = "rendement", nullable = false, precision = 5, scale = 2)
    private BigDecimal rendement;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau_risque", length = 30)
    private NiveauRisque niveauRisque;

    @NotNull(message = "Le montant minimum est obligatoire")
    @DecimalMin(value = "0.00", message = "Le montant minimum doit être positif ou nul")
    @Column(name = "montant_minimum", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantMinimum;

    @Column(name = "est_actif", nullable = false)
    private Boolean estActif = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "fonds", fetch = FetchType.LAZY)
    private List<Placement> placements = new ArrayList<>();

    public Fonds() {
    }

    public Fonds(String nomFonds, String codeIdentification, BigDecimal rendement,
                 NiveauRisque niveauRisque, BigDecimal montantMinimum) {
        this.nomFonds = nomFonds;
        this.codeIdentification = codeIdentification;
        this.rendement = rendement;
        this.niveauRisque = niveauRisque;
        this.montantMinimum = montantMinimum;
        this.estActif = true;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean estDisponible() {
        return Boolean.TRUE.equals(estActif);
    }

    public boolean montantRespecteMinimum(BigDecimal montant) {
        if (montant == null || montantMinimum == null) return false;
        return montant.compareTo(montantMinimum) >= 0;
    }

    public BigDecimal calculerGainPrevu(BigDecimal montant) {
        if (montant == null || rendement == null) return BigDecimal.ZERO;
        return montant.multiply(rendement).divide(BigDecimal.valueOf(100));
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

    public List<Placement> getPlacements() {
        return placements;
    }

    public void setPlacements(List<Placement> placements) {
        this.placements = placements;
    }

    @Override
    public String toString() {
        return "Fonds{" +
                "id=" + id +
                ", nomFonds='" + nomFonds + '\'' +
                ", codeIdentification='" + codeIdentification + '\'' +
                ", rendement=" + rendement +
                ", niveauRisque=" + niveauRisque +
                ", montantMinimum=" + montantMinimum +
                ", estActif=" + estActif +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fonds fonds)) return false;
        return id != null && Objects.equals(id, fonds.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}