package com.banking.controllers;

import com.banking.dto.response.ApiResponse;
import com.banking.dto.response.CompteEpargneResponse;
import com.banking.entities.CompteEpargne;
import com.banking.services.CompteBancaireService;
import com.banking.services.CompteEpargneService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/savings")
@CrossOrigin(origins = "*", maxAge = 3600)
@Validated
public class CompteEpargneController {

    @Autowired
    private CompteEpargneService compteEpargneService;

    @Autowired
    private CompteBancaireService compteBancaireService;

    /** Détails du compte épargne via l'id du compte bancaire */
    @GetMapping("/compte/{compteId}")
    public ResponseEntity<ApiResponse<CompteEpargneResponse>> getSavingsByCompteId(@PathVariable Long compteId) {
        CompteEpargne epargne = compteEpargneService.findByCompteId(compteId);
        String numCompte = (epargne.getCompteBancaire() != null) ? epargne.getCompteBancaire().getNumCompte() : null;
        return ResponseEntity.ok(ApiResponse.success(
                "Compte épargne trouvé",
                CompteEpargneResponse.fromEntity(epargne, numCompte)
        ));
    }

    /** Détails via le numéro de compte (recommandé) */
    @GetMapping("/{numCompte}")
    public ResponseEntity<ApiResponse<CompteEpargneResponse>> getSavingsByNum(@PathVariable String numCompte) {
        CompteEpargne epargne = compteEpargneService.findByNumCompte(numCompte);
        return ResponseEntity.ok(ApiResponse.success(
                "Compte épargne trouvé",
                CompteEpargneResponse.fromEntity(epargne, numCompte)
        ));
    }

    /** Calcul des intérêts annuels (aperçu, sans capitaliser) */
    @GetMapping("/{numCompte}/interets")
    public ResponseEntity<ApiResponse<BigDecimal>> calculerInterets(@PathVariable String numCompte) {
        BigDecimal interets = compteEpargneService.calculerInterets(numCompte);
        return ResponseEntity.ok(ApiResponse.success("Intérêts calculés", interets));
    }

    /** Capitaliser les intérêts annuels (ajoute au solde épargne) */
    @PostMapping("/{numCompte}/capitaliser")
    public ResponseEntity<ApiResponse<CompteEpargneResponse>> capitaliserInterets(@PathVariable String numCompte) {
        compteEpargneService.capitaliserInterets(numCompte);
        CompteEpargne epargne = compteEpargneService.findByNumCompte(numCompte);
        return ResponseEntity.ok(ApiResponse.success(
                "Capitalisation effectuée",
                CompteEpargneResponse.fromEntity(epargne, numCompte)
        ));
    }

    /** Indique si la capitalisation annuelle est due */
    @GetMapping("/{numCompte}/doit-capitaliser")
    public ResponseEntity<ApiResponse<Boolean>> doitCapitaliser(@PathVariable String numCompte) {
        boolean resultat = compteEpargneService.doitCapitaliser(numCompte);
        return ResponseEntity.ok(ApiResponse.success("Nécessite capitalisation ?", resultat));
    }

    /** Alimenter l'épargne (virement courant -> épargne du même compte) */
    @PostMapping("/{numCompte}/alimenter")
    public ResponseEntity<ApiResponse<CompteEpargneResponse>> alimenter(
            @PathVariable String numCompte,
            @RequestBody @Validated MontantRequest req
    ) {
        CompteEpargne updated = compteEpargneService.alimenter(numCompte, req.getMontant());
        return ResponseEntity.ok(ApiResponse.success(
                "Alimentation épargne effectuée",
                CompteEpargneResponse.fromEntity(updated, numCompte)
        ));
    }

    /** Retirer depuis l'épargne (virement épargne -> courant du même compte) */
    @PostMapping("/{numCompte}/retirer")
    public ResponseEntity<ApiResponse<CompteEpargneResponse>> retirer(
            @PathVariable String numCompte,
            @RequestBody @Validated MontantRequest req
    ) {
        CompteEpargne updated = compteEpargneService.retirer(numCompte, req.getMontant());
        return ResponseEntity.ok(ApiResponse.success(
                "Retrait épargne effectué",
                CompteEpargneResponse.fromEntity(updated, numCompte)
        ));
    }

    /** Mettre à jour le taux d'intérêt (ex: 0.025 = 2.5%) */
    @PatchMapping("/{numCompte}/taux")
    public ResponseEntity<ApiResponse<CompteEpargneResponse>> updateTaux(
            @PathVariable String numCompte,
            @RequestBody @Validated TauxRequest req
    ) {
        CompteEpargne updated = compteEpargneService.updateTaux(numCompte, req.getTauxInteret());
        return ResponseEntity.ok(ApiResponse.success(
                "Taux d'intérêt mis à jour",
                CompteEpargneResponse.fromEntity(updated, numCompte)
        ));
    }

    /** Convertir/Créer le compte épargne rattaché au compte courant */
    @PostMapping("/{numCompte}/convertir")
    public ResponseEntity<ApiResponse<CompteEpargneResponse>> convertir(
            @PathVariable String numCompte,
            @RequestBody @Validated ConvertRequest req
    ) {
        var ce = compteBancaireService.convertToSavingsAccount(numCompte, req.getTauxInteret());
        return ResponseEntity.ok(ApiResponse.success(
                "Compte converti en épargne",
                CompteEpargneResponse.fromEntity(ce, numCompte)
        ));
    }

    /* ====================== DTOs requis pour les requêtes ====================== */

    @Getter @Setter
    public static class MontantRequest {
        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal montant;
    }

    @Getter @Setter
    public static class TauxRequest {
        /** Entre 0 et 1 (inclus). Exemple: 0.0125 = 1.25% */
        @NotNull
        @DecimalMin(value = "0.0")
        private BigDecimal tauxInteret;
    }

    @Getter @Setter
    public static class ConvertRequest {
        /** Entre 0 et 1 (inclus). Exemple: 0.0125 = 1.25% */
        @NotNull
        @DecimalMin(value = "0.0001")
        private BigDecimal tauxInteret;
    }
}

