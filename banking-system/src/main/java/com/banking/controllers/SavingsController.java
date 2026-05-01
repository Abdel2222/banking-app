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

    @PostMapping("/{numCompte}/convertir")
    public ResponseEntity<CompteEpargneResponse> convertir(
            @PathVariable String numCompte,
            @RequestBody(required = false) EpargneConvertRequest body) {
        BigDecimal taux = body != null ? body.getTauxInteret() : null;
        return ResponseEntity.ok(service.convertirDepuisCompte(numCompte, taux));
    }

    @PostMapping("/{numCompte}/alimenter")
    public ResponseEntity<CompteEpargneResponse> alimenter(
            @PathVariable String numCompte,
            @RequestBody EpargneMontantRequest body) {
        return ResponseEntity.ok(service.alimenter(numCompte, body.getMontant()));
    }

    @PostMapping("/{numCompte}/retirer")
    public ResponseEntity<CompteEpargneResponse> retirer(
            @PathVariable String numCompte,
            @RequestBody EpargneMontantRequest body) {
        return ResponseEntity.ok(service.retirer(numCompte, body.getMontant()));
    }

    @GetMapping("/{numCompte}/taxation")
    public Map<String, Object> taxation(@PathVariable String numCompte) {
        return Map.of("numCompte", numCompte, "taxationVirtuelle", service.getTaxationVirtuelle(numCompte));
    }

    @PostMapping("/{numCompte}/capitaliser")
    public ResponseEntity<CompteEpargneResponse> capitaliser(@PathVariable String numCompte) {
        return ResponseEntity.ok((CompteEpargneResponse) service.capitaliser(numCompte));
    }

    @GetMapping("/{numCompte}")
    public ResponseEntity<CompteEpargneResponse> details(@PathVariable String numCompte) {
        return ResponseEntity.ok(service.getDetails(numCompte));
    }
}

