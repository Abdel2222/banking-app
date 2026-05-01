package com.banking.services.impl;

import com.banking.dto.response.CompteEpargneResponse;
import com.banking.entities.CompteBancaire;
import com.banking.entities.CompteEpargne;
import com.banking.mappers.CompteEpargneMapper;
import com.banking.repositories.CompteBancaireRepository;
import com.banking.repositories.CompteEpargneRepository;
import com.banking.services.CompteEpargneService;
import com.banking.services.SavingsNumberService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CompteEpargneServiceImpl implements CompteEpargneService {

    private final CompteBancaireRepository compteRepo;
    private final CompteEpargneRepository epargneRepo;
    private final SavingsNumberService numberService;
    private final CompteEpargneMapper mapper;

    /* ====== Helpers ====== */

    private CompteEpargne getOrThrow(String numCompteBancaire) {
        return epargneRepo.findByCompteBancaire_NumCompte(numCompteBancaire)
                .orElseThrow(() -> new EntityNotFoundException("Épargne introuvable pour le compte " + numCompteBancaire));
    }

    private static BigDecimal computeTax(BigDecimal balance) {
        if (balance == null) return BigDecimal.ZERO;
        if (balance.compareTo(new BigDecimal("100")) > 0) return new BigDecimal("30.00");
        if (balance.compareTo(new BigDecimal("50"))  < 0) return new BigDecimal("10.00");
        return BigDecimal.ZERO;
    }

    /* ====== API ====== */

    @Override
    @Transactional
    public CompteEpargneResponse convertirDepuisCompte(String numCompteBancaire, BigDecimal tauxInteret) {
        CompteBancaire cb = compteRepo.findByNumCompte(numCompteBancaire)
                .orElseThrow(() -> new EntityNotFoundException("Compte bancaire introuvable: " + numCompteBancaire));

        if (epargneRepo.findByCompteBancaire_NumCompte(numCompteBancaire).isPresent())
            throw new IllegalStateException("Épargne déjà existante pour ce compte");

        CompteEpargne ce = CompteEpargne.builder()
                .compteBancaire(cb)
                .numCompteEpargne(numberService.fromBase(cb.getNumCompte())) // 3 chiffres + aléatoire
                .tauxInteret(tauxInteret != null ? tauxInteret : new BigDecimal("0.010000"))
                .soldeEpargne(BigDecimal.ZERO)
                .build();

        ce = epargneRepo.save(ce);
        return mapper.toResponse(ce);
    }

    // Pour coller à l'interface utilisée par le contrôleur: convertir(...)
    @Override
    @Transactional
    public CompteEpargneResponse convertir(String numCompte, BigDecimal tauxInteret) {
        return convertirDepuisCompte(numCompte, tauxInteret);
    }

    @Override
    @Transactional
    public CompteEpargneResponse alimenter(String numCompteBancaire, BigDecimal montant) {
        if (montant == null || montant.signum() <= 0) throw new IllegalArgumentException("Montant invalide");
        CompteEpargne ce = getOrThrow(numCompteBancaire);
        ce.alimenter(montant); // n’impacte pas le compte bancaire de base (comme demandé)
        epargneRepo.save(ce);
        return mapper.toResponse(ce);
    }

    @Override
    @Transactional
    public CompteEpargneResponse retirer(String numCompteBancaire, BigDecimal montant) {
        if (montant == null || montant.signum() <= 0) throw new IllegalArgumentException("Montant invalide");
        CompteEpargne ce = getOrThrow(numCompteBancaire);
        ce.retirer(montant);
        epargneRepo.save(ce);
        return mapper.toResponse(ce);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTaxationVirtuelle(String numCompteBancaire) {
        CompteEpargne ce = getOrThrow(numCompteBancaire);

        // 1) Si @Formula a rempli la valeur, on renvoie
        if (ce.getTaxationVirtuelle() != null) return ce.getTaxationVirtuelle();

        // 2) Sinon, calcule en Java à partir du solde du compte bancaire lié
        BigDecimal balance = (ce.getCompteBancaire() != null) ? ce.getCompteBancaire().getBalance() : null;
        return computeTax(balance);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CompteEpargneResponse capitaliser(String numCompteBancaire) {
        // Récupère (ou 404) le compte épargne lié au numéro de compte bancaire
        CompteEpargne ce = getOrThrow(numCompteBancaire);



        // Capitalise les intérêts et met à jour la date de capitalisation
        ce.capitaliserInterets();

        // Persiste les changements
        epargneRepo.save(ce);

        // Retourne le DTO
        return mapper.toResponse(ce);
    }


    @Override
    @Transactional(readOnly = true)
    public CompteEpargneResponse findByCompteId(Long compteId) {
        var ce = epargneRepo.findByCompteBancaire_Id(compteId)
                .orElseThrow(() -> new EntityNotFoundException("Épargne introuvable pour compteId=" + compteId));
        return mapper.toResponse(ce);
    }

    // Compatibilité si l'interface historique expose encore cette méthode (entité brute).
    // Si inutile chez toi, supprime-la de l'interface et d'ici.
    @Override
    @Transactional(readOnly = true)
    public CompteEpargne findByNumCompte(String numCompte) {
        return getOrThrow(numCompte);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculerInterets(String numCompte) {
        CompteEpargne ce = getOrThrow(numCompte);
        return ce.calculerInteretsAnnuels();
    }

    @Override
    @Transactional
    public CompteEpargneResponse capitaliserInterets(String numCompte) {
        CompteEpargne ce = getOrThrow(numCompte);
        if (ce.doitCapitaliser()) {
            ce.capitaliserInterets();
            epargneRepo.save(ce);
        }
        return mapper.toResponse(ce);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean doitCapitaliser(String numCompte) {
        return getOrThrow(numCompte).doitCapitaliser();
    }

    @Override
    @Transactional
    public CompteEpargneResponse updateTaux(String numCompte, BigDecimal tauxInteret) {
        if (tauxInteret == null || tauxInteret.signum() < 0) {
            throw new IllegalArgumentException("Taux invalide");
        }
        CompteEpargne ce = getOrThrow(numCompte);
        ce.setTauxInteret(tauxInteret);
        epargneRepo.save(ce);
        return mapper.toResponse(ce);
    }

    @Override
    @Transactional(readOnly = true)
    public CompteEpargneResponse getDetails(String numCompteBancaire) {
        return mapper.toResponse(getOrThrow(numCompteBancaire));
    }
}
