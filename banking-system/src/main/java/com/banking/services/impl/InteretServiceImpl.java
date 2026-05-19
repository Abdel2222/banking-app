package com.banking.services.impl;

import com.banking.entities.CompteEpargne;
import com.banking.entities.Interet;
import com.banking.exceptions.ResourceNotFoundException;
import com.banking.repositories.CompteEpargneRepository;
import com.banking.repositories.InteretRepository;
import com.banking.services.InteretService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InteretServiceImpl implements InteretService {

    private final InteretRepository      interetRepository;
    private final CompteEpargneRepository epargneRepository;

    private CompteEpargne getEpargneOrThrow(String numCompte) {
        return epargneRepository.findByNumCompte(numCompte)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Compte épargne introuvable: " + numCompte));
    }

    @Override
    @Transactional
    public Interet creer(String numCompte, BigDecimal tauxInteret) {
        if (tauxInteret == null || tauxInteret.signum() <= 0)
            throw new IllegalArgumentException("Taux invalide");

        CompteEpargne epargne = getEpargneOrThrow(numCompte);

        Interet interet = new Interet(epargne, tauxInteret, LocalDate.now());
        return interetRepository.save(interet);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculer(String numCompte) {
        CompteEpargne epargne = getEpargneOrThrow(numCompte);

        Interet interet = interetRepository
                .findTopByCompteEpargneOrderByDateDebutDesc(epargne)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun intérêt défini pour ce compte"));

        return interet.calculer(epargne.getBalance());
    }

    @Override
    @Transactional
    public Interet capitaliser(String numCompte) {
        CompteEpargne epargne = getEpargneOrThrow(numCompte);

        Interet interet = interetRepository
                .findTopByCompteEpargneOrderByDateDebutDesc(epargne)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun intérêt défini pour ce compte"));

        if (!interet.doitCapitaliser())
            throw new IllegalStateException("Capitalisation pas encore due");

        interet.capitaliser(epargne);       // crédite le solde + set dateCapitalisation
        epargneRepository.save(epargne);    // save le solde mis à jour
        interetRepository.save(interet);    // save la date de capitalisation

        return interet;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean doitCapitaliser(String numCompte) {
        CompteEpargne epargne = getEpargneOrThrow(numCompte);
        return interetRepository
                .findTopByCompteEpargneOrderByDateDebutDesc(epargne)
                .map(Interet::doitCapitaliser)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Interet> historique(String numCompte) {
        return interetRepository.findByCompteEpargne_NumCompte(numCompte);
    }

    @Override
    @Transactional
    public Interet updateTaux(String numCompte, BigDecimal nouveauTaux) {
        if (nouveauTaux == null || nouveauTaux.signum() <= 0)
            throw new IllegalArgumentException("Taux invalide");

        CompteEpargne epargne = getEpargneOrThrow(numCompte);

        Interet interet = interetRepository
                .findTopByCompteEpargneOrderByDateDebutDesc(epargne)
                .orElseGet(() -> new Interet(epargne, nouveauTaux, LocalDate.now()));

        interet.setTauxInteret(nouveauTaux);
        return interetRepository.save(interet);
    }
}