package com.banking.repositories;

import com.banking.entities.CarteBancaire;
import com.banking.entities.CompteBancaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CarteBancaireRepository extends JpaRepository<CarteBancaire, Long> {

    // Par id de compte (association -> _Id)
    Optional<CarteBancaire> findByCompteBancaire_Id(Long compteId);

    // Existe déjà une carte pour ce compte ?
    boolean existsByCompteBancaire_Id(Long compteId);

    // Par association directe
    Optional<CarteBancaire> findByCompteBancaire(CompteBancaire compteBancaire);

    // Par numéro de carte
    Optional<CarteBancaire> findByNumeroCarte(String numeroCarte);

    // Cartes actives/inactives
    List<CarteBancaire> findByEstActive(Boolean estActive);

    // Cartes expirées
    @Query("SELECT c FROM CarteBancaire c WHERE c.dateExpiration < :date")
    List<CarteBancaire> findExpiredCards(@Param("date") LocalDate date);

    // Cartes qui vont expirer bientôt
    @Query("SELECT c FROM CarteBancaire c WHERE c.dateExpiration BETWEEN :now AND :future")
    List<CarteBancaire> findCardsExpiringSoon(@Param("now") LocalDate now,
                                              @Param("future") LocalDate future);

    // (optionnel) Lister par client
    List<CarteBancaire> findByCompteBancaire_Client_Id(Long clientId);
}
