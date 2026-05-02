package com.banking.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "comptes_epargne")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompteEpargne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Lien 1–1 vers le compte bancaire (aucune modif côté CompteBancaire) */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compte_bancaire_id", nullable = false, unique = true)
    private CompteBancaire compteBancaire;

    /** Numéro propre d’épargne (3 premiers chiffres du compte bancaire + aléatoire) */
    @Column(name = "num_compte_epargne", nullable = false, unique = true, length = 16)
    private String numCompteEpargne;

    /** Solde épargne (garde scale=4 si ta DB est déjà ainsi) */
    @Builder.Default
    @Column(name = "solde_epargne", nullable = false, precision = 19, scale = 4)
    private BigDecimal soldeEpargne = BigDecimal.ZERO;

    /** Taux d’intérêt décimal (ex: 0.0125 = 1.25%) */
    @Builder.Default
    @Column(name = "taux_interet", nullable = false, precision = 7, scale = 6)
    private BigDecimal tauxInteret = new BigDecimal("0.010000");

    @Column(name = "derniere_capitalisation")
    private LocalDateTime dateDerniereCapitalisation;

    /** Colonne VIRTUELLE (30€ si balance>100 ; 10€ si balance<50 ; sinon 0) */
    @Formula(
            "(CASE " +
                    " WHEN (SELECT cb.balance FROM comptes_bancaires cb WHERE cb.id = compte_bancaire_id) > 100 THEN 30.00 " +
                    " WHEN (SELECT cb.balance FROM comptes_bancaires cb WHERE cb.id = compte_bancaire_id) < 50  THEN 10.00 " +
                    " ELSE 0.00 END)"
    )
    private BigDecimal taxationVirtuelle;

    /* ==== Petite logique métier locale (épargne seulement) ==== */

    public BigDecimal calculerInteretsAnnuels() {
        if (tauxInteret == null || tauxInteret.signum() <= 0) return BigDecimal.ZERO;
        return soldeEpargne.multiply(tauxInteret).setScale(4, RoundingMode.HALF_UP);
    }

    public boolean doitCapitaliser() {
        return dateDerniereCapitalisation == null
                || dateDerniereCapitalisation.plusYears(1).isBefore(LocalDateTime.now());
    }

    public void capitaliserInterets() {
        BigDecimal interets = calculerInteretsAnnuels();
        if (interets.signum() > 0) {
            soldeEpargne = soldeEpargne.add(interets);
        }
        dateDerniereCapitalisation = LocalDateTime.now();
    }

    public void alimenter(BigDecimal montant) {
        requirePositive(montant, "Montant invalide");
        soldeEpargne = soldeEpargne.add(montant);
    }

    public void retirer(BigDecimal montant) {
        requirePositive(montant, "Montant invalide");
        if (soldeEpargne.compareTo(montant) < 0) throw new IllegalStateException("Solde épargne insuffisant");
        soldeEpargne = soldeEpargne.subtract(montant);
    }

    public void setTauxInteret(BigDecimal t) {
        if (t == null || t.signum() < 0) throw new IllegalArgumentException("Taux invalide");
        this.tauxInteret = t;
    }

    private void requirePositive(BigDecimal v, String msg) {
        if (v == null || v.signum() <= 0) throw new IllegalArgumentException(msg);
    }
}
