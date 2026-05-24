package com.banking.repositories;

import com.banking.entities.CompteEpargne;
import com.banking.entity.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompteEpargneRepository extends JpaRepository<CompteEpargne, Long> {

    // Chercher par numCompte (hérité de CompteBancaire)
    Optional<CompteEpargne> findByNumCompte(String numCompte);

    // Vérifier existence par numCompte
    boolean existsByNumCompte(String numCompte);

    // Chercher par id du client
    Optional<CompteEpargne> findByClient_Id(Long clientId);

    // Chercher tous les comptes épargne d'un client
    List<CompteEpargne> findAllByClient_Id(Long clientId);

    // Chercher par clientId via JPQL
    @Query("SELECT ce FROM CompteEpargne ce WHERE ce.client.id = :clientId")
    Optional<CompteEpargne> findByClientId(@Param("clientId") Long clientId);

    // ✅ AJOUT : tous les comptes épargne par statut (pour appliquerTauxATous)
    List<CompteEpargne> findByStatus(AccountStatus status);

    // ✅ AJOUT : tous les comptes épargne ACTIVATED avec leurs intérêts chargés
    @Query("SELECT ce FROM CompteEpargne ce LEFT JOIN FETCH ce.interets WHERE ce.status = :status")
    List<CompteEpargne> findByStatusWithInterets(@Param("status") AccountStatus status);
}