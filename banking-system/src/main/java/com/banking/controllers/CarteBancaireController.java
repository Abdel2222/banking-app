package com.banking.controllers;

import com.banking.dto.response.CarteBancaireResponse;
import com.banking.entities.CarteBancaire;
import com.banking.exceptions.ResourceNotFoundException;
import com.banking.services.CompteBancaireService;
import com.banking.services.EncryptionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/cartes", produces = "application/json")
public class CarteBancaireController {

    private final CompteBancaireService compteService;
    private final EncryptionService encryptionService;

    /** Récupérer la carte — numéro et CVV masqués */
    @GetMapping("/{numCompte}")
    public ResponseEntity<CarteBancaireResponse> getByCompte(@PathVariable String numCompte) {
        CarteBancaire carte = compteService.getCard(numCompte)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune carte pour " + numCompte));
        return ResponseEntity.ok(CarteBancaireResponse.fromEntity(carte));
    }

    /**
     * Récupérer le CVV déchiffré pour le client authentifié.
     * Utilisé uniquement dans card-details pour afficher le vrai CVV.
     */
    @GetMapping("/{numCompte}/mon-cvv")
    public ResponseEntity<Map<String, String>> getMonCvv(@PathVariable String numCompte) {
        CarteBancaire carte = compteService.getCard(numCompte)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune carte pour " + numCompte));
        String cvv = encryptionService.decrypt(carte.getCvv());
        return ResponseEntity.ok(Map.of("cvv", cvv));
    }

    /**
     * Émettre une nouvelle carte (ADMIN).
     * CVV retourné en clair UNE SEULE FOIS dans l'alerte admin.
     */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PostMapping("/{numCompte}/issue")
    public ResponseEntity<CarteBancaireResponse> issue(@PathVariable String numCompte) {
        CarteBancaire carte = compteService.issueCard(numCompte);
        return ResponseEntity.ok(CarteBancaireResponse.fromEntityWithCvv(carte));
    }

    /** Bloquer une carte (ADMIN) */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PostMapping("/{numCompte}/bloquer")
    public ResponseEntity<CarteBancaireResponse> bloquer(@PathVariable String numCompte,
                                                         @RequestParam(required = false) String raison) {
        CarteBancaire carte = compteService.blockCard(numCompte, raison);
        return ResponseEntity.ok(CarteBancaireResponse.fromEntity(carte));
    }

    /** Débloquer une carte (ADMIN) */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PostMapping("/{numCompte}/debloquer")
    public ResponseEntity<CarteBancaireResponse> debloquer(@PathVariable String numCompte) {
        CarteBancaire carte = compteService.unblockCard(numCompte);
        return ResponseEntity.ok(CarteBancaireResponse.fromEntity(carte));
    }

    /** Mettre à jour les plafonds (ADMIN) */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PatchMapping("/{numCompte}/plafonds")
    public ResponseEntity<CarteBancaireResponse> updatePlafonds(@PathVariable String numCompte,
                                                                @RequestBody PlafondsRequest req) {
        if (req.getPlafondJournalier() != null)
            compteService.setWithdrawalLimit(numCompte, req.getPlafondJournalier());
        if (req.getPlafondMensuel() != null)
            compteService.setTransferLimit(numCompte, req.getPlafondMensuel());
        CarteBancaire carte = compteService.getCard(numCompte)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune carte pour " + numCompte));
        return ResponseEntity.ok(CarteBancaireResponse.fromEntity(carte));
    }

    /**
     * Régénérer CVV après blocage (ADMIN).
     * Nouveau CVV chiffré AES en base, retourné en clair une seule fois.
     */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PostMapping("/{numCompte}/regenerate-cvv")
    public ResponseEntity<Map<String, String>> regenerateCvv(@PathVariable String numCompte) {
        CarteBancaire carte = compteService.getCard(numCompte)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune carte pour " + numCompte));

        String newCvv = String.format("%03d", new java.util.Random().nextInt(1000));
        carte.setCvv(encryptionService.encrypt(newCvv)); // ✅ AES
        compteService.saveCard(carte);

        return ResponseEntity.ok(Map.of("cvv", newCvv));
    }

    @Data
    public static class PlafondsRequest {
        private BigDecimal plafondJournalier;
        private BigDecimal plafondMensuel;
    }
}