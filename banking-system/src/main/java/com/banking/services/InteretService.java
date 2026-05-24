package com.banking.services;

import com.banking.entities.CompteEpargne;
import com.banking.entities.Interet;
import com.banking.entity.enums.AccountStatus;


import com.banking.repositories.CompteEpargneRepository;
import com.banking.repositories.InteretRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InteretService {

    private final InteretRepository interetRepository;
    private final CompteEpargneRepository compteEpargneRepository;

    /** Créer un intérêt pour un compte épargne */
    @Transactional
    public Interet creer(String numCompte, BigDecimal taux) {
        CompteEpargne compte = compteEpargneRepository.findByNumCompte(numCompte)
                .orElseThrow(() -> new RuntimeException("Compte épargne introuvable : " + numCompte));

        // Vérifier qu'il n'y a pas déjà un intérêt actif
        interetRepository.findInteretActif(numCompte).ifPresent(i -> {
            throw new RuntimeException("Un intérêt actif existe déjà pour ce compte");
        });

        Interet interet = new Interet(compte, taux, LocalDate.now());
        return interetRepository.save(interet);
    }

    /** Calculer le montant des intérêts en cours (sans capitaliser) */
    @Transactional(readOnly = true)
    public BigDecimal calculer(String numCompte) {
        CompteEpargne compte = compteEpargneRepository.findByNumCompte(numCompte)
                .orElseThrow(() -> new RuntimeException("Compte épargne introuvable : " + numCompte));

        return interetRepository.findInteretActif(numCompte)
                .map(i -> i.calculer(compte.getBalance()))
                .orElse(BigDecimal.ZERO);
    }

    /** Capitaliser : crédite le solde + clôture la période */
    @Transactional
    public Interet capitaliser(String numCompte) {
        CompteEpargne compte = compteEpargneRepository.findByNumCompte(numCompte)
                .orElseThrow(() -> new RuntimeException("Compte épargne introuvable : " + numCompte));

        Interet interetActif = interetRepository.findInteretActif(numCompte)
                .orElseThrow(() -> new RuntimeException("Aucun intérêt actif pour ce compte"));

        LocalDate aujourdHui = LocalDate.now();

        // sécurité : éviter une double capitalisation le même jour
        if (interetActif.getDateCapitalisation() != null &&
                interetActif.getDateCapitalisation().isEqual(aujourdHui)) {
            return interetActif;
        }

        // calcul + clôture de la période courante
        interetActif.calculer(compte.getBalance());
        interetActif.capitaliser(compte);
        compteEpargneRepository.save(compte);
        interetRepository.save(interetActif);

        // vérifier qu'il n'existe pas déjà un nouvel intérêt actif ouvert aujourd'hui
        boolean nouvelInteretExiste = interetRepository.findInteretActif(numCompte)
                .map(i -> i.getDateDebut() != null && i.getDateDebut().isEqual(aujourdHui))
                .orElse(false);

        if (!nouvelInteretExiste) {
            Interet nouveau = new Interet(compte, interetActif.getTauxInteret(), aujourdHui);
            interetRepository.save(nouveau);
        }

        return interetActif;
    }
    /** Historique des intérêts d'un compte */
    @Transactional(readOnly = true)
    public List<Interet> historique(String numCompte) {
        return interetRepository.findByCompteEpargne_NumCompteOrderByDateDebutDesc(numCompte);
    }

    /** Modifier le taux de l'intérêt actif */
    @Transactional
    public Interet updateTaux(String numCompte, BigDecimal valeur) {
        Interet interet = interetRepository.findInteretActif(numCompte)
                .orElseThrow(() -> new RuntimeException("Aucun intérêt actif pour ce compte"));

        interet.setTauxInteret(valeur);
        return interetRepository.save(interet);
    }

    /** Appliquer un taux à TOUS les comptes épargne actifs */
    @Transactional
    public List<Interet> appliquerTauxATous(BigDecimal taux) {
        List<CompteEpargne> comptes = compteEpargneRepository.findByStatus(AccountStatus.ACTIVATED);
        List<Interet> resultats = new ArrayList<>();

        for (CompteEpargne compte : comptes) {
            boolean dejaActif = interetRepository.findInteretActif(compte.getNumCompte()).isPresent();
            if (!dejaActif) {
                Interet interet = new Interet(compte, taux, LocalDate.now());
                resultats.add(interetRepository.save(interet));
            }
        }
        return resultats;
    }

    /** Capitaliser TOUS les intérêts non capitalisés */
    @Transactional
    public List<Interet> capitaliserTous() {
        List<Interet> aCapitaliser = interetRepository.findAllNonCapitalises();
        List<Interet> resultats = new ArrayList<>();

        for (Interet interet : aCapitaliser) {
            CompteEpargne compte = interet.getCompteEpargne();
            interet.capitaliser(compte);
            compteEpargneRepository.save(compte);
            resultats.add(interetRepository.save(interet));
        }
        return resultats;
    }
}