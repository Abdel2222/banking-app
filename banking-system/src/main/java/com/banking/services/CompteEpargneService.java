package com.banking.services;

import com.banking.entities.CompteEpargne;

import java.math.BigDecimal;

public interface CompteEpargneService {

    // Lecture
    CompteEpargne findByCompteId(Long compteId);
    CompteEpargne findByNumCompte(String numCompte);

    // Intérêts
    BigDecimal calculerInterets(String numCompte);
    void capitaliserInterets(String numCompte);
    boolean doitCapitaliser(String numCompte);

    // Mouvement courant ↔ épargne (même compte)
    CompteEpargne alimenter(String numCompte, BigDecimal montant); // courant -> épargne
    CompteEpargne retirer(String numCompte, BigDecimal montant);   // épargne -> courant

    // Taux
    CompteEpargne updateTaux(String numCompte, BigDecimal tauxInteret);
}
