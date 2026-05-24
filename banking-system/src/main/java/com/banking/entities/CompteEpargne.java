package com.banking.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comptes_epargne")
@Getter @Setter
@NoArgsConstructor
public class CompteEpargne extends CompteBancaire {

    @Column(name = "premier_montant", precision = 19, scale = 4, nullable = false)
    private BigDecimal premierMontant = BigDecimal.ZERO;

    @OneToMany(mappedBy = "compteEpargne", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"compteEpargne", "hibernateLazyInitializer"})
    private List<Interet> interets = new ArrayList<>();

    /* ===================== Constructeur ===================== */

    public CompteEpargne(BigDecimal premierMontant) {
        super();
        this.premierMontant = premierMontant != null ? premierMontant : BigDecimal.ZERO;
    }

    /* ===================== Helper relation ===================== */

    public void ajouterInteret(Interet interet) {
        interets.add(interet);
        interet.setCompteEpargne(this);
    }

    /* ===================== Méthodes métier ===================== */

    public void crediter(BigDecimal montant) {
        if (montant != null && montant.signum() > 0) {
            this.setBalance(this.getBalance().add(montant));
        }
    }

    public void debiter(BigDecimal montant) {
        if (montant != null && montant.signum() > 0) {
            if (this.getBalance().compareTo(montant) < 0) {
                throw new RuntimeException("Solde insuffisant");
            }
            this.setBalance(this.getBalance().subtract(montant));
        }
    }
}