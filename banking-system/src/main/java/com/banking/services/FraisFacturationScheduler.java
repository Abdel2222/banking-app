package com.banking.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FraisFacturationScheduler {

    private static final Logger log = LoggerFactory.getLogger(FraisFacturationScheduler.class);

    @Autowired
    private FraisDeGestionService fraisService;

    /**
     * S'exécute automatiquement le 1er de chaque mois à 02h00 du matin.
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void facturationMensuelleAuto() {
        log.info("=== [CRON] Démarrage facturation mensuelle automatique ===");
        try {
            fraisService.facturationAutomatique();
            log.info("=== [CRON] Facturation mensuelle terminée avec succès ===");
        } catch (Exception e) {
            log.error("=== [CRON] Erreur facturation mensuelle", e);
        }
    }
}