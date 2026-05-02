package com.banking.repositories;

import com.banking.entities.CompteBancaire;
import com.banking.entities.Client;
import com.banking.entity.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompteBancaireRepository extends JpaRepository<CompteBancaire, Long> {

    // --- Identifiants / existence ---
    Optional<CompteBancaire> findByNumCompte(String numCompte);
    boolean existsByNumCompte(String numCompte);

    // ✅ Idempotence par client
    boolean existsByClientId(Long clientId);
    Optional<CompteBancaire> findFirstByClientIdOrderByIdAsc(Long clientId);

    // --- Par client ---
    List<CompteBancaire> findByClientEmail(String email);
    List<CompteBancaire> findByClient(Client client);
    List<CompteBancaire> findByClientId(Long clientId);

    // --- Statut ---
    List<CompteBancaire> findByStatus(AccountStatus status);
    Optional<CompteBancaire> findByIdAndStatus(Long id, AccountStatus status);

    // Garde si tu veux l’appel direct via JPQL, sinon utilise findByStatus(AccountStatus.ACTIVATED)
    @Query("SELECT c FROM CompteBancaire c WHERE c.status = com.banking.entity.enums.AccountStatus.ACTIVATED")
    List<CompteBancaire> findActiveAccounts();

    // --- Bornes / dates / montants ---
    List<CompteBancaire> findByBalanceGreaterThan(BigDecimal balance);
    List<CompteBancaire> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // --- Comptes ayant une carte (nécessite l’entité CarteBancaire mappée vers CompteBancaire) ---
    @Query("""
           SELECT c
           FROM CarteBancaire cb
           JOIN cb.compteBancaire c
           """)
    List<CompteBancaire> findAccountsWithCard();

    // --- Comptes épargne (nécessite l’entité CompteEpargne mappée vers CompteBancaire) ---
    @Query("""
           SELECT c
           FROM CompteEpargne e
           JOIN e.compteBancaire c
           """)
    List<CompteBancaire> findSavingsAccounts();

    // --- Stats ---
    @Query("SELECT COALESCE(SUM(c.balance), 0) FROM CompteBancaire c")
    BigDecimal getTotalBalance();

    // AVG => Double (pour éviter les cast)
    @Query("SELECT COALESCE(AVG(c.balance), 0) FROM CompteBancaire c")
    Double getAverageBalance();

    @Query("SELECT c FROM CompteBancaire c WHERE c.balance < 0")
    List<CompteBancaire> findOverdrawnAccounts();

    @Query("""
           SELECT c FROM CompteBancaire c
           WHERE c.status <> com.banking.entity.enums.AccountStatus.ACTIVATED
             AND c.createdAt < :date
           """)
    List<CompteBancaire> findInactiveAccountsOlderThan(@Param("date") LocalDateTime date);

}
