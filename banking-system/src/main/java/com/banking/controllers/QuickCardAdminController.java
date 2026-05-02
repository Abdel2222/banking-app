package com.banking.controllers;

import com.banking.entities.CarteBancaire;
import com.banking.entities.DemandeCarteBancaire;
import com.banking.entity.enums.CardRequestStatus;
import com.banking.repositories.CarteBancaireRepository;
import com.banking.repositories.CompteBancaireRepository;
import com.banking.repositories.DemandeCarteBancaireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/quick-card")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class QuickCardAdminController {

    private final CompteBancaireRepository compteRepo;
    private final CarteBancaireRepository carteRepo;
    private final DemandeCarteBancaireRepository demandeRepo;

    @PostMapping("/{numCompte}/activate")
    @Transactional
    public Map<String, Object> quickActivate(
            @PathVariable String numCompte,
            @RequestParam(required = false) Long cardId,
            @RequestParam(required = false) String pan
    ) {
        var compte = compteRepo.findByNumCompte(numCompte)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable: " + numCompte));
        var client = compte.getClient();

        // ---- PRÉ-CHECKS : refus si déjà carte OU demande existante (PENDING/APPROVED) ----
        boolean compteDejaCarte = (compte.getCarteBancaire() != null)
                || carteRepo.existsByCompteBancaire_Id(compte.getId());
        boolean clientADejaDemande = demandeRepo.existsByClient_IdAndStatusIn(
                client.getId(),
                List.of(CardRequestStatus.PENDING, CardRequestStatus.APPROVED)
        );
        if (compteDejaCarte || clientADejaDemande) {
            var lastDemandeOpt = demandeRepo.findTopByCompte_NumCompteOrderByRequestedAtDesc(numCompte);
            String reason = compteDejaCarte
                    ? "Demande refusée : carte déjà rattachée au compte."
                    : "Demande refusée : une demande existe déjà pour ce client.";

            Long demandeId = null;
            CardRequestStatus demandeStatus = CardRequestStatus.REJECTED; // on sait ce qu’on renvoie

            if (lastDemandeOpt.isPresent()) {
                var lastDemande = lastDemandeOpt.get();
                if (lastDemande.getStatus() != CardRequestStatus.REJECTED) {
                    lastDemande.setStatus(CardRequestStatus.REJECTED);
                    // pas de setReviewedAt(...) dans ton entité → on n’appelle pas
                    lastDemande.setRejectedReason(reason);
                    demandeRepo.save(lastDemande);
                }
                demandeId = lastDemande.getId();
                // on ne lit pas lastDemande.getStatus(); on renvoie la valeur qu’on vient de poser
            }

            assert demandeId != null;
            return Map.of(
                    "numCompte", numCompte,
                    "refused", true,
                    "reason", reason,
                    "demandeId", demandeId,
                    "demandeStatus", demandeStatus, // REJECTED
                    "done", false
            );
        }

        // ---- Trouver la carte à rattacher/activer ----
        CarteBancaire carte;
        if (cardId != null) {
            carte = carteRepo.findById(cardId)
                    .orElseThrow(() -> new IllegalArgumentException("Carte introuvable: id=" + cardId));
        } else if (pan != null && !pan.isBlank()) {
            carte = carteRepo.findByNumeroCarte(pan)
                    .orElseThrow(() -> new IllegalArgumentException("Carte introuvable: PAN=" + pan));
        } else {
            carte = (CarteBancaire) carteRepo.findByCompteBancaire_NumCompte(numCompte).orElse(null);
            if (carte == null) {
                return Map.of(
                        "numCompte", numCompte,
                        "refused", true,
                        "reason", "Aucune carte disponible à attacher (passez cardId=... ou pan=...)",
                        "done", false
                );
            }
        }

        // ---- Rattacher + activer ----
        carte.setCompteBancaire(compte);
        compte.setCarteBancaire(carte);
        carte.setEstActive(true);
        carteRepo.save(carte);
        boolean carteActive = true; // on vient de l’activer

        // ---- Mettre à jour la dernière demande (si existe) ----
        DemandeCarteBancaire demande = demandeRepo
                .findTopByCompte_NumCompteOrderByRequestedAtDesc(numCompte)
                .orElse(null);

        Long demandeId = null;
        CardRequestStatus demandeStatus = null;

        if (demande != null && demande.getStatus() != CardRequestStatus.REJECTED) {
            demande.setStatus(CardRequestStatus.APPROVED); // pas d’état ACTIVATED dans ton enum
            // pas de setActivatedAt(...) ni setCarte(...), car absents dans ton entité
            demandeRepo.save(demande);

            demandeId = demande.getId();
            demandeStatus = CardRequestStatus.APPROVED; // on renvoie la valeur posée
        }

        // ---- Réponse ----
        assert demandeId != null;
        return Map.of(
                "numCompte", numCompte,
                "carteId", carte.getId(),
                "numeroCarte", carte.getNumeroCarte(),
                "carteActive", carteActive,            // ← pas d’appel à getEstActive()
                "demandeId", demandeId,
                "demandeStatus", demandeStatus,        // ← pas d’appel à getStatus()
                "refused", false,
                "done", true
        );
    }
}

