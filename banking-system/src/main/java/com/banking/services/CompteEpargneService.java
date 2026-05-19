package com.banking.services;

import com.banking.dto.response.CompteEpargneResponse;
import com.banking.entities.CompteEpargne;

import java.math.BigDecimal;

public interface CompteEpargneService {

    // Création / conversion
    CompteEpargneResponse convertirDepuisCompte(String numCompteBancaire, BigDecimal premierMontant);
    CompteEpargneResponse convertir(String numCompte, BigDecimal premierMontant);

    // Mouvements
    CompteEpargneResponse alimenter(String numCompte, BigDecimal montant);
    CompteEpargneResponse retirer(String numCompte, BigDecimal montant);

    // Lectures
    CompteEpargneResponse findByCompteId(Long compteId);
    CompteEpargneResponse getDetails(String numCompte);
    CompteEpargne findByNumCompte(String numCompte);
}