// src/main/java/com/banking/services/CompteEpargneService.java
package com.banking.services;

import com.banking.dto.response.CompteEpargneResponse;
import com.banking.entities.CompteEpargne;

import java.math.BigDecimal;

public interface CompteEpargneService {

    // Création / conversion
    CompteEpargneResponse convertirDepuisCompte(String numCompteBancaire, BigDecimal tauxInteret);
    CompteEpargneResponse convertir(String numCompte, BigDecimal tauxInteret);

    // Mouvements
    CompteEpargneResponse alimenter(String numCompte, BigDecimal montant);
    CompteEpargneResponse retirer(String numCompte, BigDecimal montant);

    // Taxe virtuelle
    BigDecimal getTaxationVirtuelle(String numCompte);

    // Lectures
    CompteEpargneResponse findByCompteId(Long compteId);
    CompteEpargneResponse getDetails(String numCompte);

    // Intérêts
    BigDecimal calculerInterets(String numCompte);
    CompteEpargneResponse capitaliserInterets(String numCompte);

    // Règle de capitalisation
    boolean doitCapitaliser(String numCompte);

    // Taux
    CompteEpargneResponse updateTaux(String numCompte, BigDecimal tauxInteret);

    // (optionnel) Compat héritage si du code existant l’utilise encore
    CompteEpargne findByNumCompte(String numCompte);

    Object capitaliser(String numCompte);
}
