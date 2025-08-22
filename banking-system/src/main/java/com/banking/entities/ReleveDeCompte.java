package com.banking.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "releves_de_compte")
public class ReleveDeCompte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== Données métier =====
    @NotNull
    @Column(name = "date_releve", nullable = false)
    private LocalDate dateReleve;

    @NotNull
    @DecimalMin("0.00")
    @Column(name = "solde_initial", nullable = false, precision = 15, scale = 2)
    private BigDecimal soldeInitial;

    @NotNull
    @DecimalMin("0.00")
    @Column(name = "solde_final", nullable = false, precision = 15, scale = 2)
    private BigDecimal soldeFinal;

    @Column(name = "total_credits", precision = 15, scale = 2)
    private BigDecimal totalCredits = BigDecimal.ZERO;

    @Column(name = "total_debits", precision = 15, scale = 2)
    private BigDecimal totalDebits = BigDecimal.ZERO;

    @Column(name = "nombre_operations")
    private Integer nombreOperations = 0;

    @Column(name = "mois")
    private Integer mois;   // 1..12

    @Column(name = "annee")
    private Integer annee;  // ex: 2025

    @Column(name = "pdf_genere", nullable = false)
    private boolean pdfGenere = false; // bit(1) en BDD

    @Column(name = "chemin_pdf")
    private String cheminPdf;

    // ===== Audit =====
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== Relation (FK: compte_bancaire_id) =====
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_bancaire_id", nullable = false)
    private CompteBancaire compte;

    // ===== Ctors =====
    public ReleveDeCompte() {}

    // ===== Callbacks =====
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (dateReleve == null) dateReleve = LocalDate.now();
        if (mois == null) mois = dateReleve.getMonthValue();
        if (annee == null) annee = dateReleve.getYear();
        if (totalCredits == null) totalCredits = BigDecimal.ZERO;
        if (totalDebits == null) totalDebits = BigDecimal.ZERO;
        if (nombreOperations == null) nombreOperations = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== Utils =====
    public BigDecimal getVariation() {
        if (soldeFinal == null || soldeInitial == null) return BigDecimal.ZERO;
        return soldeFinal.subtract(soldeInitial);
    }

    // ===== Getters/Setters =====
    public Long getId() { return id; }

    public LocalDate getDateReleve() { return dateReleve; }
    public void setDateReleve(LocalDate dateReleve) { this.dateReleve = dateReleve; }

    public BigDecimal getSoldeInitial() { return soldeInitial; }
    public void setSoldeInitial(BigDecimal soldeInitial) { this.soldeInitial = soldeInitial; }

    public BigDecimal getSoldeFinal() { return soldeFinal; }
    public void setSoldeFinal(BigDecimal soldeFinal) { this.soldeFinal = soldeFinal; }

    public BigDecimal getTotalCredits() { return totalCredits; }
    public void setTotalCredits(BigDecimal totalCredits) { this.totalCredits = totalCredits; }

    public BigDecimal getTotalDebits() { return totalDebits; }
    public void setTotalDebits(BigDecimal totalDebits) { this.totalDebits = totalDebits; }

    public Integer getNombreOperations() { return nombreOperations; }
    public void setNombreOperations(Integer nombreOperations) { this.nombreOperations = nombreOperations; }

    public Integer getMois() { return mois; }
    public void setMois(Integer mois) { this.mois = mois; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public boolean isPdfGenere() { return pdfGenere; }
    public void setPdfGenere(boolean pdfGenere) { this.pdfGenere = pdfGenere; }

    public String getCheminPdf() { return cheminPdf; }
    public void setCheminPdf(String cheminPdf) { this.cheminPdf = cheminPdf; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public CompteBancaire getCompte() { return compte; }
    public void setCompte(CompteBancaire compte) { this.compte = compte; }

    // ===== equals/hashCode =====
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReleveDeCompte that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
