

package com.banking.controllers;

import com.banking.dto.request.DemandeCarteRequest;
import com.banking.dto.response.ApiResponse;
import com.banking.dto.response.DemandeCarteResponse;
import com.banking.entities.DemandeCarteBancaire;
import com.banking.services.DemandeCarteBancaireService;
import com.banking.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cartes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class DemandeCarteClientController {

    private final DemandeCarteBancaireService demandeService;

    /**
     * POST /api/cartes/demande
     * body: { "compteId": <id> }
     */
    @PostMapping("/demande")
    public ResponseEntity<ApiResponse<?>> demanderCarte(@RequestBody DemandeCarteRequest req) {
        // (optionnel) on vérifie que le compte appartient bien au client connecté
        Long clientId = SecurityUtils.currentClientId(); // => on ajoute la méthode plus bas
        // Si ton service gère déjà l'appartenance, tu peux ignorer clientId ici.

        DemandeCarteBancaire entity = demandeService.demanderCarte(req.getCompteId());
        DemandeCarteResponse dto = DemandeCarteResponse.fromEntity(entity);
        return ResponseEntity.ok(ApiResponse.success("Demande de carte créée", dto));
    }
}


