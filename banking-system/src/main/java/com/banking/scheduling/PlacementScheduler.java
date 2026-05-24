package com.banking.scheduling;

import com.banking.entities.CompteBancaire;
import com.banking.entities.Operation;
import com.banking.entities.Placement;
import com.banking.entity.enums.StatutPlacement;
import com.banking.entity.enums.TypeOperation;
import com.banking.repositories.CompteBancaireRepository;
import com.banking.repositories.OperationRepository;
import com.banking.repositories.PlacementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Job planifié qui clôture automatiquement les placements arrivés à échéance.
 *
 * Logique : pour chaque placement ACTIF dont date_cloture < NOW :
 *   - statut → CLOTURE
 *   - date_sortie = NOW
 *   - le compte est crédité de (montant + gain prévu)
 *   - une opération CLOTURE_PLACEMENT est tracée
 */
@Component
public class PlacementScheduler {

    private static final Logger log = LoggerFactory.getLogger(PlacementScheduler.class);

    private final PlacementRepository placementRepository;
    private final CompteBancaireRepository compteBancaireRepository;
    private final OperationRepository operationRepository;

    public PlacementScheduler(
            PlacementRepository placementRepository,
            CompteBancaireRepository compteBancaireRepository,
            OperationRepository operationRepository
    ) {
        this.placementRepository      = placementRepository;
        this.compteBancaireRepository = compteBancaireRepository;
        this.operationRepository      = operationRepository;
    }

    /**
     * S'exécute toutes les nuits à 02:00.
     * cron = "seconde minute heure jour mois jourSemaine"
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cloturerPlacementsEchus() {
        LocalDateTime maintenant = LocalDateTime.now();
        log.info("🔄 [JOB] Démarrage de la clôture automatique des placements à {}", maintenant);

        List<Placement> echus = placementRepository
                .findByStatutAndDateClotureBefore(StatutPlacement.ACTIF, maintenant);

        if (echus.isEmpty()) {
            log.info("✅ [JOB] Aucun placement à clôturer.");
            return;
        }

        log.info("📦 [JOB] {} placement(s) à clôturer.", echus.size());

        int succes = 0;
        int erreurs = 0;

        for (Placement placement : echus) {
            try {
                cloturerPlacement(placement);
                succes++;
            } catch (Exception e) {
                erreurs++;
                log.error("❌ [JOB] Erreur clôture placement {} : {}",
                        placement.getId(), e.getMessage(), e);
            }
        }

        log.info("✨ [JOB] Terminé — Succès : {}, Erreurs : {}", succes, erreurs);
    }

    /**
     * Clôture un placement unique :
     * - crédite le compte (montant + gain prévu)
     * - met le statut à CLOTURE et date_sortie à NOW
     * - trace une opération CLOTURE_PLACEMENT
     */
    private void cloturerPlacement(Placement placement) {
        CompteBancaire compte = placement.getCompteBancaire();
        BigDecimal retour = placement.getValeurEstimee(); // montant + gain prévu

        // Créditer le compte
        compte.setBalance(compte.getBalance().add(retour));
        compteBancaireRepository.save(compte);

        // Mettre à jour le placement (statut + dateSortie)
        placement.cloturer();
        placementRepository.save(placement);

        // Tracer l'opération
        String nomFonds = placement.getFonds() != null ? placement.getFonds().getNomFonds() : "—";
        Operation op = new Operation(
                compte,
                retour,
                TypeOperation.CLOTURE_PLACEMENT,
                "Clôture automatique du placement " + nomFonds + " (capital + gain)"
        );
        op.setCommunication("CLOTURE_PLACEMENT_AUTO");
        operationRepository.save(op);

        log.info("✅ Placement #{} ({}) clôturé — restitué : {} € au compte {}",
                placement.getId(), nomFonds, retour, compte.getNumCompte());
    }
}
