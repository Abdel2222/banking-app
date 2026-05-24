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

    /* ====== Helpers ====== */

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

    /* ====== Convertir ====== */

    @Override
    @Transactional
    public CompteEpargneResponse convertirDepuisCompte(String numCompteBancaire, BigDecimal premierMontant) {
        CompteBancaire cb = getCompteOrThrow(numCompteBancaire);

        if (epargneRepo.findByNumCompte(numCompteBancaire).isPresent())
            throw new IllegalStateException("Un compte épargne existe déjà pour ce compte");

        BigDecimal montant = (premierMontant != null && premierMontant.signum() > 0)
                ? premierMontant : BigDecimal.ZERO;

        CompteEpargne ce = new CompteEpargne();
        ce.setNumCompte(CompteBancaire.generateAccountNumber());
        ce.setClient(cb.getClient());
        ce.setDevise(cb.getDevise());
        ce.setIntitule("Compte épargne");
        ce.setBalance(montant);
        ce.setPremierMontant(montant);
        ce.activate();

        return mapper.toResponse(epargneRepo.save(ce));
    }

    @Override
    @Transactional
    public CompteEpargneResponse convertir(String numCompte, BigDecimal premierMontant) {
        return convertirDepuisCompte(numCompte, premierMontant);
    }

    /* ====== Alimenter simple (crédit direct) ====== */

    @Override
    @Transactional
    public CompteEpargneResponse alimenter(String numCompte, BigDecimal montant) {
        if (montant == null || montant.signum() <= 0)
            throw new IllegalArgumentException("Montant invalide");

        CompteEpargne ce = getEpargneOrThrow(numCompte);
        ce.crediter(montant);
        epargneRepo.save(ce);
        return mapper.toResponse(ce);
    }

    /* ====== Alimenter depuis un compte courant (virement interne) ====== */

    @Override
    @Transactional
    public CompteEpargneResponse alimenterDepuis(
            String numCompteEpargne,
            BigDecimal montant,
            String numCompteSource) {

        if (montant == null || montant.signum() <= 0)
            throw new IllegalArgumentException("Montant invalide");

        // Compte épargne destinataire
        CompteEpargne epargne = getEpargneOrThrow(numCompteEpargne);

        // Compte courant source
        CompteBancaire source = getCompteOrThrow(numCompteSource);

        if (source.getBalance().compareTo(montant) < 0)
            throw new IllegalStateException("Solde insuffisant sur le compte source");

        // Débiter la source, créditer l'épargne
        source.debiter(montant);
        epargne.crediter(montant);

        compteRepo.save(source);
        epargneRepo.save(epargne);

        return mapper.toResponse(epargne);
    }

    /* ====== Retirer ====== */

    @Override
    @Transactional
    public CompteEpargneResponse retirer(String numCompte, BigDecimal montant) {
        if (montant == null || montant.signum() <= 0)
            throw new IllegalArgumentException("Montant invalide");

        CompteEpargne ce = getEpargneOrThrow(numCompte);
        ce.debiter(montant);
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
        return mapper.toResponse(
                epargneRepo.findById(compteId)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Compte épargne introuvable pour id=" + compteId)));
    }

    @Override
    @Transactional(readOnly = true)
    public CompteEpargne findByNumCompte(String numCompte) {
        return getEpargneOrThrow(numCompte);
    }
}