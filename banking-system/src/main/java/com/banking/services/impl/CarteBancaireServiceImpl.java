package com.banking.services.impl;

import com.banking.dto.response.CarteBancaireResponse;
import com.banking.entities.CarteBancaire;
import com.banking.entities.CompteBancaire;
import com.banking.entity.enums.CardRequestStatus;
import com.banking.exceptions.AccountNotActiveException;
import com.banking.exceptions.DuplicateResourceException;
import com.banking.exceptions.InvalidOperationException;
import com.banking.exceptions.ResourceNotFoundException;
import com.banking.repositories.CarteBancaireRepository;
import com.banking.repositories.CompteBancaireRepository;
import com.banking.repositories.DemandeCarteBancaireRepository;
import com.banking.services.CarteBancaireService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class CarteBancaireServiceImpl implements CarteBancaireService {

    private final CarteBancaireRepository carteRepo;
    private final CompteBancaireRepository compteRepo;
    private final DemandeCarteBancaireRepository demandeRepo;

    // ===== Consultation =====
    @Override
    public Optional<CarteBancaire> findById(Long id) {
        return carteRepo.findById(id);
    }

    @Override
    public Optional<CarteBancaire> findByCompteId(Long compteId) {
        return carteRepo.findByCompteBancaire_Id(compteId);
    }

    @Override
    public Optional<CarteBancaire> findByNumeroCarte(String numeroCarte) {
        return carteRepo.findByNumeroCarte(numeroCarte);
    }

    @Override
    public List<CarteBancaire> findAllActives() {
        return carteRepo.findByEstActive(true);
    }

    // ===== État =====
    @Override
    public CarteBancaire activer(Long carteId) {
        CarteBancaire c = carteRepo.findById(carteId)
                .orElseThrow(() -> new ResourceNotFoundException("Carte introuvable"));
        c.setEstActive(true);
        return carteRepo.save(c);
    }

    @Override
    public CarteBancaire desactiver(Long carteId) {
        CarteBancaire c = carteRepo.findById(carteId)
                .orElseThrow(() -> new ResourceNotFoundException("Carte introuvable"));
        c.setEstActive(false);
        return carteRepo.save(c);
    }
    @Override
    @jakarta.transaction.Transactional
    public CarteBancaireResponse activateCard(Long cardId) {
        var card = carteRepo.findById(cardId)
                .orElseThrow(() -> new com.banking.exceptions.ResourceNotFoundException("Carte introuvable: " + cardId));

        // Si tu as un enum de statut, mets-le à ACTIVE ici si besoin.
        // Sinon on active par le booléen estActive :
        card.setEstActive(true);

        var saved = carteRepo.save(card);
        return com.banking.dto.response.CarteBancaireResponse.fromEntity(saved);
    }

    @Override
    public boolean estExpiree(Long carteId) {
        return carteRepo.findById(carteId)
                .map(CarteBancaire::estExpiree)
                .orElseThrow(() -> new ResourceNotFoundException("Carte introuvable"));
    }

    // ===== Paramètres =====
    @Override
    public CarteBancaire mettreAJourPlafonds(Long carteId, Double plafondJournalier, Double plafondMensuel) {
        CarteBancaire c = carteRepo.findById(carteId)
                .orElseThrow(() -> new ResourceNotFoundException("Carte introuvable"));
        if (plafondJournalier != null) c.setPlafondJournalier(plafondJournalier);
        if (plafondMensuel != null)   c.setPlafondMensuel(plafondMensuel);
        return carteRepo.save(c);
    }

    // ===== Émission (ADMIN) =====
    @Override
    public CarteBancaire emettrePourCompte(Long compteId, boolean estActive, Double plafondJournalier, Double plafondMensuel) {
        CompteBancaire compte = compteRepo.findById(compteId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable"));

        String status = (compte.getStatus() == null) ? null : compte.getStatus().toString();
        if (!"ACTIVATED".equals(status)) {
            throw new AccountNotActiveException("Le compte n'est pas ACTIVATED");
        }

        if (carteRepo.findByCompteBancaire_Id(compteId).isPresent() || compte.getCarteBancaire() != null) {
            throw new DuplicateResourceException("Une carte existe déjà pour ce compte");
        }

        boolean hasApproved = demandeRepo.existsByCompte_IdAndStatus(compteId, CardRequestStatus.APPROVED);
        if (!hasApproved) {
            throw new InvalidOperationException("Aucune demande APPROVED pour ce compte");
        }

        String numero = generateCardNumber16();
        String cvv    = generateCVV3();
        LocalDate exp = LocalDate.now().plusYears(3);

        double pj = (plafondJournalier == null) ? 500d  : plafondJournalier;
        double pm = (plafondMensuel   == null) ? 2000d : plafondMensuel;

        CarteBancaire carte = new CarteBancaire();
        carte.setNumeroCarte(numero);
        carte.setCvv(cvv);
        carte.setDateExpiration(exp);
        carte.setEstActive(estActive);
        carte.setPlafondJournalier(pj);
        carte.setPlafondMensuel(pm);
        carte.setCompteBancaire(compte);

        CarteBancaire saved = carteRepo.save(carte);
        compte.setCarteBancaire(saved); // relation bidirectionnelle
        return saved;
    }

    // ===== Helpers =====
    private static final SecureRandom RNG = new SecureRandom();

    private String generateCardNumber16() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) sb.append(RNG.nextInt(10));
        return sb.toString();
    }

    private String generateCVV3() {
        int n = RNG.nextInt(1000);
        return String.format("%03d", n);
    }
}
