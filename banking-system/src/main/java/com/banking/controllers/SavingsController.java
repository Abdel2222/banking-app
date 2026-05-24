package com.banking.controllers;

import com.banking.dto.request.EpargneConvertRequest;
import com.banking.dto.request.EpargneMontantRequest;
import com.banking.dto.response.CompteEpargneResponse;
import com.banking.services.CompteEpargneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/savings")
@RequiredArgsConstructor
public class SavingsController {

    private final CompteEpargneService service;

    // ✅ Créer/convertir un compte épargne
    @PostMapping("/{numCompte}/convertir")
    public ResponseEntity<CompteEpargneResponse> convertir(
            @PathVariable String numCompte,
            @RequestBody(required = false) EpargneConvertRequest body) {
        BigDecimal premierMontant = body != null ? body.getPremierMontant() : null;
        return ResponseEntity.ok(service.convertirDepuisCompte(numCompte, premierMontant));
    }

    @PostMapping("/{numCompte}/alimenter")
    public ResponseEntity<CompteEpargneResponse> alimenter(
            @PathVariable String numCompte,
            @RequestBody EpargneMontantRequest body) {

        // Virement interne : débiter le source + créditer l'épargne
        if (body.getNumCompteSource() != null && !body.getNumCompteSource().isBlank()) {
            return ResponseEntity.ok(
                    service.alimenterDepuis(numCompte, body.getMontant(), body.getNumCompteSource())
            );
        }
        // Crédit direct sans débit source
        return ResponseEntity.ok(service.alimenter(numCompte, body.getMontant()));
    }

    // ✅ Retirer
    @PostMapping("/{numCompte}/retirer")
    public ResponseEntity<CompteEpargneResponse> retirer(
            @PathVariable String numCompte,
            @RequestBody EpargneMontantRequest body) {
        return ResponseEntity.ok(service.retirer(numCompte, body.getMontant()));
    }

    // ✅ Détails
    @GetMapping("/{numCompte}")
    public ResponseEntity<CompteEpargneResponse> details(@PathVariable String numCompte) {
        return ResponseEntity.ok(service.getDetails(numCompte));
    }

    // ❌ SUPPRIMÉ — taxation virtuelle n'existe plus
    @GetMapping("/{numCompte}/taxation")
    public ResponseEntity<Map<String, String>> taxation(@PathVariable String numCompte) {
        return ResponseEntity.status(410).body(Map.of(
                "message", "La taxation virtuelle a été supprimée",
                "alternative", "Les intérêts sont gérés via /api/interets"
        ));
    }

    // ❌ SUPPRIMÉ — capitalisation déplacée dans InteretService
    @PostMapping("/{numCompte}/capitaliser")
    public ResponseEntity<Map<String, String>> capitaliser(@PathVariable String numCompte) {
        return ResponseEntity.status(410).body(Map.of(
                "message", "La capitalisation est gérée via /api/interets/" + numCompte + "/capitaliser"
        ));
    }
}