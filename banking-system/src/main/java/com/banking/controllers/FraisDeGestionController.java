package com.banking.controllers;

import com.banking.dto.request.FraisDeGestionCreateRequest;
import com.banking.dto.request.FraisDeGestionUpdateRequest;
import com.banking.dto.response.FraisDeGestionResponse;
import com.banking.dto.response.FraisStatistiquesResponse;
import com.banking.entities.CompteBancaire;
import com.banking.entities.FraisDeGestion;
import com.banking.entities.Operation;
import com.banking.repositories.CompteBancaireRepository;
import com.banking.repositories.OperationRepository;
import com.banking.services.FraisDeGestionService;
import com.banking.entity.enums.TypeOperation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.format.DateTimeFormatter;


import jakarta.persistence.EntityNotFoundException;
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
import java.util.LinkedHashMap;
import java.util.HashMap;
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
    @PersistenceContext
    private EntityManager em;


    // ========= Utils (compatibles Java 8) =========
    private Map<String, Object> map(Object... kv) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }

    // ==================== CRUD DE BASE ====================

    @PostMapping
    public ResponseEntity<Map<String, Object>> creerFrais(@Valid @RequestBody FraisDeGestionCreateRequest request) {
        try {
            logger.info("Création d'un nouveau frais de type: {}", request.getTypeFrais());
            FraisDeGestion frais = convertirVersEntity(request);
            FraisDeGestion fraisCree = fraisService.creerFrais(frais);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    map("success", true, "message", "Frais créé avec succès", "data", new FraisDeGestionResponse(fraisCree))
            );
        } catch (Exception e) {
            logger.error("Erreur lors de la création du frais", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    map("success", false, "message", "Erreur lors de la création: " + e.getMessage())
            );
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenirFrais(@PathVariable Long id) {
        try {
            FraisDeGestion frais = fraisService.obtenirFrais(id);
            return ResponseEntity.ok(map("success", true, "data", new FraisDeGestionResponse(frais)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    map("success", false, "message", "Frais non trouvé: " + e.getMessage())
            );
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listerTousFrais() {
        try {
            List<FraisDeGestion> frais = fraisService.obtenirTousFrais();
            List<FraisDeGestionResponse> responses = frais.stream().map(FraisDeGestionResponse::new).collect(Collectors.toList());
            return ResponseEntity.ok(map("success", true, "data", responses, "count", responses.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    map("success", false, "message", "Erreur lors de la récupération: " + e.getMessage())
            );
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> mettreAJourFrais(@PathVariable Long id,
                                                                @Valid @RequestBody FraisDeGestionUpdateRequest request) {
        try {
            logger.info("Mise à jour du frais ID: {}", id);
            FraisDeGestion fraisModifie = new FraisDeGestion();
            BeanUtils.copyProperties(request, fraisModifie);
            FraisDeGestion fraisMisAJour = fraisService.mettreAJourFrais(id, fraisModifie);
            return ResponseEntity.ok(map("success", true, "message", "Frais mis à jour avec succès",
                    "data", new FraisDeGestionResponse(fraisMisAJour)));
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour du frais ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    map("success", false, "message", "Erreur lors de la mise à jour: " + e.getMessage())
            );
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> supprimerFrais(@PathVariable Long id) {
        try {
            logger.info("Suppression du frais ID: {}", id);
            fraisService.supprimerFrais(id);
            return ResponseEntity.ok(map("success", true, "message", "Frais supprimé avec succès"));
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression du frais ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    map("success", false, "message", "Erreur lors de la suppression: " + e.getMessage())
            );
        }
    }

    // ==================== GESTION PAR CLIENT ====================

    @GetMapping("/client/{clientId}")
    public ResponseEntity<Map<String, Object>> obtenirFraisClient(@PathVariable Long clientId) {
        try {
            List<FraisDeGestion> frais = fraisService.obtenirFraisClient(clientId);
            List<FraisDeGestionResponse> responses = frais.stream().map(FraisDeGestionResponse::new).collect(Collectors.toList());
            return ResponseEntity.ok(map("success", true, "data", responses, "count", responses.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/client/{clientId}/actifs")
    public ResponseEntity<Map<String, Object>> obtenirFraisActifsClient(@PathVariable Long clientId) {
        try {
            List<FraisDeGestion> frais = fraisService.obtenirFraisActifsClient(clientId);
            List<FraisDeGestionResponse> responses = frais.stream().map(FraisDeGestionResponse::new).collect(Collectors.toList());
            return ResponseEntity.ok(map("success", true, "data", responses, "count", responses.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/client/{clientId}/en-cours")
    public ResponseEntity<Map<String, Object>> obtenirFraisEnCoursClient(@PathVariable Long clientId) {
        try {
            List<FraisDeGestion> frais = fraisService.obtenirFraisEnCoursClient(clientId);
            List<FraisDeGestionResponse> responses = frais.stream().map(FraisDeGestionResponse::new).collect(Collectors.toList());
            return ResponseEntity.ok(map("success", true, "data", responses, "count", responses.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    @PostMapping("/client/{clientId}")
    public ResponseEntity<Map<String, Object>> creerFraisClient(@PathVariable Long clientId,
                                                                @Valid @RequestBody FraisDeGestionCreateRequest request) {
        try {
            logger.info("Création d'un frais pour le client ID: {}", clientId);
            FraisDeGestion frais = convertirVersEntity(request);
            FraisDeGestion fraisCree = fraisService.creerFraisClient(clientId, frais);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    map("success", true, "message", "Frais créé avec succès pour le client",
                            "data", new FraisDeGestionResponse(fraisCree))
            );
        } catch (Exception e) {
            logger.error("Erreur lors de la création du frais pour le client ID: {}", clientId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    map("success", false, "message", "Erreur lors de la création: " + e.getMessage())
            );
        }
    }

    // ==================== FACTURATION ====================

    @GetMapping("/a-facturer")
    public ResponseEntity<Map<String, Object>> obtenirFraisAFacturer() {
        try {
            List<FraisDeGestion> frais = fraisService.obtenirFraisAFacturer();
            List<FraisDeGestionResponse> responses = frais.stream().map(FraisDeGestionResponse::new).collect(Collectors.toList());
            BigDecimal montantTotal = BigDecimal.ZERO;
            for (FraisDeGestion f : frais) {
                if (f.getMontant() != null) montantTotal = montantTotal.add(f.getMontant());
            }
            return ResponseEntity.ok(map("success", true, "data", responses, "count", responses.size(), "montantTotal", montantTotal));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    map("success", false, "message", "Erreur: " + e.getMessage())
            );
        }
    }

    @PostMapping("/{id}/facturer")
    public ResponseEntity<Map<String, Object>> facturerFrais(@PathVariable Long id) {
        try {
            logger.info("Facturation du frais ID: {}", id);
            fraisService.facturerFrais(id);
            FraisDeGestion fraisMisAJour = fraisService.obtenirFrais(id);
            return ResponseEntity.ok(map("success", true, "message", "Frais facturé avec succès",
                    "data", new FraisDeGestionResponse(fraisMisAJour)));
        } catch (Exception e) {
            logger.error("Erreur lors de la facturation du frais ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    map("success", false, "message", "Erreur lors de la facturation: " + e.getMessage())
            );
        }
    }

    @PostMapping("/client/{clientId}/facturer")
    public ResponseEntity<Map<String, Object>> facturerFraisClient(@PathVariable Long clientId) {
        try {
            logger.info("Facturation des frais du client ID: {}", clientId);
            fraisService.facturerFraisClient(clientId);
            return ResponseEntity.ok(map("success", true, "message", "Facturation terminée pour le client"));
        } catch (Exception e) {
            logger.error("Erreur lors de la facturation des frais du client ID: {}", clientId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    map("success", false, "message", "Erreur lors de la facturation: " + e.getMessage())
            );
        }
    }

    @PostMapping("/facturation-automatique")
    public ResponseEntity<Map<String, Object>> lancerFacturationAutomatique() {
        try {
            logger.info("Lancement de la facturation automatique");
            fraisService.facturationAutomatique();
            return ResponseEntity.ok(map("success", true, "message", "Facturation automatique lancée avec succès"));
        } catch (Exception e) {
            logger.error("Erreur lors de la facturation automatique", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    map("success", false, "message", "Erreur lors de la facturation automatique: " + e.getMessage())
            );
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

        cb = compteRepo.findById(cb.getId()).orElseThrow(() -> new EntityNotFoundException("Compte introuvable (après)"));
        BigDecimal after = cb.getBalance() == null ? BigDecimal.ZERO : cb.getBalance();

        return ResponseEntity.ok(map(
                "success", true, "message", "Frais d'ouverture appliqué (si éligible)",
                "numCompte", numCompte, "before", before, "after", after, "debite", before.subtract(after)
        ));
    }

    /**
     * Appliquer un frais de gestion sur UN compte (respecte autoriserDecouvert).
     * URL: POST /api/frais/fees/gestion/{numCompte}?montant=2.89&autoriserDecouvert=false
     */
    @PostMapping("/fees/gestion/{numCompte}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> appliquerFraisGestionUn(
            @PathVariable String numCompte,
            @RequestParam(defaultValue = "2.89") BigDecimal montant,
            @RequestParam(defaultValue = "false") boolean autoriserDecouvert) {

        String phase = "start";
        try {
            if (montant == null || montant.signum() <= 0) {
                return ResponseEntity.badRequest().body(map("success", false, "message", "Montant invalide"));
            }

            phase = "loadCompte";
            CompteBancaire cb = compteRepo.findByNumCompte(numCompte)
                    .orElseThrow(() -> new EntityNotFoundException("Compte introuvable: " + numCompte));

            BigDecimal before = cb.getBalance() == null ? BigDecimal.ZERO : cb.getBalance();
            if (!autoriserDecouvert && before.compareTo(montant) < 0) {
                return ResponseEntity.badRequest().body(map(
                        "success", false, "message", "Solde insuffisant",
                        "numCompte", numCompte, "solde", before
                ));
            }

            phase = "debit";
            cb.setBalance(before.subtract(montant));
            compteRepo.save(cb);

            // Journaliser (ne bloque pas le débit)
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
                logger.warn("Journalisation échouée pour {} : {}", numCompte, jex.toString());
            }

            BigDecimal after = cb.getBalance() == null ? BigDecimal.ZERO : cb.getBalance();
            return ResponseEntity.ok(map(
                    "success", true, "message", "Frais de gestion appliqué",
                    "numCompte", numCompte, "before", before, "after", after, "debite", before.subtract(after)
            ));
        } catch (Exception e) {
            logger.error("appliquerFraisGestionUn failed phase={}", phase, e);
            return ResponseEntity.status(500).body(map(
                    "success", false, "message", "Échec application frais de gestion",
                    "phase", phase, "error", e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "(no message)" : e.getMessage())
            ));
        }
    }

    /**
     * Appliquer un frais de gestion à TOUS les comptes.
     * URL: POST /api/frais/fees/gestion/apply-all?montant=2.89&autoriserDecouvert=false
     */
    @PostMapping("/fees/gestion/apply-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> appliquerFraisGestionTous(
            @RequestParam(defaultValue = "2.89") BigDecimal montant,
            @RequestParam(defaultValue = "false") boolean autoriserDecouvert) {

        if (montant == null || montant.signum() <= 0) {
            return ResponseEntity.badRequest().body(map("success", false, "message", "Montant invalide"));
        }

        int total = 0, appliques = 0, insuffisants = 0, erreurs = 0, journalWarn = 0;
        try {
            List<CompteBancaire> comptes = compteRepo.findAll();
            for (CompteBancaire cb : comptes) {
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
                    logger.error("Apply-all KO {}: {}", cb.getNumCompte(), one.toString(), one);
                }
            }

            Map<String, Object> stats = new HashMap<String, Object>();
            stats.put("total", total);
            stats.put("appliques", appliques);
            stats.put("soldeInsuffisant", insuffisants);
            stats.put("journalWarnings", journalWarn);
            stats.put("erreurs", erreurs);

            return ResponseEntity.ok(map("success", true, "message", "Frais de gestion appliqués", "montant", montant, "stats", stats));
        } catch (Exception e) {
            logger.error("appliquerFraisGestionTous failed", e);
            return ResponseEntity.status(500).body(map("success", false, "message", "Échec apply-all",
                    "error", e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "(no message)" : e.getMessage())));
        }
    }
    private void enregistrerFraisGestion(CompteBancaire cb, BigDecimal montant, String tag) {
        try {
            // dates obligatoires (dateFin NOT NULL dans ton entity)
            LocalDate debut = LocalDate.now();
            LocalDate fin   = debut.withDayOfMonth(debut.lengthOfMonth());

            FraisDeGestion fg = new FraisDeGestion();
            fg.setMontant(montant);
            fg.setDescription("FRAIS_GESTION " + tag + " / " + cb.getNumCompte());
            fg.setTypeFrais(FraisDeGestion.TypeFrais.TENUE_COMPTE);   // valeurs de ton enum
            fg.setPeriodicite(FraisDeGestion.Periodicite.MENSUEL);
            fg.setEstActif(true);
            fg.setDateDebut(debut);
            fg.setDateFin(fin);

            // champ obligatoire dans ton entity
            try {
                fg.setClient(cb.getClient());
            } catch (Exception e) {
                // si jamais cb.getClient() est null → on log et on sort (évite un 500)
                logger.warn("Client manquant pour compte {} : pas d'insert FraisDeGestion", cb.getNumCompte());
                return;
            }

            em.persist(fg);
        } catch (Exception ex) {
            // on n'empêche pas le débit si l’insert de journal échoue
            logger.warn("Insert FraisDeGestion KO pour {} : {}", cb.getNumCompte(), ex.toString());
        }
    }


    /**
     * FORCER le frais de gestion sur UN compte (autorise le découvert).
     * URL: POST /api/frais/fees/gestion/force/{numCompte}?montant=2.89&autoriserDecouvert=true
     */
    @PostMapping("/fees/gestion/force/{numCompte}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> forcerFraisUn(
            @PathVariable String numCompte,
            @RequestParam(defaultValue = "2.89") BigDecimal montant,
            @RequestParam(defaultValue = "true") boolean autoriserDecouvert) {

        // on délègue au même flux que "un compte" mais en autorisant le découvert par défaut
        return appliquerFraisGestionUn(numCompte, montant, autoriserDecouvert);
    }

    /**
     * FORCER le frais de gestion sur TOUS les comptes (autorise le découvert).
     * URL: POST /api/frais/fees/gestion/force-all?montant=2.89&autoriserDecouvert=true
     */
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
            logger.info("Activation du frais ID: {}", id);
            fraisService.activerFrais(id);
            return ResponseEntity.ok(map("success", true, "message", "Frais activé avec succès"));
        } catch (Exception e) {
            logger.error("Erreur lors de l'activation du frais ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    map("success", false, "message", "Erreur lors de l'activation: " + e.getMessage())
            );
        }
    }
    /** Insère une ligne dans la table FraisDeGestion en mode tolérant (évite les 500 si certains champs diffèrent). */
    private void insertFraisGestionRow(CompteBancaire cb, BigDecimal montant, String tag) {
        try {
            FraisDeGestion fg = new FraisDeGestion();
            // Champs “sûrs”
            try { fg.setMontant(montant); } catch (Throwable ignored) {}
            try { fg.setDescription("FRAIS_GESTION " + cb.getNumCompte() + " / " + tag); } catch (Throwable ignored) {}
            try { fg.setEstActif(Boolean.TRUE); } catch (Throwable ignored) {}
            try { fg.setDateDebut(LocalDate.now()); } catch (Throwable ignored) {}
            try { fg.setDateFin(LocalDate.now()); } catch (Throwable ignored) {}

            // TypeFrais : on essaie "GESTION", sinon “FRAIS_GESTION”, sinon on laisse tomber
            try {
                FraisDeGestion.TypeFrais tf;
                try { tf = FraisDeGestion.TypeFrais.valueOf("GESTION"); }
                catch (IllegalArgumentException e1) { tf = FraisDeGestion.TypeFrais.valueOf("FRAIS_GESTION"); }
                fg.setTypeFrais(tf);
            } catch (Throwable ignored) {}

            // Periodicite : on essaie “MENSUELLE”, sinon “MENSUEL”
            try {
                FraisDeGestion.Periodicite per;
                try { per = FraisDeGestion.Periodicite.valueOf("MENSUELLE"); }
                catch (IllegalArgumentException e1) { per = FraisDeGestion.Periodicite.valueOf("MENSUEL"); }
                fg.setPeriodicite(per);
            } catch (Throwable ignored) {}

            // Attacher le client si possible, via réflexion (aucun impact compilation)
            attachClientIfPossible(fg, cb);

            em.persist(fg);
        } catch (Exception ex) {
            logger.warn("Insert FraisDeGestion KO pour {}: {}", cb.getNumCompte(), ex.toString());
        }
    }

    /** Essaie d'appeler fg.setClient(cb.getClient()) si ces méthodes existent. */
    private void attachClientIfPossible(FraisDeGestion fg, CompteBancaire cb) {
        try {
            Object client = null;
            try { client = cb.getClass().getMethod("getClient").invoke(cb); } catch (Throwable ignored) {}
            if (client == null) return;
            for (var m : fg.getClass().getMethods()) {
                if (m.getName().equals("setClient") && m.getParameterCount() == 1) {
                    try { m.invoke(fg, client); break; } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    /** Construit un tag mensuel stable, ex: FRAIS_GESTION_2025-08 */
    private String monthlyTag() {
        return "FRAIS_GESTION_" + java.time.YearMonth.now().toString(); // YYYY-MM
    }

    /** Construit un tag “forcé” horodaté, ex: FRAIS_GESTION_FORCE_2025-08-26T12:03 */
    private String forcedTag() {
        return "FRAIS_GESTION_FORCE_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }


    @PutMapping("/{id}/desactiver")
    public ResponseEntity<Map<String, Object>> desactiverFrais(@PathVariable Long id) {
        try {
            logger.info("Désactivation du frais ID: {}", id);
            fraisService.desactiverFrais(id);
            return ResponseEntity.ok(map("success", true, "message", "Frais désactivé avec succès"));
        } catch (Exception e) {
            logger.error("Erreur lors de la désactivation du frais ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    map("success", false, "message", "Erreur lors de la désactivation: " + e.getMessage())
            );
        }
    }

    // ==================== STATISTIQUES ====================

    @GetMapping("/client/{clientId}/statistiques")
    public ResponseEntity<Map<String, Object>> obtenirStatistiquesClient(@PathVariable Long clientId) {
        try {
            Long nombreFraisActifs = fraisService.compterFraisActifsClient(clientId);
            BigDecimal montantTotalActif = fraisService.calculerMontantTotalActifClient(clientId);
            BigDecimal montantTotalFacture = fraisService.calculerMontantTotalFactureClient(clientId);

            BigDecimal moyenne = BigDecimal.ZERO;
            if (nombreFraisActifs != null && nombreFraisActifs > 0 && montantTotalActif != null) {
                moyenne = montantTotalActif.divide(BigDecimal.valueOf(nombreFraisActifs), BigDecimal.ROUND_HALF_UP);
            }

            FraisStatistiquesResponse stats = new FraisStatistiquesResponse(
                    nombreFraisActifs, montantTotalActif, montantTotalFacture, moyenne
            );
            return ResponseEntity.ok(map("success", true, "data", stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(map("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/client/{clientId}/historique")
    public ResponseEntity<Map<String, Object>> obtenirHistoriqueFacturation(@PathVariable Long clientId) {
        try {
            List<FraisDeGestion> historique = fraisService.obtenirHistoriqueFacturationClient(clientId);
            List<FraisDeGestionResponse> responses = historique.stream().map(FraisDeGestionResponse::new).collect(Collectors.toList());
            return ResponseEntity.ok(map("success", true, "data", responses, "count", responses.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(map("success", false, "message", "Erreur: " + e.getMessage()));
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
            List<FraisDeGestionResponse> responses = frais.stream().map(FraisDeGestionResponse::new).collect(Collectors.toList());
            return ResponseEntity.ok(map("success", true, "data", responses, "count", responses.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    map("success", false, "message", "Erreur lors de la recherche: " + e.getMessage())
            );
        }
    }

    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> obtenirTypesFrais() {
        try {
            return ResponseEntity.ok(map("success", true, "data", FraisDeGestion.TypeFrais.values()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    map("success", false, "message", "Erreur: " + e.getMessage())
            );
        }
    }

    @GetMapping("/periodicites")
    public ResponseEntity<Map<String, Object>> obtenirPeriodicites() {
        try {
            return ResponseEntity.ok(map("success", true, "data", FraisDeGestion.Periodicite.values()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    map("success", false, "message", "Erreur: " + e.getMessage())
            );
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

    /* ===== Helpers tolérants pour Operation (compat Java 8) ===== */

    private void setOperationDate(Operation op) {
        LocalDateTime now = LocalDateTime.now();
        try { op.getClass().getMethod("setDateOperation", LocalDateTime.class).invoke(op, now); return; } catch (Throwable ignored) {}
        try { op.getClass().getMethod("setDateOp",        LocalDateTime.class).invoke(op, now); return; } catch (Throwable ignored) {}
        try { op.getClass().getMethod("setDate",          LocalDateTime.class).invoke(op, now); return; } catch (Throwable ignored) {}
        // pas bloquant si pas de champ date
    }

    private void setOperationType(Operation op) {
        try {
            op.getClass().getMethod("setType", TypeOperation.class).invoke(op, TypeOperation.FRAIS);
            return;
        } catch (Throwable ignored) {}
        try {
            op.getClass().getMethod("setTypeOperation", TypeOperation.class).invoke(op, TypeOperation.FRAIS);
        } catch (Throwable ignored) {
            // pas bloquant si pas de champ type
        }
    }
}

