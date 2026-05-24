package com.banking.controllers;

import com.banking.dto.request.FraisDeGestionCreateRequest;
import com.banking.dto.request.FraisDeGestionUpdateRequest;
import com.banking.dto.response.FraisDeGestionResponse;
import com.banking.dto.response.FraisStatistiquesResponse;
import com.banking.entities.CompteBancaire;
import com.banking.entities.FraisDeGestion;
import com.banking.entities.Operation;
import com.banking.entity.enums.Periodicite;
import com.banking.entity.enums.TypeFrais;
import com.banking.entity.enums.TypeOperation;
import com.banking.repositories.CompteBancaireRepository;
import com.banking.repositories.OperationRepository;
import com.banking.services.FraisDeGestionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/frais")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FraisDeGestionController {

    private static final Logger logger = LoggerFactory.getLogger(FraisDeGestionController.class);

    @Autowired private FraisDeGestionService fraisService;
    @Autowired private CompteBancaireRepository compteRepo;
    @Autowired private OperationRepository operationRepository;
    @PersistenceContext private EntityManager em;

    // ========= Utils =========
    private Map<String, Object> map(Object... kv) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2)
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        return m;
    }

    // ==================== CRUD DE BASE ====================

    @PostMapping
    public ResponseEntity<Map<String, Object>> creerFrais(
            @Valid @RequestBody FraisDeGestionCreateRequest request) {
        try {
            logger.info("Création frais type: {}", request.getTypeFrais());
            FraisDeGestion frais = convertirVersEntity(request);

            // 🔑 Attacher le compte bancaire si fourni
            attacherCompteBancaire(frais, request);

            FraisDeGestion fraisCree = fraisService.creerFrais(frais);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    map("success", true, "message", "Frais créé avec succès",
                            "data", new FraisDeGestionResponse(fraisCree)));
        } catch (Exception e) {
            logger.error("Erreur création frais", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenirFrais(@PathVariable Long id) {
        try {
            FraisDeGestion frais = fraisService.obtenirFrais(id);
            return ResponseEntity.ok(map("success", true, "data", new FraisDeGestionResponse(frais)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    map("success", false, "message", "Frais non trouvé: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listerTousFrais() {
        try {
            List<FraisDeGestionResponse> responses = fraisService.obtenirTousFrais().stream()
                    .map(FraisDeGestionResponse::new).collect(Collectors.toList());
            return ResponseEntity.ok(map("success", true, "data", responses, "count", responses.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> mettreAJourFrais(
            @PathVariable Long id,
            @Valid @RequestBody FraisDeGestionUpdateRequest request) {
        try {
            FraisDeGestion fraisModifie = new FraisDeGestion();
            BeanUtils.copyProperties(request, fraisModifie);
            FraisDeGestion fraisMisAJour = fraisService.mettreAJourFrais(id, fraisModifie);
            return ResponseEntity.ok(map("success", true, "message", "Frais mis à jour avec succès",
                    "data", new FraisDeGestionResponse(fraisMisAJour)));
        } catch (Exception e) {
            logger.error("Erreur mise à jour frais ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> supprimerFrais(@PathVariable Long id) {
        try {
            fraisService.supprimerFrais(id);
            return ResponseEntity.ok(map("success", true, "message", "Frais supprimé avec succès"));
        } catch (Exception e) {
            logger.error("Erreur suppression frais ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    // ==================== GESTION PAR CLIENT ====================

    @GetMapping("/client/{clientId}")
    public ResponseEntity<Map<String, Object>> obtenirFraisClient(@PathVariable Long clientId) {
        try {
            List<FraisDeGestionResponse> responses = fraisService.obtenirFraisClient(clientId)
                    .stream().map(FraisDeGestionResponse::new).collect(Collectors.toList());
            return ResponseEntity.ok(map("success", true, "data", responses, "count", responses.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/client/{clientId}/actifs")
    public ResponseEntity<Map<String, Object>> obtenirFraisActifsClient(@PathVariable Long clientId) {
        try {
            List<FraisDeGestionResponse> responses = fraisService.obtenirFraisActifsClient(clientId)
                    .stream().map(FraisDeGestionResponse::new).collect(Collectors.toList());
            return ResponseEntity.ok(map("success", true, "data", responses, "count", responses.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/client/{clientId}/en-cours")
    public ResponseEntity<Map<String, Object>> obtenirFraisEnCoursClient(@PathVariable Long clientId) {
        try {
            List<FraisDeGestionResponse> responses = fraisService.obtenirFraisEnCoursClient(clientId)
                    .stream().map(FraisDeGestionResponse::new).collect(Collectors.toList());
            return ResponseEntity.ok(map("success", true, "data", responses, "count", responses.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    @PostMapping("/client/{clientId}")
    public ResponseEntity<Map<String, Object>> creerFraisClient(
            @PathVariable Long clientId,
            @Valid @RequestBody FraisDeGestionCreateRequest request) {
        try {
            FraisDeGestion frais = convertirVersEntity(request);

            // 🔑 Le compte bancaire est obligatoire pour cette route
            if (request.getCompteBancaireId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        map("success", false, "message", "compteBancaireId est requis"));
            }
            CompteBancaire compte = compteRepo.findById(request.getCompteBancaireId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Compte introuvable: " + request.getCompteBancaireId()));
            frais.setCompteBancaire(compte);

            FraisDeGestion fraisCree = fraisService.creerFraisClient(clientId, frais);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    map("success", true, "message", "Frais créé pour le client",
                            "data", new FraisDeGestionResponse(fraisCree)));
        } catch (Exception e) {
            logger.error("Erreur création frais client ID: {}", clientId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    // ==================== FACTURATION ====================

    @GetMapping("/a-facturer")
    public ResponseEntity<Map<String, Object>> obtenirFraisAFacturer() {
        try {
            List<FraisDeGestion> frais = fraisService.obtenirFraisAFacturer();
            List<FraisDeGestionResponse> responses = frais.stream()
                    .map(FraisDeGestionResponse::new).collect(Collectors.toList());
            BigDecimal montantTotal = frais.stream()
                    .map(f -> f.getMontant() != null ? f.getMontant() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return ResponseEntity.ok(map("success", true, "data", responses,
                    "count", responses.size(), "montantTotal", montantTotal));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/facturer")
    public ResponseEntity<Map<String, Object>> facturerFrais(@PathVariable Long id) {
        try {
            fraisService.facturerFrais(id);
            FraisDeGestion fraisMisAJour = fraisService.obtenirFrais(id);
            return ResponseEntity.ok(map("success", true, "message", "Frais facturé avec succès",
                    "data", new FraisDeGestionResponse(fraisMisAJour)));
        } catch (Exception e) {
            logger.error("Erreur facturation frais ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    @PostMapping("/client/{clientId}/facturer")
    public ResponseEntity<Map<String, Object>> facturerFraisClient(@PathVariable Long clientId) {
        try {
            fraisService.facturerFraisClient(clientId);
            return ResponseEntity.ok(map("success", true, "message", "Facturation terminée pour le client"));
        } catch (Exception e) {
            logger.error("Erreur facturation client ID: {}", clientId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    @PostMapping("/facturation-automatique")
    public ResponseEntity<Map<String, Object>> lancerFacturationAutomatique() {
        try {
            fraisService.facturationAutomatique();
            return ResponseEntity.ok(map("success", true, "message", "Facturation automatique lancée"));
        } catch (Exception e) {
            logger.error("Erreur facturation automatique", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    // ==================== TRIGGERS DE FRAIS ====================

    @PostMapping("/fees/ouverture/{numCompte}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> appliquerFraisOuverture(@PathVariable String numCompte) {
        CompteBancaire cb = compteRepo.findByNumCompte(numCompte)
                .orElseThrow(() -> new EntityNotFoundException("Compte introuvable: " + numCompte));
        BigDecimal before = cb.getBalance() == null ? BigDecimal.ZERO : cb.getBalance();
        fraisService.appliquerFraisOuverture(numCompte);
        cb = compteRepo.findById(cb.getId())
                .orElseThrow(() -> new EntityNotFoundException("Compte introuvable après"));
        BigDecimal after = cb.getBalance() == null ? BigDecimal.ZERO : cb.getBalance();
        return ResponseEntity.ok(map(
                "success", true, "message", "Frais d'ouverture appliqué (si éligible)",
                "numCompte", numCompte, "before", before, "after", after,
                "debite", before.subtract(after)));
    }

    @PostMapping("/fees/gestion/{numCompte}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> appliquerFraisGestionUn(
            @PathVariable String numCompte,
            @RequestParam(defaultValue = "2.89") BigDecimal montant,
            @RequestParam(defaultValue = "false") boolean autoriserDecouvert) {

        String phase = "start";
        try {
            if (montant == null || montant.signum() <= 0)
                return ResponseEntity.badRequest().body(map("success", false, "message", "Montant invalide"));

            phase = "loadCompte";
            CompteBancaire cb = compteRepo.findByNumCompte(numCompte)
                    .orElseThrow(() -> new EntityNotFoundException("Compte introuvable: " + numCompte));

            BigDecimal before = cb.getBalance() == null ? BigDecimal.ZERO : cb.getBalance();
            if (!autoriserDecouvert && before.compareTo(montant) < 0)
                return ResponseEntity.badRequest().body(map(
                        "success", false, "message", "Solde insuffisant",
                        "numCompte", numCompte, "solde", before));

            phase = "debit";
            cb.setBalance(before.subtract(montant));
            compteRepo.save(cb);

            phase = "journal";
            try {
                Operation op = new Operation();
                op.setCompteBancaire(cb);
                op.setMontant(montant);
                op.setDescription("Frais de gestion");
                op.setCommentaire("FRAIS_GESTION_MANUEL");
                setOperationDate(op);
                setOperationType(op);
                operationRepository.save(op);
            } catch (Exception jex) {
                logger.warn("Journalisation échouée pour {}: {}", numCompte, jex.toString());
            }

            BigDecimal after = cb.getBalance() == null ? BigDecimal.ZERO : cb.getBalance();
            return ResponseEntity.ok(map(
                    "success", true, "message", "Frais de gestion appliqué",
                    "numCompte", numCompte, "before", before, "after", after,
                    "debite", before.subtract(after)));

        } catch (Exception e) {
            logger.error("appliquerFraisGestionUn failed phase={}", phase, e);
            return ResponseEntity.status(500).body(map(
                    "success", false, "message", "Échec application frais",
                    "phase", phase,
                    "error", e.getClass().getSimpleName() + ": " +
                            (e.getMessage() == null ? "(no message)" : e.getMessage())));
        }
    }

    @PostMapping("/fees/gestion/apply-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> appliquerFraisGestionTous(
            @RequestParam(defaultValue = "2.89") BigDecimal montant,
            @RequestParam(defaultValue = "false") boolean autoriserDecouvert) {

        if (montant == null || montant.signum() <= 0)
            return ResponseEntity.badRequest().body(map("success", false, "message", "Montant invalide"));

        int total = 0, appliques = 0, insuffisants = 0, erreurs = 0, journalWarn = 0;
        try {
            for (CompteBancaire cb : compteRepo.findAll()) {
                total++;
                try {
                    BigDecimal before = cb.getBalance() == null ? BigDecimal.ZERO : cb.getBalance();
                    if (!autoriserDecouvert && before.compareTo(montant) < 0) { insuffisants++; continue; }
                    cb.setBalance(before.subtract(montant));
                    compteRepo.save(cb);
                    try {
                        Operation op = new Operation();
                        op.setCompteBancaire(cb);
                        op.setMontant(montant);
                        op.setDescription("Frais de gestion (apply-all)");
                        op.setCommentaire("FRAIS_GESTION_APPLY_ALL_" + YearMonth.now());
                        setOperationDate(op);
                        setOperationType(op);
                        operationRepository.save(op);
                    } catch (Exception jex) {
                        journalWarn++;
                        logger.warn("Journal KO {}: {}", cb.getNumCompte(), jex.toString());
                    }
                    appliques++;
                } catch (Exception one) {
                    erreurs++;
                    logger.error("Apply-all KO {}: {}", cb.getNumCompte(), one.toString());
                }
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", total);
            stats.put("appliques", appliques);
            stats.put("soldeInsuffisant", insuffisants);
            stats.put("journalWarnings", journalWarn);
            stats.put("erreurs", erreurs);

            return ResponseEntity.ok(map("success", true, "message", "Frais de gestion appliqués",
                    "montant", montant, "stats", stats));
        } catch (Exception e) {
            logger.error("appliquerFraisGestionTous failed", e);
            return ResponseEntity.status(500).body(map("success", false, "message", "Échec apply-all",
                    "error", e.getClass().getSimpleName() + ": " +
                            (e.getMessage() == null ? "(no message)" : e.getMessage())));
        }
    }

    @PostMapping("/fees/gestion/force/{numCompte}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> forcerFraisUn(
            @PathVariable String numCompte,
            @RequestParam(defaultValue = "2.89") BigDecimal montant,
            @RequestParam(defaultValue = "true") boolean autoriserDecouvert) {
        return appliquerFraisGestionUn(numCompte, montant, autoriserDecouvert);
    }

    @PostMapping("/fees/gestion/force-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> forcerFraisTous(
            @RequestParam(defaultValue = "2.89") BigDecimal montant,
            @RequestParam(defaultValue = "true") boolean autoriserDecouvert) {
        return appliquerFraisGestionTous(montant, autoriserDecouvert);
    }

    // ==================== GESTION DU CYCLE DE VIE ====================

    @PutMapping("/{id}/activer")
    public ResponseEntity<Map<String, Object>> activerFrais(@PathVariable Long id) {
        try {
            fraisService.activerFrais(id);
            return ResponseEntity.ok(map("success", true, "message", "Frais activé avec succès"));
        } catch (Exception e) {
            logger.error("Erreur activation frais ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/desactiver")
    public ResponseEntity<Map<String, Object>> desactiverFrais(@PathVariable Long id) {
        try {
            fraisService.desactiverFrais(id);
            return ResponseEntity.ok(map("success", true, "message", "Frais désactivé avec succès"));
        } catch (Exception e) {
            logger.error("Erreur désactivation frais ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    // ==================== STATISTIQUES ====================

    @GetMapping("/client/{clientId}/statistiques")
    public ResponseEntity<Map<String, Object>> obtenirStatistiquesClient(@PathVariable Long clientId) {
        try {
            Long nombreFraisActifs         = fraisService.compterFraisActifsClient(clientId);
            BigDecimal montantTotalActif   = fraisService.calculerMontantTotalActifClient(clientId);
            BigDecimal montantTotalFacture = fraisService.calculerMontantTotalFactureClient(clientId);
            BigDecimal moyenne = BigDecimal.ZERO;
            if (nombreFraisActifs != null && nombreFraisActifs > 0 && montantTotalActif != null)
                moyenne = montantTotalActif.divide(BigDecimal.valueOf(nombreFraisActifs), BigDecimal.ROUND_HALF_UP);
            return ResponseEntity.ok(map("success", true,
                    "data", new FraisStatistiquesResponse(nombreFraisActifs, montantTotalActif, montantTotalFacture, moyenne)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/client/{clientId}/historique")
    public ResponseEntity<Map<String, Object>> obtenirHistoriqueFacturation(@PathVariable Long clientId) {
        try {
            List<FraisDeGestionResponse> responses = fraisService.obtenirHistoriqueFacturationClient(clientId)
                    .stream().map(FraisDeGestionResponse::new).collect(Collectors.toList());
            return ResponseEntity.ok(map("success", true, "data", responses, "count", responses.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    // ==================== RECHERCHES AVANCÉES ====================

    @GetMapping("/recherche")
    public ResponseEntity<Map<String, Object>> rechercherFrais(
            @RequestParam(required = false) TypeFrais typeFrais,
            @RequestParam(required = false) Periodicite periodicite,
            @RequestParam(required = false) Boolean estActif,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        try {
            List<FraisDeGestionResponse> responses = fraisService
                    .rechercherFrais(typeFrais, periodicite, estActif, dateDebut, dateFin)
                    .stream().map(FraisDeGestionResponse::new).collect(Collectors.toList());
            return ResponseEntity.ok(map("success", true, "data", responses, "count", responses.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> obtenirTypesFrais() {
        try {
            return ResponseEntity.ok(map("success", true, "data", TypeFrais.values()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/periodicites")
    public ResponseEntity<Map<String, Object>> obtenirPeriodicites() {
        try {
            return ResponseEntity.ok(map("success", true, "data", Periodicite.values()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    // ==================== MÉTHODES UTILITAIRES ====================

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

    /**
     * 🔑 Attache le compte bancaire à un frais si l'ID est fourni dans le request.
     * Lève une exception si l'ID est fourni mais le compte n'existe pas.
     */
    private void attacherCompteBancaire(FraisDeGestion frais, FraisDeGestionCreateRequest request) {
        if (request.getCompteBancaireId() != null) {
            CompteBancaire compte = compteRepo.findById(request.getCompteBancaireId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Compte introuvable: " + request.getCompteBancaireId()));
            frais.setCompteBancaire(compte);
        }
    }

    private void enregistrerFraisGestion(CompteBancaire cb, BigDecimal montant, String tag) {
        try {
            LocalDate debut = LocalDate.now();
            FraisDeGestion fg = new FraisDeGestion();
            fg.setMontant(montant);
            fg.setDescription("FRAIS_GESTION " + tag + " / " + cb.getNumCompte());
            fg.setTypeFrais(TypeFrais.TENUE_COMPTE);
            fg.setPeriodicite(Periodicite.MENSUELLE);
            fg.setEstActif(true);
            fg.setDateDebut(debut);
            fg.setDateFin(debut.withDayOfMonth(debut.lengthOfMonth()));
            fg.setCompteBancaire(cb); // 🔑 important pour la contrainte NOT NULL
            try {
                fg.setClient(cb.getClient());
            } catch (Exception e) {
                logger.warn("Client manquant pour {}: pas d'insert FraisDeGestion", cb.getNumCompte());
                return;
            }
            em.persist(fg);
        } catch (Exception ex) {
            logger.warn("Insert FraisDeGestion KO pour {}: {}", cb.getNumCompte(), ex.toString());
        }
    }

    private String monthlyTag() {
        return "FRAIS_GESTION_" + YearMonth.now();
    }

    private String forcedTag() {
        return "FRAIS_GESTION_FORCE_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    private void setOperationDate(Operation op) {
        LocalDateTime now = LocalDateTime.now();
        try { op.getClass().getMethod("setDateOperation", LocalDateTime.class).invoke(op, now); return; } catch (Throwable ignored) {}
        try { op.getClass().getMethod("setDateOp",        LocalDateTime.class).invoke(op, now); return; } catch (Throwable ignored) {}
        try { op.getClass().getMethod("setDate",          LocalDateTime.class).invoke(op, now); } catch (Throwable ignored) {}
    }

    private void setOperationType(Operation op) {
        try { op.getClass().getMethod("setTypeOperation", TypeOperation.class).invoke(op, TypeOperation.FRAIS); return; } catch (Throwable ignored) {}
        try { op.getClass().getMethod("setType",          TypeOperation.class).invoke(op, TypeOperation.FRAIS); } catch (Throwable ignored) {}
    }
}