package com.banking.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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

    /**
     * Calcul mensuel fixe — logique bancaire standard pour un livret d'épargne.
     *
     * Formule : solde × taux_annuel / 12
     *
     * Le montant est FIXE pour toute la période mensuelle.
     * Il ne change pas chaque jour — il est calculé une fois à l'ouverture
     * et crédité en totalité à la capitalisation mensuelle.
     *
     * Ex : 300 € × 3 % / 12 = 0,75 € par mois (constant)
     */
    public BigDecimal calculer(BigDecimal solde) {
        if (solde == null || tauxInteret == null) return BigDecimal.ZERO;
        if (solde.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        // ✅ Intérêt mensuel fixe = solde × taux_annuel / 12
        this.montantInteret = solde
                .multiply(tauxInteret)
                .divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);

        return this.montantInteret;
    }

    /**
     * Capitalisation : crédite le solde et clôture la période.
     * Appelée une fois par mois par le scheduler ou manuellement par l'admin.
     */
    public void capitaliser(CompteEpargne epargne) {
        // Recalculer si montant pas encore calculé
        if (this.montantInteret == null || this.montantInteret.signum() <= 0) {
            calculer(epargne.getBalance());
        }
        // Créditer le solde si montant positif
        if (this.montantInteret != null && this.montantInteret.signum() > 0) {
            epargne.crediter(this.montantInteret);
        }
        this.dateCapitalisation = LocalDate.now();
        this.dateFin            = LocalDate.now();
    }

    /**
     * Retourne vrai si la période d'un mois est écoulée depuis la dernière capitalisation.
     */
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