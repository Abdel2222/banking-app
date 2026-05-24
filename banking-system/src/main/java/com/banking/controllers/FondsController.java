package com.banking.controllers;

import com.banking.dto.request.CreateFondsRequest;
import com.banking.dto.response.FondsResponse;
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

    @GetMapping("/{id}")
    public ResponseEntity<FondsResponse> getFondsById(@PathVariable Long id) {
        return ResponseEntity.ok(fondsService.getFondsById(id));
    }
}