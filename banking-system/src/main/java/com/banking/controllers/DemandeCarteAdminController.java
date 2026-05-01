package com.banking.controllers;

import com.banking.dto.request.DemandeCarteRejectRequest;
import com.banking.dto.response.ApiResponse;
import com.banking.dto.response.DemandeCarteResponse;
import com.banking.entities.CarteBancaire;
import com.banking.entities.DemandeCarteBancaire;
import com.banking.entity.enums.CardRequestStatus;
import com.banking.repositories.CarteBancaireRepository;
import com.banking.repositories.CompteBancaireRepository;
import com.banking.repositories.DemandeCarteBancaireRepository;
import com.banking.services.DemandeCarteBancaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/cartes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class DemandeCarteAdminController {

    // === Dépendances existantes ===
    private final DemandeCarteBancaireService demandeService;

    // === Ajouts pour quick-approve / lecture résumé ===
    private final CompteBancaireRepository compteRepo;
    private final CarteBancaireRepository carteRepo;
    private final DemandeCarteBancaireRepository demandeRepo;

    // ======================================
    //         Endpoints existants
    // ======================================

    /**
     * PATCH /api/admin/cartes/{id}/approve
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<?>> approve(@PathVariable Long id) {
        DemandeCarteBancaire entity = demandeService.approuverDemande(id);
        DemandeCarteResponse dto = DemandeCarteResponse.fromEntity(entity);
        return ResponseEntity.ok(ApiResponse.success("Demande approuvée", dto));
    }

    /**
     * PATCH /api/admin/cartes/{id}/reject
     * body: { "reason": "motif..." }
     */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<?>> reject(
            @PathVariable Long id,
            @RequestBody DemandeCarteRejectRequest body
    ) {
        DemandeCarteBancaire entity = demandeService.rejeterDemande(id, body.getReason());
        DemandeCarteResponse dto = DemandeCarteResponse.fromEntity(entity);
        return ResponseEntity.ok(ApiResponse.success("Demande rejetée", dto));
    }

    // ======================================
    //          Nouveaux endpoints
    // ======================================

    /**
     * POST /api/admin/cartes/quick-approve/{numCompte}?pan=...&cardId=...
     *
     * - Trouve une carte (par PAN ou cardId, sinon déjà liée / dernière orpheline)
     * - Attache + active la carte sur le compte
     * - Crée ou met à jour la DemandeCarteBancaire en APPROVED
     * - Retourne un petit résumé
     */
    @PostMapping("/quick-approve/{numCompte}")
    @Transactional
    public ResponseEntity<?> quickApprove(
            @PathVariable String numCompte,
            @RequestParam(required = false) Long cardId,
            @RequestParam(required = false) String pan
    ) {
        try {
            var compte = compteRepo.findByNumCompte(numCompte)
                    .orElseThrow(() -> new IllegalArgumentException("Compte introuvable: " + numCompte));

            // 1) Trouver la carte (adapté à Optional<Object> de findByCompteBancaire_NumCompte)
            CarteBancaire carte = null;
            if (cardId != null) {
                carte = carteRepo.findById(cardId)
                        .orElseThrow(() -> new IllegalArgumentException("Carte introuvable: id=" + cardId));
            } else if (pan != null && !pan.isBlank()) {
                carte = carteRepo.findByNumeroCarte(pan)
                        .orElseThrow(() -> new IllegalArgumentException("Carte introuvable: PAN=" + pan));
            } else {
                Object maybe = carteRepo.findByCompteBancaire_NumCompte(numCompte).orElse(null);
                if (maybe instanceof CarteBancaire) {
                    carte = (CarteBancaire) maybe;
                }
                if (carte == null) {
                    carte = carteRepo.findTopByCompteBancaireIsNullOrderByIdDesc()
                            .orElseThrow(() -> new IllegalStateException("Aucune carte disponible : passez ?pan=... ou ?cardId=..."));
                }
            }

            // 2) Attacher + activer
            carte.setCompteBancaire(compte);
            compte.setCarteBancaire(carte);
            carte.setEstActive(true);
            carteRepo.save(carte);

            // 3) UPSERT demande → APPROVED (sans setRequestedAt, l'entité/DB gère la date)
            var demOpt = demandeRepo.findTopByCompte_NumCompteOrderByRequestedAtDesc(numCompte);
            DemandeCarteBancaire demande = demOpt.orElse(null);
            if (demande == null) {
                demande = new DemandeCarteBancaire();
                demande.setClient(compte.getClient());
                demande.setCompte(compte);
                demande.setStatus(CardRequestStatus.APPROVED);
                // pas d'appel à setRequestedAt(...) -> champ auto (PrePersist/DB default) si présent
                demandeRepo.save(demande);
            } else if (demande.getStatus() != CardRequestStatus.REJECTED) {
                demande.setStatus(CardRequestStatus.APPROVED);
                demandeRepo.save(demande);
            }

            return ResponseEntity.ok(Map.of(
                    "numCompte", numCompte,
                    "carteId", carte.getId(),
                    "numeroCarte", carte.getNumeroCarte(),
                    "carteActive", true,
                    "demandeId", demande != null ? demande.getId() : null,
                    "demandeStatus", demande != null ? demande.getStatus().name() : null,
                    "done", true
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "numCompte", numCompte));
        }
    }

    /**
     * GET /api/admin/cartes/demandes/{numCompte}/resume
     * → Dernière demande + identité + n° carte + statut (+ requestedAt si dispo)
     */
    @GetMapping("/demandes/{numCompte}/resume")
    public ResponseEntity<?> lastResume(@PathVariable String numCompte) {
        var opt = demandeRepo.findTopByCompte_NumCompteOrderByRequestedAtDesc(numCompte);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "numCompte", numCompte,
                    "error", "Aucune demande trouvée pour ce compte"
            ));
        }
        var d = opt.get();
        var c = d.getCompte();
        var cli = d.getClient();
        var carte = (c != null ? c.getCarteBancaire() : null);

        Object requestedAt = null;
        try {
            requestedAt = d.getClass().getMethod("getRequestedAt").invoke(d);
        } catch (Exception ignore) {
            // si l'entité n'a pas de getter, on laisse null
        }

        return ResponseEntity.ok(Map.of(
                "numCompte", numCompte,
                "demandeId", d.getId(),
                "status", d.getStatus().name(),
                "approved", d.getStatus() == CardRequestStatus.APPROVED,
                "numeroCarte", (carte != null ? carte.getNumeroCarte() : null),
                "prenom", (cli != null ? cli.getPrenom() : null),
                "nom", (cli != null ? cli.getNom() : null),
                "requestedAt", requestedAt
        ));
    }
}
