package com.banking.controllers;

import com.banking.dto.request.CreateAccountRequest;
import com.banking.dto.response.AccountResponse;
import com.banking.entities.CompteBancaire;
import com.banking.exceptions.UnauthorizedAccessException;
import com.banking.services.CompteBancaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comptes")
@RequiredArgsConstructor
public class CompteBancaireController {

    private final CompteBancaireService compteService;

    // 👇 IMPORTANT : on utilise Authentication (principal=email)
    // car ton service s'appuie sur authentication.getName() == email

    /** Récupérer MES comptes (client connecté) en DTO */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new UnauthorizedAccessException("Utilisateur non connecté");
        }
        List<CompteBancaire> comptes = compteService.getAccountsForPrincipal(authentication);
        List<AccountResponse> result = comptes.stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /** Créer un compte pour le client connecté (idempotent) */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(Authentication authentication,
                                                         @RequestBody(required = false) CreateAccountRequest request) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new UnauthorizedAccessException("Utilisateur non connecté");
        }
        CompteBancaire created = compteService.createForPrincipal(authentication, request);
        return ResponseEntity.ok(AccountResponse.fromEntity(created));
    }

    /** Activer un compte (ADMIN) par numéro */
    @PostMapping("/{numCompte}/activer")
    public ResponseEntity<AccountResponse> activerCompte(@PathVariable String numCompte) {
        CompteBancaire actif = compteService.activateAccount(numCompte);
        return ResponseEntity.ok(AccountResponse.fromEntity(actif));
    }

    /** Suspendre un compte (ADMIN) par numéro */
    @PostMapping("/{numCompte}/bloquer")
    public ResponseEntity<AccountResponse> bloquerCompte(@PathVariable String numCompte) {
        CompteBancaire bloque = compteService.suspendAccount(numCompte);
        return ResponseEntity.ok(AccountResponse.fromEntity(bloque));
    }
}