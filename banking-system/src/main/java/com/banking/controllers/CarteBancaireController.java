package com.banking.controllers;

import com.banking.dto.response.CarteBancaireResponse;
import com.banking.entities.CarteBancaire;
import com.banking.exceptions.ResourceNotFoundException;
import com.banking.services.CompteBancaireService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/cartes", produces = "application/json")
public class CarteBancaireController {

    private final CompteBancaireService compteService;

    /** Récupérer la carte liée à un compte (client connecté ou admin) */
    @GetMapping("/{numCompte}")
    public ResponseEntity<CarteBancaireResponse> getByCompte(@PathVariable String numCompte) {
        CarteBancaire carte = compteService.getCard(numCompte)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune carte associée au compte " + numCompte));
        return ResponseEntity.ok(CarteBancaireResponse.fromEntity(carte));
    }

    /** Émettre une nouvelle carte pour un compte (ADMIN) */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{numCompte}/issue")
    public ResponseEntity<CarteBancaireResponse> issue(@PathVariable String numCompte) {
        CarteBancaire carte = compteService.issueCard(numCompte);
        return ResponseEntity.ok(CarteBancaireResponse.fromEntity(carte));
    }

    /** Bloquer une carte (ADMIN) */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{numCompte}/bloquer")
    public ResponseEntity<CarteBancaireResponse> bloquer(@PathVariable String numCompte,
                                                         @RequestParam(required = false) String raison) {
        CarteBancaire carte = compteService.blockCard(numCompte, raison);
        return ResponseEntity.ok(CarteBancaireResponse.fromEntity(carte));
    }

    /** Débloquer une carte (ADMIN) */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{numCompte}/debloquer")
    public ResponseEntity<CarteBancaireResponse> debloquer(@PathVariable String numCompte) {
        CarteBancaire carte = compteService.unblockCard(numCompte);
        return ResponseEntity.ok(CarteBancaireResponse.fromEntity(carte));
    }

    /** Mettre à jour les plafonds (ADMIN) */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{numCompte}/plafonds")
    public ResponseEntity<CarteBancaireResponse> updatePlafonds(@PathVariable String numCompte,
                                                                @RequestBody PlafondsRequest req) {
        if (req.getPlafondJournalier() != null) {
            compteService.setWithdrawalLimit(numCompte, req.getPlafondJournalier());
        }
        if (req.getPlafondMensuel() != null) {
            compteService.setTransferLimit(numCompte, req.getPlafondMensuel());
        }
        CarteBancaire carte = compteService.getCard(numCompte)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune carte associée au compte " + numCompte));
        return ResponseEntity.ok(CarteBancaireResponse.fromEntity(carte));
    }

    @Data
    public static class PlafondsRequest {
        private BigDecimal plafondJournalier; // en devise du compte
        private BigDecimal plafondMensuel;    // en devise du compte
    }
}
