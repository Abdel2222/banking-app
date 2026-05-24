package com.banking.services;

import com.banking.entities.FraisDeGestion;
import com.banking.entity.enums.Periodicite;   // ✅ import corrigé
import com.banking.entity.enums.TypeFrais;      // ✅ import corrigé

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FraisDeGestionService {

    // ==================== CRUD DE BASE ====================

    FraisDeGestion creerFrais(FraisDeGestion frais);

    FraisDeGestion obtenirFrais(Long id);

    List<FraisDeGestion> obtenirTousFrais();

    FraisDeGestion mettreAJourFrais(Long id, FraisDeGestion fraisModifie);

    void supprimerFrais(Long id);

    // ==================== GESTION PAR CLIENT ====================

    List<FraisDeGestion> obtenirFraisClient(Long clientId);

    List<FraisDeGestion> obtenirFraisActifsClient(Long clientId);

    List<FraisDeGestion> obtenirFraisEnCoursClient(Long clientId);

    FraisDeGestion creerFraisClient(Long clientId, FraisDeGestion frais);

    // ==================== GESTION DE LA FACTURATION ====================

    List<FraisDeGestion> obtenirFraisAFacturer();

    void facturerFrais(Long fraisId);

    void facturerFraisClient(Long clientId);

    void facturationAutomatique();

    // ==================== GESTION DU CYCLE DE VIE ====================

    void activerFrais(Long fraisId);

    void desactiverFrais(Long fraisId);

    void desactiverFraisExpires();

    // ==================== STATISTIQUES ====================

    default void appliquerFraisOuverture(String numCompte) {}

    BigDecimal calculerMontantTotalActifClient(Long clientId);

    BigDecimal calculerMontantTotalFactureClient(Long clientId);

    Long compterFraisActifsClient(Long clientId);

    // ==================== RECHERCHES AVANCÉES ====================

    List<FraisDeGestion> rechercherFrais(TypeFrais typeFrais,        // ✅ corrigé
                                         Periodicite periodicite,    // ✅ corrigé
                                         Boolean estActif,
                                         LocalDate dateDebut,
                                         LocalDate dateFin);

    List<FraisDeGestion> obtenirHistoriqueFacturationClient(Long clientId);
}