package com.banking.controllers;

import com.banking.dto.request.DepositRequest;
import com.banking.dto.request.TransferRequest;
import com.banking.dto.request.WithdrawRequest;
import com.banking.dto.response.OperationResponse;
import com.banking.entities.CompteBancaire;
import com.banking.entities.Operation;
import com.banking.repositories.CompteBancaireRepository;
import com.banking.repositories.OperationRepository;
import com.banking.services.OperationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = {"/api/operations", "/api/transactions"}, produces = "application/json")
@CrossOrigin(origins = "*", maxAge = 3600)
@Validated
public class OperationController {

    private static final Logger logger = LoggerFactory.getLogger(OperationController.class);

    private final OperationService operationService;
    private final OperationRepository operationRepository;
    private final CompteBancaireRepository compteRepository;

    @GetMapping("/recent")
    public ResponseEntity<?> recent(
            @RequestParam String numCompte,
            @RequestParam(defaultValue = "20") int limit) {

        try {
            logger.info("[OPS] Recherche opérations pour numCompte={} limit={}", numCompte, limit);

            CompteBancaire compte = compteRepository.findByNumCompte(numCompte).orElse(null);
            if (compte == null) {
                logger.warn("[OPS] Compte introuvable: {}", numCompte);
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<Operation> allOps = operationRepository.findAll();
            List<Operation> filtered = new ArrayList<>();
            for (Operation op : allOps) {
                try {
                    if (op.getCompteBancaire() != null
                            && op.getCompteBancaire().getId() != null
                            && op.getCompteBancaire().getId().equals(compte.getId())) {
                        filtered.add(op);
                    }
                } catch (Exception ignore) {}
            }

            filtered.sort(Comparator.comparing(
                    (Operation o) -> {
                        Object d = getOpDate(o);
                        return d == null ? "" : d.toString();
                    },
                    Comparator.reverseOrder()
            ));

            if (filtered.size() > limit) {
                filtered = filtered.subList(0, limit);
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Operation op : filtered) {
                Map<String, Object> dto = new LinkedHashMap<>();
                dto.put("id", op.getId());
                dto.put("montant", op.getMontant());
                dto.put("description", safeGet(() -> op.getDescription()));
                dto.put("commentaire", safeGet(() -> getCommentaire(op)));
                dto.put("date", getOpDate(op));
                dto.put("dateOperation", getOpDate(op));
                dto.put("type", getType(op));
                dto.put("statut", "COMPLETED");

                // ✅ Champs virement — contrepartie et communication
                dto.put("numeroCompte", safeGet(() -> op.getCompteBancaire().getNumCompte()));
                dto.put("numeroCompteDestinataire", safeGet(() -> op.getNumeroCompteDestinataire()));
                dto.put("communication", safeGet(() -> op.getCommunication()));
                dto.put("nomTitulaireDestinataire", safeGet(() -> op.getNomTitulaireDestinataire()));

                result.add(dto);
            }

            logger.info("[OPS] Retourne {} opérations pour {}", result.size(), numCompte);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("[OPS] Erreur recent()", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "(no msg)" : e.getMessage())
            ));
        }
    }

    @GetMapping("/compte/{numCompte}")
    public ResponseEntity<?> byCompte(
            @PathVariable String numCompte,
            @RequestParam(defaultValue = "100") int limit) {
        return recent(numCompte, limit);
    }

    @PostMapping(value = {"/deposit", "/depot"}, consumes = "application/json")
    public ResponseEntity<OperationResponse> deposit(@Valid @RequestBody DepositRequest req) {
        Operation op = operationService.executeDeposit(req.getNumCompte(), req.getMontant(), req.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(OperationResponse.fromEntity(op));
    }

    @PostMapping(value = {"/withdraw", "/retrait"}, consumes = "application/json")
    public ResponseEntity<OperationResponse> withdraw(@Valid @RequestBody WithdrawRequest req) {
        Operation op = operationService.executeWithdrawal(req.getNumCompte(), req.getMontant(), req.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(OperationResponse.fromEntity(op));
    }

    @PostMapping(value = {"/transfer", "/virement"}, consumes = "application/json")
    public ResponseEntity<OperationResponse> transfer(@Valid @RequestBody TransferRequest req) {
        Operation op = operationService.executeTransfer(
                req.getNumCompteSource(),
                req.getNumCompteDestinataire(),
                req.getMontant(),
                req.getCommunication()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(OperationResponse.fromEntity(op));
    }

    // ============================================================
    // Helpers
    // ============================================================

    private Object getOpDate(Operation op) {
        try {
            Object d = op.getClass().getMethod("getDateOperation").invoke(op);
            if (d != null) return d;
        } catch (Throwable ignored) {}
        try {
            Object d = op.getClass().getMethod("getDate").invoke(op);
            if (d != null) return d;
        } catch (Throwable ignored) {}
        try {
            Object d = op.getClass().getMethod("getCreatedAt").invoke(op);
            if (d != null) return d;
        } catch (Throwable ignored) {}
        return null;
    }

    private Object getCommentaire(Operation op) {
        try {
            return op.getClass().getMethod("getCommentaire").invoke(op);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object getType(Operation op) {
        try {
            Object t = op.getClass().getMethod("getType").invoke(op);
            if (t != null) return t.toString();
        } catch (Throwable ignored) {}
        try {
            Object t = op.getClass().getMethod("getTypeOperation").invoke(op);
            if (t != null) return t.toString();
        } catch (Throwable ignored) {}
        return "OPERATION";
    }

    private Object safeGet(SafeCallable<Object> fn) {
        try { return fn.call(); } catch (Throwable t) { return null; }
    }

    @FunctionalInterface
    private interface SafeCallable<T> {
        T call() throws Exception;
    }
}