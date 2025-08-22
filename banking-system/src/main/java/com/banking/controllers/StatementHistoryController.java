package com.banking.controllers;

import com.banking.entities.ReleveDeCompte;
import com.banking.services.StatementHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statements")
public class StatementHistoryController {

    private final StatementHistoryService service;

    public StatementHistoryController(StatementHistoryService service) {
        this.service = service;
    }

    // ➜ Crée et enregistre le relevé mensuel (snapshot) pour un compte donné
    // POST /api/statements/{numeroCompte}/monthly?year=2025&month=8
    @PostMapping("/{numeroCompte}/monthly")
    public ResponseEntity<ReleveDeCompte> createMonthly(
            @PathVariable String numeroCompte,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(service.persistMonthly(numeroCompte, year, month));
    }

    // ➜ Liste les relevés enregistrés en BDD pour ce compte (ordre décroissant)
    // GET /api/statements/{numeroCompte}/history
    @GetMapping("/{numeroCompte}/history")
    public ResponseEntity<List<ReleveDeCompte>> history(@PathVariable String numeroCompte) {
        return ResponseEntity.ok(service.listHistory(numeroCompte));
    }
}
