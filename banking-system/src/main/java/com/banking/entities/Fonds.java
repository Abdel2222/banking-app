package com.banking.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.00", message = "Le montant doit être positif ou nul")
    @Column(name = "montant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "fonds", fetch = FetchType.LAZY)
    private List<Placement> placements = new ArrayList<>();

    public Fonds() {
    }

    public Fonds(String nomFonds, String codeIdentification, BigDecimal rendement, BigDecimal montant) {
        this.nomFonds = nomFonds;
        this.codeIdentification = codeIdentification;
        this.rendement = rendement;
        this.montant = montant;
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

    public boolean montantRespecteMinimum(BigDecimal montantPlacement) {
        if (montantPlacement == null || montant == null) return false;
        return montantPlacement.compareTo(montant) >= 0;
    }

    public BigDecimal calculerGainPrevu(BigDecimal montantPlacement) {
        if (montantPlacement == null || rendement == null) return BigDecimal.ZERO;
        return montantPlacement.multiply(rendement).divide(BigDecimal.valueOf(100));
    }

    public BigDecimal calculerGainJournalier(BigDecimal montantPlacement) {
        if (montantPlacement == null || rendement == null) return BigDecimal.ZERO;
        return montantPlacement.multiply(rendement)
                .divide(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(365), 6, RoundingMode.HALF_UP);
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

    public List<Placement> getPlacements() { return placements; }
    public void setPlacements(List<Placement> placements) { this.placements = placements; }

    @Override
    public String toString() {
        return "Fonds{id=" + id + ", nomFonds='" + nomFonds + "', code='" + codeIdentification +
                "', rendement=" + rendement + ", montant=" + montant + '}';
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