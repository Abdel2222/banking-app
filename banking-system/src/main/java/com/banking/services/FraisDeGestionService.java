package com.banking.services;

import com.banking.entities.FraisDeGestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Interface pour la gestion des frais bancaires
 */
public interface FraisDeGestionService {

    // ==================== CRUD DE BASE ====================

    /**
     * Créer un nouveau frais de gestion
     * @param frais Le frais à créer
     * @return Le frais créé avec son ID
     */
    FraisDeGestion creerFrais(FraisDeGestion frais);

    /**
     * Obtenir un frais par son ID
     * @param id L'identifiant du frais
     * @return Le frais trouvé
     * @throws ResourceNotFoundException si le frais n'existe pas
     */
    FraisDeGestion obtenirFrais(Long id);

    /**
     * Obtenir tous les frais de gestion
     * @return Liste de tous les frais
     */
    List<FraisDeGestion> obtenirTousFrais();

    /**
     * Mettre à jour un frais existant
     * @param id L'identifiant du frais
     * @param fraisModifie Les nouvelles données
     * @return Le frais mis à jour
     */
    FraisDeGestion mettreAJourFrais(Long id, FraisDeGestion fraisModifie);

    /**
     * Supprimer un frais
     * @param id L'identifiant du frais
     * @throws BusinessException si le frais a déjà été facturé
     */
    void supprimerFrais(Long id);

    // ==================== GESTION PAR CLIENT ====================

    /**
     * Obtenir tous les frais d'un client
     * @param clientId L'identifiant du client
     * @return Liste des frais du client
     */
    List<FraisDeGestion> obtenirFraisClient(Long clientId);

    /**
     * Obtenir les frais actifs d'un client
     * @param clientId L'identifiant du client
     * @return Liste des frais actifs
     */
    List<FraisDeGestion> obtenirFraisActifsClient(Long clientId);

    /**
     * Obtenir les frais en cours d'un client (dans la période)
     * @param clientId L'identifiant du client
     * @return Liste des frais en cours
     */
    List<FraisDeGestion> obtenirFraisEnCoursClient(Long clientId);

    /**
     * Créer un frais pour un client spécifique
     * @param clientId L'identifiant du client
     * @param frais Le frais à créer
     * @return Le frais créé
     */
    FraisDeGestion creerFraisClient(Long clientId, FraisDeGestion frais);

    // ==================== GESTION DE LA FACTURATION ====================

    /**
     * Obtenir la liste des frais à facturer
     * @return Liste des frais éligibles à la facturation
     */
    List<FraisDeGestion> obtenirFraisAFacturer();

    /**
     * Facturer un frais spécifique
     * @param fraisId L'identifiant du frais
     */
    void facturerFrais(Long fraisId);

    /**
     * Facturer tous les frais éligibles d'un client
     * @param clientId L'identifiant du client
     */
    void facturerFraisClient(Long clientId);

    /**
     * Processus de facturation automatique (tâche planifiée)
     */
    void facturationAutomatique();

    // ==================== GESTION DU CYCLE DE VIE ====================

    /**
     * Activer un frais
     * @param fraisId L'identifiant du frais
     */
    void activerFrais(Long fraisId);

    /**
     * Désactiver un frais
     * @param fraisId L'identifiant du frais
     */
    void desactiverFrais(Long fraisId);

    /**
     * Désactiver automatiquement les frais expirés
     */
    void desactiverFraisExpires();

    // ==================== STATISTIQUES ====================

    /**
     * Calculer le montant total des frais actifs pour un client
     * @param clientId L'identifiant du client
     * @return Montant total des frais actifs
     */
    default void appliquerFraisOuverture(String numCompte) {

    }
    BigDecimal calculerMontantTotalActifClient(Long clientId);

    /**
     * Calculer le montant total facturé pour un client
     * @param clientId L'identifiant du client
     * @return Montant total facturé
     */
    BigDecimal calculerMontantTotalFactureClient(Long clientId);

    /**
     * Compter le nombre de frais actifs pour un client
     * @param clientId L'identifiant du client
     * @return Nombre de frais actifs
     */
    Long compterFraisActifsClient(Long clientId);

    // ==================== RECHERCHES AVANCÉES ====================

    /**
     * Rechercher des frais selon plusieurs critères
     * @param typeFrais Type de frais (optionnel)
     * @param periodicite Périodicité (optionnel)
     * @param estActif Statut actif (optionnel)
     * @param dateDebut Date de début minimum (optionnel)
     * @param dateFin Date de fin maximum (optionnel)
     * @return Liste des frais correspondants aux critères
     */
    List<FraisDeGestion> rechercherFrais(FraisDeGestion.TypeFrais typeFrais,
                                         FraisDeGestion.Periodicite periodicite,
                                         Boolean estActif,
                                         LocalDate dateDebut,
                                         LocalDate dateFin);

    /**
     * Obtenir l'historique de facturation d'un client
     * @param clientId L'identifiant du client
     * @return Liste des frais facturés par ordre chronologique inverse
     */
    List<FraisDeGestion> obtenirHistoriqueFacturationClient(Long clientId);
}