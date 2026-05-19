package com.banking.controllers;

import com.banking.entities.Interet;
import com.banking.services.InteretService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Interet> updateTaux(
            @PathVariable String numCompte,
            @RequestParam BigDecimal valeur) {
        return ResponseEntity.ok(service.updateTaux(numCompte, valeur));
    }
}