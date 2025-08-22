package com.banking.controllers;

import com.banking.dto.request.FraisDeGestionCreateRequest;
import com.banking.dto.request.FraisDeGestionUpdateRequest;
import com.banking.dto.response.FraisDeGestionResponse;
import com.banking.dto.response.FraisStatistiquesResponse;
import com.banking.entities.FraisDeGestion;
import com.banking.services.FraisDeGestionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/frais")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FraisDeGestionController {

    private static final Logger logger = LoggerFactory.getLogger(FraisDeGestionController.class);

    @Autowired
    private FraisDeGestionService fraisService;

    // ==================== CRUD DE BASE ====================

    @PostMapping
    public ResponseEntity<Map<String, Object>> creerFrais(
            @Valid @RequestBody FraisDeGestionCreateRequest request) {

        logger.info("Création d'un nouveau frais de type: {}", request.getTypeFrais());

        try {
            // Conversion DTO vers Entity
            FraisDeGestion frais = convertirVersEntity(request);

            FraisDeGestion fraisCree = fraisService.creerFrais(frais);
            FraisDeGestionResponse response = new FraisDeGestionResponse(fraisCree);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Frais créé avec succès",
                    "data", response
            ));

        } catch (Exception e) {
            logger.error("Erreur lors de la création du frais", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la création: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenirFrais(
            @PathVariable Long id) {

        try {
            FraisDeGestion frais = fraisService.obtenirFrais(id);
            FraisDeGestionResponse response = new FraisDeGestionResponse(frais);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", response
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "Frais non trouvé: " + e.getMessage()
            ));
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listerTousFrais() {

        try {
            List<FraisDeGestion> frais = fraisService.obtenirTousFrais();
            List<FraisDeGestionResponse> responses = frais.stream()
                    .map(FraisDeGestionResponse::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", responses,
                    "count", responses.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la récupération: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> mettreAJourFrais(
            @PathVariable Long id,
            @Valid @RequestBody FraisDeGestionUpdateRequest request) {

        logger.info("Mise à jour du frais ID: {}", id);

        try {
            FraisDeGestion fraisModifie = new FraisDeGestion();
            BeanUtils.copyProperties(request, fraisModifie);

            FraisDeGestion fraisMisAJour = fraisService.mettreAJourFrais(id, fraisModifie);
            FraisDeGestionResponse response = new FraisDeGestionResponse(fraisMisAJour);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Frais mis à jour avec succès",
                    "data", response
            ));

        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour du frais ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la mise à jour: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> supprimerFrais(
            @PathVariable Long id) {

        logger.info("Suppression du frais ID: {}", id);

        try {
            fraisService.supprimerFrais(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Frais supprimé avec succès"
            ));

        } catch (Exception e) {
            logger.error("Erreur lors de la suppression du frais ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la suppression: " + e.getMessage()
            ));
        }
    }

    // ==================== GESTION PAR CLIENT ====================

    @GetMapping("/client/{clientId}")
    public ResponseEntity<Map<String, Object>> obtenirFraisClient(
            @PathVariable Long clientId) {

        try {
            List<FraisDeGestion> frais = fraisService.obtenirFraisClient(clientId);
            List<FraisDeGestionResponse> responses = frais.stream()
                    .map(FraisDeGestionResponse::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", responses,
                    "count", responses.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "Erreur: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/client/{clientId}/actifs")
    public ResponseEntity<Map<String, Object>> obtenirFraisActifsClient(
            @PathVariable Long clientId) {

        try {
            List<FraisDeGestion> frais = fraisService.obtenirFraisActifsClient(clientId);
            List<FraisDeGestionResponse> responses = frais.stream()
                    .map(FraisDeGestionResponse::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", responses,
                    "count", responses.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "Erreur: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/client/{clientId}/en-cours")
    public ResponseEntity<Map<String, Object>> obtenirFraisEnCoursClient(
            @PathVariable Long clientId) {

        try {
            List<FraisDeGestion> frais = fraisService.obtenirFraisEnCoursClient(clientId);
            List<FraisDeGestionResponse> responses = frais.stream()
                    .map(FraisDeGestionResponse::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", responses,
                    "count", responses.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "Erreur: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/client/{clientId}")
    public ResponseEntity<Map<String, Object>> creerFraisClient(
            @PathVariable Long clientId,
            @Valid @RequestBody FraisDeGestionCreateRequest request) {

        logger.info("Création d'un frais pour le client ID: {}", clientId);

        try {
            FraisDeGestion frais = convertirVersEntity(request);
            FraisDeGestion fraisCree = fraisService.creerFraisClient(clientId, frais);
            FraisDeGestionResponse response = new FraisDeGestionResponse(fraisCree);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Frais créé avec succès pour le client",
                    "data", response
            ));

        } catch (Exception e) {
            logger.error("Erreur lors de la création du frais pour le client ID: {}", clientId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la création: " + e.getMessage()
            ));
        }
    }

    // ==================== FACTURATION ====================

    @GetMapping("/a-facturer")
    public ResponseEntity<Map<String, Object>> obtenirFraisAFacturer() {

        try {
            List<FraisDeGestion> frais = fraisService.obtenirFraisAFacturer();
            List<FraisDeGestionResponse> responses = frais.stream()
                    .map(FraisDeGestionResponse::new)
                    .collect(Collectors.toList());

            BigDecimal montantTotal = frais.stream()
                    .map(FraisDeGestion::getMontant)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", responses,
                    "count", responses.size(),
                    "montantTotal", montantTotal
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Erreur: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/facturer")
    public ResponseEntity<Map<String, Object>> facturerFrais(
            @PathVariable Long id) {

        logger.info("Facturation du frais ID: {}", id);

        try {
            fraisService.facturerFrais(id);

            // Récupérer le frais mis à jour
            FraisDeGestion fraisMisAJour = fraisService.obtenirFrais(id);
            FraisDeGestionResponse response = new FraisDeGestionResponse(fraisMisAJour);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Frais facturé avec succès",
                    "data", response
            ));

        } catch (Exception e) {
            logger.error("Erreur lors de la facturation du frais ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la facturation: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/client/{clientId}/facturer")
    public ResponseEntity<Map<String, Object>> facturerFraisClient(
            @PathVariable Long clientId) {

        logger.info("Facturation des frais du client ID: {}", clientId);

        try {
            fraisService.facturerFraisClient(clientId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Facturation terminée pour le client"
            ));

        } catch (Exception e) {
            logger.error("Erreur lors de la facturation des frais du client ID: {}", clientId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la facturation: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/facturation-automatique")
    public ResponseEntity<Map<String, Object>> lancerFacturationAutomatique() {

        logger.info("Lancement de la facturation automatique");

        try {
            fraisService.facturationAutomatique();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Facturation automatique lancée avec succès"
            ));

        } catch (Exception e) {
            logger.error("Erreur lors de la facturation automatique", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la facturation automatique: " + e.getMessage()
            ));
        }
    }

    // ==================== GESTION DU CYCLE DE VIE ====================

    @PutMapping("/{id}/activer")
    public ResponseEntity<Map<String, Object>> activerFrais(
            @PathVariable Long id) {

        logger.info("Activation du frais ID: {}", id);

        try {
            fraisService.activerFrais(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Frais activé avec succès"
            ));

        } catch (Exception e) {
            logger.error("Erreur lors de l'activation du frais ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Erreur lors de l'activation: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}/desactiver")
    public ResponseEntity<Map<String, Object>> desactiverFrais(
            @PathVariable Long id) {

        logger.info("Désactivation du frais ID: {}", id);

        try {
            fraisService.desactiverFrais(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Frais désactivé avec succès"
            ));

        } catch (Exception e) {
            logger.error("Erreur lors de la désactivation du frais ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la désactivation: " + e.getMessage()
            ));
        }
    }

    // ==================== STATISTIQUES ====================

    @GetMapping("/client/{clientId}/statistiques")
    public ResponseEntity<Map<String, Object>> obtenirStatistiquesClient(
            @PathVariable Long clientId) {

        try {
            Long nombreFraisActifs = fraisService.compterFraisActifsClient(clientId);
            BigDecimal montantTotalActif = fraisService.calculerMontantTotalActifClient(clientId);
            BigDecimal montantTotalFacture = fraisService.calculerMontantTotalFactureClient(clientId);

            FraisStatistiquesResponse stats = new FraisStatistiquesResponse(
                    nombreFraisActifs, montantTotalActif, montantTotalFacture,
                    nombreFraisActifs > 0 ? montantTotalActif.divide(BigDecimal.valueOf(nombreFraisActifs)) : BigDecimal.ZERO
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", stats
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "Erreur: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/client/{clientId}/historique")
    public ResponseEntity<Map<String, Object>> obtenirHistoriqueFacturation(
            @PathVariable Long clientId) {

        try {
            List<FraisDeGestion> historique = fraisService.obtenirHistoriqueFacturationClient(clientId);
            List<FraisDeGestionResponse> responses = historique.stream()
                    .map(FraisDeGestionResponse::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", responses,
                    "count", responses.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "Erreur: " + e.getMessage()
            ));
        }
    }

    // ==================== RECHERCHES AVANCÉES ====================

    @GetMapping("/recherche")
    public ResponseEntity<Map<String, Object>> rechercherFrais(
            @RequestParam(required = false) FraisDeGestion.TypeFrais typeFrais,
            @RequestParam(required = false) FraisDeGestion.Periodicite periodicite,
            @RequestParam(required = false) Boolean estActif,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        try {
            List<FraisDeGestion> frais = fraisService.rechercherFrais(typeFrais, periodicite, estActif, dateDebut, dateFin);
            List<FraisDeGestionResponse> responses = frais.stream()
                    .map(FraisDeGestionResponse::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", responses,
                    "count", responses.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la recherche: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> obtenirTypesFrais() {

        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", FraisDeGestion.TypeFrais.values()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Erreur: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/periodicites")
    public ResponseEntity<Map<String, Object>> obtenirPeriodicites() {

        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", FraisDeGestion.Periodicite.values()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Erreur: " + e.getMessage()
            ));
        }
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Convertit un DTO de création vers une entité
     */
    private FraisDeGestion convertirVersEntity(FraisDeGestionCreateRequest request) {
        FraisDeGestion frais = new FraisDeGestion();
        frais.setMontant(request.getMontant());
        frais.setDescription(request.getDescription());
        frais.setDateDebut(request.getDateDebut());
        frais.setDateFin(request.getDateFin());
        frais.setTypeFrais(request.getTypeFrais());
        frais.setPeriodicite(request.getPeriodicite());
        frais.setEstActif(request.getEstActif() != null ? request.getEstActif() : true);
        return frais;
    }
}