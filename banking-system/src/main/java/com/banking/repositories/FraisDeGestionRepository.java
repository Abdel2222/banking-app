package com.banking.repositories;

import com.banking.entities.Client;
import com.banking.entities.FraisDeGestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraisDeGestionRepository extends JpaRepository<FraisDeGestion, Long> {

    List<FraisDeGestion> findByClient(Client client);

    List<FraisDeGestion> findByEstActif(Boolean estActif);

    List<FraisDeGestion> findByClientAndEstActif(Client client, Boolean estActif);

    // ❌ SUPPRIMÉES — typeFrais et periodicite sont @Transient dans FraisDeGestion
    // Spring Data JPA ne peut pas faire de requête SQL sur un champ @Transient
    // findByTypeFrais(TypeFrais typeFrais)
    // findByPeriodicite(Periodicite periodicite)
    // findByClientAndTypeFrais(Client client, TypeFrais typeFrais)
}