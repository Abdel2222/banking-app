package com.banking.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "comptes_epargne")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CompteEpargne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ Relation OneToOne (FK unique) vers CompteBancaire
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compte_bancaire_id", nullable = false, unique = true)
    @JsonIgnore   // 👈 évite les boucles infinies dans les réponses JSON
    private CompteBancaire compteBancaire;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal soldeEpargne = BigDecimal.ZERO;

    @Column(nullable = false, precision = 7, scale = 6) // ex: 0.025000 = 2.5%
    private BigDecimal tauxInteret;

    private LocalDateTime dateDerniereCapitalisation;

    /* ======== Méthodes métier ======== */

    public void alimenter(BigDecimal montant) {
        requirePositive(montant, "Montant invalide");
        this.soldeEpargne = this.soldeEpargne.add(montant);
    }

    public void retirer(BigDecimal montant) {
        requirePositive(montant, "Montant invalide");
        if (this.soldeEpargne.compareTo(montant) < 0) {
            throw new IllegalStateException("Solde épargne insuffisant");
        }
        this.soldeEpargne = this.soldeEpargne.subtract(montant);
    }

    public BigDecimal calculerInteretsAnnuels() {
        if (tauxInteret == null || tauxInteret.signum() <= 0) return BigDecimal.ZERO;
        return this.soldeEpargne.multiply(tauxInteret).setScale(4, RoundingMode.HALF_UP);
    }

    public void capitaliserInterets() {
        BigDecimal interets = calculerInteretsAnnuels();
        if (interets.signum() > 0) {
            this.soldeEpargne = this.soldeEpargne.add(interets);
        }
        this.dateDerniereCapitalisation = LocalDateTime.now();
    }

    public boolean doitCapitaliser() {
        return dateDerniereCapitalisation == null ||
                dateDerniereCapitalisation.plusYears(1).isBefore(LocalDateTime.now());
    }

    public BigDecimal applyMonthlyInterest() {
        if (tauxInteret == null || tauxInteret.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal mensual = tauxInteret.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        BigDecimal interets = this.soldeEpargne.multiply(mensual).setScale(4, RoundingMode.HALF_UP);
        this.soldeEpargne = this.soldeEpargne.add(interets);
        return interets;
    }

    /* ======== Helpers ======== */

    public void setTauxInteret(BigDecimal taux) {
        if (taux == null || taux.signum() < 0 || taux.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Taux d'intérêt doit être entre 0 et 1 (ex: 0.025 = 2.5%)");
        }
        this.tauxInteret = taux;
    }

    private void requirePositive(BigDecimal value, String msg) {
        if (value == null || value.signum() <= 0) throw new IllegalArgumentException(msg);
    }
}
