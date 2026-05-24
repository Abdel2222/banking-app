package com.banking.controllers;

import com.banking.entities.Interet;
import com.banking.services.InteretService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interets")
@RequiredArgsConstructor
public class InteretController {

    private final InteretService service;

    // POST /api/interets/{numCompte}?taux=0.03
    @PostMapping("/{numCompte}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Interet> creer(
            @PathVariable String numCompte,
            @RequestParam BigDecimal taux) {
        return ResponseEntity.ok(service.creer(numCompte, taux));
    }

    // GET /api/interets/{numCompte}/calcul
    @GetMapping("/{numCompte}/calcul")
    public ResponseEntity<Map<String, Object>> calculer(@PathVariable String numCompte) {
        BigDecimal montant = service.calculer(numCompte);
        return ResponseEntity.ok(Map.of(
                "numCompte", numCompte,
                "montantInterets", montant
        ));
    }

    // POST /api/interets/{numCompte}/capitaliser
    @PostMapping("/{numCompte}/capitaliser")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Interet> capitaliser(@PathVariable String numCompte) {
        return ResponseEntity.ok(service.capitaliser(numCompte));
    }

    // GET /api/interets/{numCompte}/historique
    @GetMapping("/{numCompte}/historique")
    public ResponseEntity<List<Interet>> historique(@PathVariable String numCompte) {
        return ResponseEntity.ok(service.historique(numCompte));
    }

    // PUT /api/interets/{numCompte}/taux?valeur=0.05
    @PutMapping("/{numCompte}/taux")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Interet> updateTaux(
            @PathVariable String numCompte,
            @RequestParam BigDecimal valeur) {
        return ResponseEntity.ok(service.updateTaux(numCompte, valeur));
    }

    // POST /api/interets/appliquer-tous?taux=0.03
    @PostMapping("/appliquer-tous")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> appliquerTauxATous(@RequestParam BigDecimal taux) {
        List<Interet> resultats = service.appliquerTauxATous(taux);
        return ResponseEntity.ok(Map.of(
                "message", "Taux appliqué à " + resultats.size() + " comptes",
                "taux", taux,
                "comptesConcernes", resultats.size()
        ));
    }

    // POST /api/interets/capitaliser-tous
    @PostMapping("/capitaliser-tous")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> capitaliserTous() {
        List<Interet> resultats = service.capitaliserTous();
        return ResponseEntity.ok(Map.of(
                "message", "Capitalisation effectuée pour " + resultats.size() + " comptes",
                "comptesConcernes", resultats.size()
        ));
    }
}