package com.banking.controllers;

import com.banking.entities.ReleveDeCompte;
import com.banking.repositories.ReleveDeCompteRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/releves")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ReleveDeCompteController {

    private final ReleveDeCompteRepository repo;

    public ReleveDeCompteController(ReleveDeCompteRepository repo) {
        this.repo = repo;
    }

    // GET /api/releves/{numCompte}?start=2025-08-01T00:00:00&end=2025-08-31T23:59:59
    @GetMapping("/{numCompte}")
    public ResponseEntity<List<ReleveDeCompte>> getHistorique(
            @PathVariable String numCompte,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        if (start != null && end != null) {
            return ResponseEntity.ok(
                    repo.findByNumCompteAndDateOperationBetweenOrderByDateOperationAsc(numCompte, start, end)
            );
        }
        return ResponseEntity.ok(repo.findByNumCompteOrderByDateOperationDesc(numCompte));
    }

    // GET /api/releves/{numCompte}/last/10
    @GetMapping("/{numCompte}/last/{n}")
    public ResponseEntity<List<ReleveDeCompte>> getDerniers(
            @PathVariable String numCompte, @PathVariable int n
    ) {
        // Simple: on prend tout et on tronque (si tu veux optimisé, fais un query paginé)
        List<ReleveDeCompte> all = repo.findByNumCompteOrderByDateOperationDesc(numCompte);
        int to = Math.min(n, all.size());
        return ResponseEntity.ok(all.subList(0, to));
    }
}
