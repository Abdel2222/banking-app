package com.banking.controllers;

import com.banking.dto.request.DepositRequest;
import com.banking.dto.request.TransferRequest;
import com.banking.dto.request.WithdrawRequest;
import com.banking.dto.response.OperationResponse;
import com.banking.entities.Operation;
import com.banking.services.OperationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = {"/api/operations", "/api/transactions"}, produces = "application/json")
@CrossOrigin(origins = "*", maxAge = 3600)
@Validated
public class OperationController {

    private final OperationService operationService;

    // Dépôt (alias FR/EN)
    @PostMapping(value = {"/deposit", "/depot"}, consumes = "application/json")
    public ResponseEntity<OperationResponse> deposit(@Valid @RequestBody DepositRequest req) {
        Operation op = operationService.executeDeposit(req.getNumCompte(), req.getMontant(), req.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(OperationResponse.fromEntity(op));
    }

    // Retrait (alias FR/EN)
    @PostMapping(value = {"/withdraw", "/retrait"}, consumes = "application/json")
    public ResponseEntity<OperationResponse> withdraw(@Valid @RequestBody WithdrawRequest req) {
        Operation op = operationService.executeWithdrawal(req.getNumCompte(), req.getMontant(), req.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(OperationResponse.fromEntity(op));
    }

    // Virement (alias FR/EN)
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
}

