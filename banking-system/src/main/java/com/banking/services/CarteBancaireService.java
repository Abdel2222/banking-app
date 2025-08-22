package com.banking.services;

import com.banking.dto.response.CarteBancaireResponse;
import com.banking.entities.CarteBancaire;

import java.util.List;
import java.util.Optional;

public interface CarteBancaireService {

    // Consultation
    Optional<CarteBancaire> findById(Long id);
    Optional<CarteBancaire> findByCompteId(Long compteId);
    Optional<CarteBancaire> findByNumeroCarte(String numeroCarte);
    List<CarteBancaire> findAllActives();

    // État
    CarteBancaire activer(Long carteId);
    CarteBancaire desactiver(Long carteId);
    boolean estExpiree(Long carteId);

    // Paramètres
    CarteBancaire mettreAJourPlafonds(Long carteId, Double plafondJournalier, Double plafondMensuel);
        CarteBancaireResponse activateCard(Long cardId);


    // Émission (ADMIN) — utilisée par AdminController
    CarteBancaire emettrePourCompte(Long compteId, boolean estActive, Double plafondJournalier, Double plafondMensuel);
}
