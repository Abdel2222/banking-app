package com.banking.services.impl;

import com.banking.dto.response.CompteEpargneResponse;
import com.banking.entities.CompteBancaire;
import com.banking.entities.CompteEpargne;
import com.banking.mappers.CompteEpargneMapper;
import com.banking.repositories.CompteBancaireRepository;
import com.banking.repositories.CompteEpargneRepository;
import com.banking.services.CompteEpargneService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CompteEpargneServiceImpl implements CompteEpargneService {

    private final CompteBancaireRepository compteRepo;
    private final CompteEpargneRepository  epargneRepo;
    private final CompteEpargneMapper      mapper;

    /* ====== Helper ====== */

    private CompteBancaire getCompteOrThrow(String numCompte) {
        return compteRepo.findByNumCompte(numCompte)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Compte bancaire introuvable: " + numCompte));
    }

    private CompteEpargne getEpargneOrThrow(String numCompte) {
        return epargneRepo.findByNumCompte(numCompte)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Compte épargne introuvable pour: " + numCompte));
    }

    /* ====== Créer un compte épargne ====== */

    @Override
    @Transactional
    public CompteEpargneResponse convertirDepuisCompte(String numCompteBancaire, BigDecimal premierMontant) {

        // Vérifier que le compte bancaire existe
        CompteBancaire cb = getCompteOrThrow(numCompteBancaire);

        // Vérifier qu'il n'y a pas déjà un compte épargne
        if (epargneRepo.findByNumCompte(numCompteBancaire).isPresent())
            throw new IllegalStateException("Un compte épargne existe déjà pour ce compte");

        // Vérifier que le solde du compte bancaire est suffisant
        BigDecimal montant = (premierMontant != null && premierMontant.signum() > 0)
                ? premierMontant
                : BigDecimal.ZERO;

        // Créer le CompteEpargne — hérite de CompteBancaire via JOINED
        CompteEpargne ce = new CompteEpargne();
        ce.setNumCompte(CompteBancaire.generateAccountNumber()); // numéro propre
        ce.setClient(cb.getClient());
        ce.setDevise(cb.getDevise());
        ce.setIntitule("Compte épargne");
        ce.setBalance(montant);
        ce.setPremierMontant(montant);
        ce.activate();

        ce = epargneRepo.save(ce);
        return mapper.toResponse(ce);
    }

    @Override
    @Transactional
    public CompteEpargneResponse convertir(String numCompte, BigDecimal premierMontant) {
        return convertirDepuisCompte(numCompte, premierMontant);
    }

    /* ====== Alimenter ====== */

    @Override
    @Transactional
    public CompteEpargneResponse alimenter(String numCompte, BigDecimal montant) {
        if (montant == null || montant.signum() <= 0)
            throw new IllegalArgumentException("Montant invalide");

        CompteEpargne ce = getEpargneOrThrow(numCompte);
        ce.crediter(montant); // méthode héritée de CompteBancaire
        epargneRepo.save(ce);
        return mapper.toResponse(ce);
    }

    /* ====== Retirer ====== */

    @Override
    @Transactional
    public CompteEpargneResponse retirer(String numCompte, BigDecimal montant) {
        if (montant == null || montant.signum() <= 0)
            throw new IllegalArgumentException("Montant invalide");

        CompteEpargne ce = getEpargneOrThrow(numCompte);
        ce.debiter(montant); // méthode héritée de CompteBancaire
        epargneRepo.save(ce);
        return mapper.toResponse(ce);
    }

    /* ====== Consulter ====== */

    @Override
    @Transactional(readOnly = true)
    public CompteEpargneResponse getDetails(String numCompte) {
        return mapper.toResponse(getEpargneOrThrow(numCompte));
    }

    @Override
    @Transactional(readOnly = true)
    public CompteEpargneResponse findByCompteId(Long compteId) {
        CompteEpargne ce = epargneRepo.findById(compteId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Compte épargne introuvable pour id=" + compteId));
        return mapper.toResponse(ce);
    }

    @Override
    @Transactional(readOnly = true)
    public CompteEpargne findByNumCompte(String numCompte) {
        return getEpargneOrThrow(numCompte);
    }
}