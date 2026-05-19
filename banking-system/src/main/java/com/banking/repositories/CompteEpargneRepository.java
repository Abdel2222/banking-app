package com.banking.repositories;

import com.banking.entities.CompteEpargne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompteEpargneRepository extends JpaRepository<CompteEpargne, Long> {

    // ✅ Chercher par numCompte (hérité de CompteBancaire)
    Optional<CompteEpargne> findByNumCompte(String numCompte);

    // ✅ Vérifier existence par numCompte
    boolean existsByNumCompte(String numCompte);

    // ✅ Chercher par id du client (hérité de CompteBancaire -> client)
    Optional<CompteEpargne> findByClient_Id(Long clientId);

    // ✅ Chercher tous les comptes épargne d'un client
    java.util.List<CompteEpargne> findAllByClient_Id(Long clientId);

    // ✅ Chercher par numCompte du client
    @Query("SELECT ce FROM CompteEpargne ce WHERE ce.client.id = :clientId")
    Optional<CompteEpargne> findByClientId(@Param("clientId") Long clientId);
}