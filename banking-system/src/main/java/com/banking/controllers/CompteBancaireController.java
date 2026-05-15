package com.banking.controllers;

import com.banking.dto.request.CreateAccountRequest;
import com.banking.dto.response.AccountResponse;
import com.banking.entities.CompteBancaire;
import com.banking.entities.Operation;
import com.banking.exceptions.ResourceNotFoundException;
import com.banking.exceptions.UnauthorizedAccessException;
import com.banking.repositories.CompteBancaireRepository;
import com.banking.services.CompteBancaireService;
import com.banking.services.FraisDeGestionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comptes")
@RequiredArgsConstructor
public class CompteBancaireController {

    private static final Logger log = LoggerFactory.getLogger(CompteBancaireController.class);

    private final CompteBancaireService compteService;
    private final FraisDeGestionService fraisDeGestionService;
    private final CompteBancaireRepository compteRepo;

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

    @GetMapping("/by-id/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        CompteBancaire cb = compteRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Compte id introuvable: " + id));

        return ResponseEntity.ok(Map.of(
                "id", cb.getId(),
                "numCompte", cb.getNumCompte(),
                "balance", cb.getBalance(),
                "status", cb.getStatus()
        ));
    }

    @GetMapping("/by-num/{numCompte}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getByNum(@PathVariable String numCompte) {
        CompteBancaire cb = compteRepo.findByNumCompte(numCompte)
                .orElseThrow(() -> new EntityNotFoundException("Compte introuvable: " + numCompte));

        return ResponseEntity.ok(Map.of(
                "id", cb.getId(),
                "numCompte", cb.getNumCompte(),
                "balance", cb.getBalance(),
                "status", cb.getStatus(),
                "clientName", cb.getClient() != null ? cb.getClient().getNomComplet() : null,
                "createdAt", cb.getCreatedAt()
        ));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            Authentication authentication,
            @Valid @RequestBody(required = false) CreateAccountRequest request
    ) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new UnauthorizedAccessException("Utilisateur non connecté");
        }

        CompteBancaire created = compteService.createForPrincipal(authentication, request);

        try {
            fraisDeGestionService.appliquerFraisOuverture(created.getNumCompte());
        } catch (Exception e) {
            log.warn("Frais d'ouverture non appliqué sur {} : {}", created.getNumCompte(), e.getMessage());
        }

        return ResponseEntity.ok(AccountResponse.fromEntity(created));
    }

    @PostMapping("/{numCompte}/activer")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<AccountResponse> activerCompte(@PathVariable String numCompte) {
        CompteBancaire actif = compteService.activateAccount(numCompte);
        return ResponseEntity.ok(AccountResponse.fromEntity(actif));
    }

    @PostMapping("/{numCompte}/bloquer")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<AccountResponse> bloquerCompte(@PathVariable String numCompte) {
        CompteBancaire bloque = compteService.suspendAccount(numCompte);
        return ResponseEntity.ok(AccountResponse.fromEntity(bloque));
    }

    @PostMapping("/virement")
    public ResponseEntity<?> effectuerVirement(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            String src = pickString(
                    body,
                    "sourceAccount",
                    "fromAccount",
                    "from",
                    "numCompteSource",
                    "compteSource",
                    "source_account"
            );

            String dst = pickString(
                    body,
                    "destinationAccount",
                    "toAccount",
                    "to",
                    "numCompteDestination",
                    "compteDestination",
                    "destination_account"
            );

            BigDecimal amount = pickBigDecimal(body, "montant", "amount", "value", "somme");
            String description = pickString(body, "description", "libelle", "label");

            if (src == null || dst == null || amount == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Champs requis manquants",
                        "requis", Map.of(
                                "source", "sourceAccount|fromAccount|from|numCompteSource|compteSource",
                                "destination", "destinationAccount|toAccount|to|numCompteDestination|compteDestination",
                                "montant", "montant|amount|value"
                        )
                ));
            }

            log.info("Virement: {} de [{}] vers [{}] description='{}'", amount, src, dst, description);

            Operation op = compteService.transfer(src, dst, amount, description);

            return ResponseEntity.ok(op);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Erreur métier virement : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (ResourceNotFoundException e) {
            log.error("Ressource non trouvée : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur inattendue lors du virement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne du serveur"));
        }
    }

    @PostMapping("/{numCompte}/apply-opening-fee")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> applyOpeningFee(@PathVariable String numCompte) {
        try {
            fraisDeGestionService.appliquerFraisOuverture(numCompte);

            return ResponseEntity.ok(Map.of(
                    "numCompte", numCompte,
                    "applied", true,
                    "message", "Frais d'ouverture tenté seulement si balance >= 20."
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "numCompte", numCompte,
                    "applied", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<AccountResponse>> getComptesClient(@PathVariable Long clientId) {
        try {
            List<CompteBancaire> comptes = compteRepo.findByClientId(clientId);

            if (comptes.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<AccountResponse> response = comptes.stream()
                    .map(AccountResponse::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur récupération comptes client {}: {}", clientId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    private static String pickString(Map<String, Object> body, String... keys) {
        for (String k : keys) {
            Object v = body.get(k);
            if (v != null) {
                String s = v.toString().trim();
                if (!s.isEmpty()) return s;
            }
        }
        return null;
    }

    private static BigDecimal pickBigDecimal(Map<String, Object> body, String... keys) {
        for (String k : keys) {
            Object v = body.get(k);
            if (v == null) continue;

            if (v instanceof Number n) {
                return new BigDecimal(n.toString());
            }

            try {
                String s = v.toString().trim().replace(',', '.');
                if (!s.isEmpty()) {
                    return new BigDecimal(s);
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}