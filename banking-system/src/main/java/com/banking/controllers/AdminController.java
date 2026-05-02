package com.banking.controllers;

import com.banking.dto.response.*;
import com.banking.entities.CompteBancaire;
import com.banking.services.CompteBancaireService;
import com.banking.services.CarteBancaireService;
import com.banking.services.DemandeCarteBancaireService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminController {

    private final CompteBancaireService compteBancaireService;
    private final CarteBancaireService carteBancaireService;
    private final DemandeCarteBancaireService demandeCarteBancaireService;

    // ==================== COMPTES ====================

    // PATCH /api/admin/accounts/activate/{id}  (active par ID)
    @PatchMapping("/accounts/activate/{id}")
    public ResponseEntity<ApiResponse<?>> activateAccount(@PathVariable Long id) {
        CompteBancaire compte = compteBancaireService.findById(id)
                .orElseThrow(() -> new RuntimeException("Compte introuvable: id=" + id));
        // ton service active par numCompte
        CompteBancaire updated = compteBancaireService.activateAccount(compte.getNumCompte());
        return ResponseEntity.ok(ApiResponse.success("Compte activé", AccountResponse.fromEntity(updated)));
    }

    // ==================== CARTES ====================

    // Émettre une carte directement pour un compte
    @PostMapping("/accounts/{accountId}/cards")
    public ResponseEntity<ApiResponse<?>> issueForAccount(@PathVariable Long accountId) {
        var carte = carteBancaireService.emettrePourCompte(accountId, true, null, null);
        return ResponseEntity.ok(ApiResponse.success("Carte émise", CarteBancaireResponse.fromEntity(carte)));
    }

    @PostMapping("/cards/issue")
    public ResponseEntity<ApiResponse<?>> issueGeneric(@RequestBody IssueCardRequest req) {
        boolean active = (req.estActive == null) ? true : req.estActive;
        var carte = carteBancaireService.emettrePourCompte(
                req.compteId, active, req.plafondJournalier, req.plafondMensuel
        );
        return ResponseEntity.ok(ApiResponse.success("Carte émise", CarteBancaireResponse.fromEntity(carte)));
    }

    // ✅ Utilise la signature existante du service : activer(Long carteId) -> entity
    @PatchMapping("/cards/activate/{cardId}")
    public ResponseEntity<ApiResponse<?>> activateCard(@PathVariable Long cardId) {
        var entity = carteBancaireService.activer(cardId);
        return ResponseEntity.ok(ApiResponse.success("Carte activée", CarteBancaireResponse.fromEntity(entity)));
    }

    // ==================== DEMANDES DE CARTE ====================

    // Approve
    @PatchMapping("/card-requests/{id}/approve")
    public ResponseEntity<ApiResponse<?>> approveRequest(@PathVariable Long id) {
        var demande = demandeCarteBancaireService.approuverDemande(id);
        return ResponseEntity.ok(ApiResponse.success("Demande approuvée", DemandeCarteResponse.fromEntity(demande)));
    }

    // Reject
    @PatchMapping("/card-requests/{id}/reject")
    public ResponseEntity<ApiResponse<?>> rejectRequest(
            @PathVariable Long id,
            @RequestBody RejectCardRequest body
    ) {
        var demande = demandeCarteBancaireService.rejeterDemande(id, body.reason);
        return ResponseEntity.ok(ApiResponse.success("Demande rejetée", DemandeCarteResponse.fromEntity(demande)));
    }

    // ============ DTOs internes ============

    public static class IssueCardRequest {
        @NotNull public Long compteId;
        public Boolean estActive;
        public Double plafondJournalier;
        public Double plafondMensuel;
    }

    public static class RejectCardRequest {
        @NotNull public String reason;
    }
}
