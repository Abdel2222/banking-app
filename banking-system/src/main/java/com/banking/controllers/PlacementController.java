package com.banking.controllers;

import com.banking.dto.request.CreatePlacementRequest;
import com.banking.dto.response.PlacementResponse;
import com.banking.services.PlacementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/placements")
@RequiredArgsConstructor
public class PlacementController {

    private final PlacementService placementService;

    /* ==================== Création ==================== */

    @PostMapping
    public ResponseEntity<PlacementResponse> creerPlacement(
            @Valid @RequestBody CreatePlacementRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(placementService.creerPlacement(request));
    }

    /* ==================== Lecture ==================== */

    @GetMapping
    public ResponseEntity<List<PlacementResponse>> getTousLesPlacements() {
        return ResponseEntity.ok(placementService.getTousLesPlacements());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlacementResponse> getPlacementById(@PathVariable Long id) {
        return ResponseEntity.ok(placementService.getPlacementById(id));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<PlacementResponse>> getPlacementsByClient(
            @PathVariable Long clientId
    ) {
        return ResponseEntity.ok(placementService.getPlacementsByClient(clientId));
    }

    @GetMapping("/client/{clientId}/actifs")
    public ResponseEntity<List<PlacementResponse>> getPlacementsActifsByClient(
            @PathVariable Long clientId
    ) {
        return ResponseEntity.ok(placementService.getPlacementsActifsByClient(clientId));
    }

    /* ==================== Actions ==================== */

    @PatchMapping("/{placementId}/cloturer")
    public ResponseEntity<PlacementResponse> cloturerPlacement(
            @PathVariable Long placementId
    ) {
        return ResponseEntity.ok(placementService.cloturerPlacement(placementId));
    }

    /**
     * Sortie anticipée — frais dégressifs calculés automatiquement par le backend.
     * Règle : max(capital × 0.25%, capital × 2% × ratioRestant)
     */
    @PostMapping("/{id}/sortir")
    public ResponseEntity<PlacementResponse> sortirAvantEcheance(@PathVariable Long id) {
        return ResponseEntity.ok(placementService.sortirAvantEcheance(id));
    }

    @PatchMapping("/{placementId}/annuler")
    public ResponseEntity<PlacementResponse> annulerPlacement(
            @PathVariable Long placementId
    ) {
        return ResponseEntity.ok(placementService.annulerPlacement(placementId));
    }
}