package com.banking.services;

import com.banking.entities.CompteEpargne;
import com.banking.entities.Interet;
import com.banking.entity.enums.AccountStatus;
import com.banking.repositories.CompteEpargneRepository;
import com.banking.repositories.InteretRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Compte épargne introuvable : " + numCompte
                ));

        interetRepository.findInteretActif(numCompte).ifPresent(i -> {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Un intérêt actif existe déjà pour ce compte"
            );
        });

        Interet interet = new Interet(compte, taux, LocalDate.now());
        return interetRepository.save(interet);
    }

    /** Calculer le montant des intérêts en cours (sans capitaliser) */
    @Transactional(readOnly = true)
    public BigDecimal calculer(String numCompte) {
        CompteEpargne compte = compteEpargneRepository.findByNumCompte(numCompte)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Compte épargne introuvable : " + numCompte
                ));

        return interetRepository.findInteretActif(numCompte)
                .map(i -> i.calculer(compte.getBalance()))
                .orElse(BigDecimal.ZERO);
    }

    /** Capitaliser : crédite le solde + clôture la période */
    @Transactional
    public Interet capitaliser(String numCompte) {
        CompteEpargne compte = compteEpargneRepository.findByNumCompte(numCompte)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Compte épargne introuvable : " + numCompte
                ));

        LocalDate aujourdHui = LocalDate.now();

        Interet interetActif = interetRepository.findInteretActif(numCompte)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Aucun intérêt actif pour ce compte. Veuillez d'abord appliquer un taux."
                ));

        // Éviter une double capitalisation le même jour
        if (interetActif.getDateCapitalisation() != null &&
                interetActif.getDateCapitalisation().isEqual(aujourdHui)) {
            return interetActif;
        }

        // Calculer le montant
        BigDecimal montantCalcule = interetActif.calculer(compte.getBalance());

        // ✅ FIX : si montant = 0 (ex: compte créé le jour même, prorata = 0 jour),
        // on clôture quand même la période et on en ouvre une nouvelle.
        // Sans ce fix, le service retournait silencieusement sans rien faire,
        // ce qui rendait le bouton "Capitaliser" sans effet visible.
        if (montantCalcule == null || montantCalcule.compareTo(BigDecimal.ZERO) <= 0) {
            montantCalcule = BigDecimal.ZERO;
        }

        // Capitalisation (clôture la période courante + crédite le solde si montant > 0)
        interetActif.capitaliser(compte);
        compteEpargneRepository.save(compte);
        interetRepository.save(interetActif);

        // Ouvrir une nouvelle période avec le même taux
        Interet nouveau = new Interet(compte, interetActif.getTauxInteret(), aujourdHui);
        interetRepository.save(nouveau);

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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Aucun intérêt actif pour ce compte"
                ));

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

            BigDecimal montant = interet.calculer(compte.getBalance());
            if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
                montant = BigDecimal.ZERO;
            }

            interet.capitaliser(compte);
            compteEpargneRepository.save(compte);
            resultats.add(interetRepository.save(interet));
        }

        return resultats;
    }
}