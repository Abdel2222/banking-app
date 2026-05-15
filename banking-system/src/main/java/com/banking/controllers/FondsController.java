package com.banking.controllers;

import com.banking.dto.request.CreateFondsRequest;
import com.banking.dto.response.FondsResponse;
import com.banking.entity.enums.NiveauRisque;
import com.banking.services.FondsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fonds")
@RequiredArgsConstructor
public class FondsController {

    private final FondsService fondsService;

    @PostMapping
    public ResponseEntity<FondsResponse> creerFonds(@Valid @RequestBody CreateFondsRequest request) {
        FondsResponse response = fondsService.creerFonds(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FondsResponse>> getTousLesFonds() {
        return ResponseEntity.ok(fondsService.getTousLesFonds());
    }

    @GetMapping("/actifs")
    public ResponseEntity<List<FondsResponse>> getFondsActifs() {
        return ResponseEntity.ok(fondsService.getFondsActifs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FondsResponse> getFondsById(@PathVariable Long id) {
        return ResponseEntity.ok(fondsService.getFondsById(id));
    }

    @GetMapping("/risque/{niveauRisque}")
    public ResponseEntity<List<FondsResponse>> getFondsParRisque(@PathVariable NiveauRisque niveauRisque) {
        return ResponseEntity.ok(fondsService.getFondsParRisque(niveauRisque));
    }

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<FondsResponse> desactiverFonds(@PathVariable Long id) {
        return ResponseEntity.ok(fondsService.desactiverFonds(id));
    }

    @PatchMapping("/{id}/activer")
    public ResponseEntity<FondsResponse> activerFonds(@PathVariable Long id) {
        return ResponseEntity.ok(fondsService.activerFonds(id));
    }
}