package com.banking.entities;

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

    // ✅ Intérêts ici — pas dans CompteBancaire
    @OneToMany(mappedBy = "compteEpargne", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Interet> interets = new ArrayList<>();

    /* ===================== Constructeur ===================== */

    public CompteEpargne(BigDecimal premierMontant) {
        super();
        this.premierMontant = premierMontant != null ? premierMontant : BigDecimal.ZERO;
    }

    /* ===================== Helper relation ===================== */

    public void ajouterInteret(Interet interet) {
        interets.add(interet);
        interet.setCompteEpargne(this); // ✅ pointe vers CompteEpargne
    }
}