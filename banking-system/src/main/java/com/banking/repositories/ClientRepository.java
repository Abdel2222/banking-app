package com.banking.repositories;

import com.banking.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    // Recherche par email
    Optional<Client> findByEmail(String email);

    // Vérifier si un email existe
    boolean existsByEmail(String email);

    // Recherche par nom complet
    @Query("SELECT c FROM Client c WHERE LOWER(c.nom) LIKE LOWER(CONCAT('%', :nom, '%')) " +
            "OR LOWER(c.prenom) LIKE LOWER(CONCAT('%', :prenom, '%'))")
    List<Client> findByNomOrPrenom(@Param("nom") String nom, @Param("prenom") String prenom);

    // Clients avec au moins un compte actif
    @Query("SELECT DISTINCT c FROM Client c JOIN c.comptes cb WHERE cb.status = 'ACTIVATED'")
    List<Client> findClientsWithActiveAccounts();

    // Clients avec solde total supérieur à un montant
    @Query("SELECT c FROM Client c WHERE " +
            "(SELECT SUM(cb.balance) FROM CompteBancaire cb WHERE cb.client = c) > :montant")
    List<Client> findClientsBySoldeTotalGreaterThan(@Param("montant") BigDecimal montant);

    // Clients sans comptes
    @Query("SELECT c FROM Client c WHERE c.comptes IS EMPTY")
    List<Client> findClientsWithoutAccounts();

    // Clients avec frais impayés
    @Query("SELECT DISTINCT c FROM Client c JOIN c.fraisDeGestion f " +
            "WHERE f.estActif = true AND f.derniereFacturation IS NULL")
    List<Client> findClientsWithUnpaidFees();

    // Nombre de clients actifs
    @Query("SELECT COUNT(DISTINCT c) FROM Client c JOIN c.comptes cb WHERE cb.status = 'ACTIVATED'")
    Long countActiveClients();
}