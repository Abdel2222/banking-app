package com.banking.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "interets")
public class Interet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "taux_interet", precision = 10, scale = 6, nullable = false)
    private BigDecimal tauxInteret;

    @Column(name = "montant_interet", precision = 19, scale = 4)
    private BigDecimal montantInteret = BigDecimal.ZERO;

    @Column(name = "date_capitalisation")
    private LocalDate dateCapitalisation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_epargne_id", nullable = false)
    @JsonIgnoreProperties({"interets", "operations", "fraisDeGestions",
            "carteBancaire", "hibernateLazyInitializer"})
    private CompteEpargne compteEpargne;

    /* ===================== Constructeurs ===================== */

    public Interet() {}

    public Interet(CompteEpargne compteEpargne, BigDecimal tauxInteret, LocalDate dateDebut) {
        this.compteEpargne = compteEpargne;
        this.tauxInteret   = tauxInteret;
        this.dateDebut     = dateDebut;
    }

    /* ===================== Méthodes métier ===================== */

    public BigDecimal calculer(BigDecimal solde) {
        if (solde == null || tauxInteret == null) return BigDecimal.ZERO;
        long jours = ChronoUnit.DAYS.between(
                dateDebut,
                dateFin != null ? dateFin : LocalDate.now());
        if (jours <= 0) return BigDecimal.ZERO;
        BigDecimal ratio = BigDecimal.valueOf(jours)
                .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
        this.montantInteret = solde.multiply(tauxInteret)
                .multiply(ratio)
                .setScale(4, RoundingMode.HALF_UP);
        return this.montantInteret;
    }

    public void capitaliser(CompteEpargne epargne) {
        if (this.montantInteret == null || this.montantInteret.signum() <= 0)
            calculer(epargne.getBalance());
        if (this.montantInteret.signum() > 0)
            epargne.crediter(this.montantInteret);
        this.dateCapitalisation = LocalDate.now();
        this.dateFin            = LocalDate.now();
    }

    public boolean doitCapitaliser() {
        if (dateCapitalisation == null) return true;
        return LocalDate.now().isAfter(dateCapitalisation.plusMonths(1));
    }

    /* ===================== Getters / Setters ===================== */

    public Long getId()                            { return id; }
    public LocalDate getDateDebut()                { return dateDebut; }
    public void setDateDebut(LocalDate d)          { this.dateDebut = d; }
    public LocalDate getDateFin()                  { return dateFin; }
    public void setDateFin(LocalDate d)            { this.dateFin = d; }
    public BigDecimal getTauxInteret()             { return tauxInteret; }
    public void setTauxInteret(BigDecimal t)       { this.tauxInteret = t; }
    public BigDecimal getMontantInteret()          { return montantInteret; }
    public void setMontantInteret(BigDecimal m)    { this.montantInteret = m; }
    public LocalDate getDateCapitalisation()       { return dateCapitalisation; }
    public void setDateCapitalisation(LocalDate d) { this.dateCapitalisation = d; }
    public CompteEpargne getCompteEpargne()        { return compteEpargne; }
    public void setCompteEpargne(CompteEpargne ce) { this.compteEpargne = ce; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Interet)) return false;
        return Objects.equals(id, ((Interet) o).id);
    }

    @Override public int hashCode() { return Objects.hash(id); }
}