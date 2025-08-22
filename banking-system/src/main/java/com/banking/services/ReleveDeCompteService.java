package com.banking.services;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ReleveDeCompteService {

    /**
     * Génère un relevé PDF pour un compte et une période (dates incluses).
     */
    byte[] buildStatement(String numCompte, LocalDate start, LocalDate end);

    /**
     * Génère un relevé PDF mensuel.
     */
    byte[] buildMonthlyStatement(String numCompte, int year, int month);

    /**
     * Génère un relevé en TEXTE brut (plain text) pour une période précise.
     * Cette signature correspond à l’appel du contrôleur:
     * generatePlainTextStatement(String, LocalDateTime, LocalDateTime)
     */
    String generatePlainTextStatement(String numCompte, LocalDateTime from, LocalDateTime to);
}
