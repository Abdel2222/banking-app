package com.banking.controllers;

import com.banking.dto.request.DemandeCarteRejectRequest;
import com.banking.dto.response.ApiResponse;
import com.banking.dto.response.DemandeCarteResponse;
import com.banking.entities.DemandeCarteBancaire;
import com.banking.services.DemandeCarteBancaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/cartes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class DemandeCarteAdminController {

    private final DemandeCarteBancaireService demandeService;

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
}
