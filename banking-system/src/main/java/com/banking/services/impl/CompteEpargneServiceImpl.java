package com.banking.services.impl;

import com.banking.entities.CompteBancaire;
import com.banking.entities.CompteEpargne;
import com.banking.entities.Operation;
import com.banking.entity.enums.TypeOperation;
import com.banking.exceptions.ResourceNotFoundException;
import com.banking.repositories.CompteBancaireRepository;
import com.banking.repositories.CompteEpargneRepository;
import com.banking.repositories.OperationRepository;
import com.banking.services.CompteEpargneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class CompteEpargneServiceImpl implements CompteEpargneService {

    private final CompteBancaireRepository compteRepo;
    private final CompteEpargneRepository epargneRepo;
    private final OperationRepository operationRepo;

    /* ================== Utils ================== */

    private void assertPositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(field + " doit être strictement positif");
        }
    }

    private CompteBancaire getCompteOrThrowByNum(String numCompte) {
        return compteRepo.findByNumCompte(numCompte)
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé: " + numCompte));
    }

    private CompteEpargne getEpargneOrThrow(CompteBancaire compte) {
        return epargneRepo.findByCompteBancaire(compte)
                .orElseThrow(() -> new ResourceNotFoundException("Compte épargne introuvable pour " + compte.getNumCompte()));
    }

    /* ================== Lecture ================== */

    @Override
    @Transactional(readOnly = true)
    public CompteEpargne findByCompteId(Long compteId) {
        CompteBancaire compte = compteRepo.findById(compteId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé: id=" + compteId));
        return getEpargneOrThrow(compte);
    }

    @Override
    @Transactional(readOnly = true)
    public CompteEpargne findByNumCompte(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        return getEpargneOrThrow(compte);
    }

    /* ================== Intérêts ================== */

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculerInterets(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        CompteEpargne epargne = getEpargneOrThrow(compte);
        return epargne.calculerInteretsAnnuels();
    }

    @Override
    public void capitaliserInterets(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        CompteEpargne epargne = getEpargneOrThrow(compte);
        epargne.capitaliserInterets();
        epargneRepo.save(epargne);

        // trace optionnelle (montant = intérêts)
        BigDecimal interets = epargne.calculerInteretsAnnuels();
        Operation op = new Operation(compte, BigDecimal.ZERO, TypeOperation.DEPOT, "Capitalisation intérêts épargne");
        op.setDateOperation(LocalDateTime.now());
        operationRepo.save(op);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean doitCapitaliser(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        CompteEpargne epargne = getEpargneOrThrow(compte);
        return epargne.doitCapitaliser();
    }

    /* ========== Mouvement courant ↔ épargne (même compte) ========== */

    @Override
    public CompteEpargne alimenter(String numCompte, BigDecimal montant) {
        assertPositive(montant, "Montant");
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        if (!compte.isActive()) throw new IllegalStateException("Le compte n'est pas actif");

        // débiter le courant
        compte.debiter(montant);
        compteRepo.save(compte);

        // créditer l'épargne
        CompteEpargne epargne = getEpargneOrThrow(compte);
        epargne.alimenter(montant);
        epargneRepo.save(epargne);

        // tracer
        Operation op = new Operation(compte, montant, TypeOperation.VIREMENT, "Virement vers épargne");
        op.setNumeroCompteDestinataire(numCompte);
        op.setCommunication("Courant -> Epargne");
        operationRepo.save(op);

        return epargne;
    }

    @Override
    public CompteEpargne retirer(String numCompte, BigDecimal montant) {
        assertPositive(montant, "Montant");
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        if (!compte.isActive()) throw new IllegalStateException("Le compte n'est pas actif");

        // débiter l'épargne
        CompteEpargne epargne = getEpargneOrThrow(compte);
        epargne.retirer(montant);
        epargneRepo.save(epargne);

        // créditer le courant
        compte.crediter(montant);
        compteRepo.save(compte);

        // tracer
        Operation op = new Operation(compte, montant, TypeOperation.VIREMENT, "Virement depuis épargne");
        op.setNumeroCompteDestinataire(numCompte);
        op.setCommunication("Epargne -> Courant");
        operationRepo.save(op);

        return epargne;
    }

    /* ================== Taux ================== */

    @Override
    public CompteEpargne updateTaux(String numCompte, BigDecimal tauxInteret) {
        if (tauxInteret == null || tauxInteret.signum() < 0 || tauxInteret.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Taux d'intérêt doit être entre 0 et 1 (ex: 0.025 = 2.5%)");
        }
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        CompteEpargne epargne = getEpargneOrThrow(compte);
        epargne.setTauxInteret(tauxInteret);
        return epargneRepo.save(epargne);
    }
}
