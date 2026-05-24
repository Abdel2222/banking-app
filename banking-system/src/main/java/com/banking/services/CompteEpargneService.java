package com.banking.services;

import com.banking.dto.response.CompteEpargneResponse;
import com.banking.entities.CompteEpargne;
import java.math.BigDecimal;

public interface CompteEpargneService {

    CompteEpargneResponse convertirDepuisCompte(String numCompteBancaire, BigDecimal premierMontant);
    CompteEpargneResponse convertir(String numCompte, BigDecimal premierMontant);

    CompteEpargneResponse alimenter(String numCompte, BigDecimal montant);
    CompteEpargneResponse alimenterDepuis(String numCompteEpargne, BigDecimal montant, String numCompteSource); // ← nouveau
    CompteEpargneResponse retirer(String numCompte, BigDecimal montant);

    CompteEpargneResponse findByCompteId(Long compteId);
    CompteEpargneResponse getDetails(String numCompte);
    CompteEpargne findByNumCompte(String numCompte);
}