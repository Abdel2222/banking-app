package com.banking.repositories;

import com.banking.entities.Client;
import com.banking.entities.FraisDeGestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository simple pour FraisDeGestion
 * Version de base qui fonctionne immédiatement
 */
@Repository
public interface FraisDeGestionRepository extends JpaRepository<FraisDeGestion, Long> {

    /**
     * Recherche par client
     */
    List<FraisDeGestion> findByClient(Client client);

    /**
     * Recherche par statut actif
     */
    List<FraisDeGestion> findByEstActif(Boolean estActif);

    /**
     * Recherche par type de frais
     */
    List<FraisDeGestion> findByTypeFrais(FraisDeGestion.TypeFrais typeFrais);

    /**
     * Recherche par périodicité
     */
    List<FraisDeGestion> findByPeriodicite(FraisDeGestion.Periodicite periodicite);

    /**
     * Frais par client et type
     */
    List<FraisDeGestion> findByClientAndTypeFrais(Client client, FraisDeGestion.TypeFrais typeFrais);

    /**
     * Frais actifs par client
     */
    List<FraisDeGestion> findByClientAndEstActif(Client client, Boolean estActif);
}