package com.banking.repositories;

import com.banking.entities.DemandeCarteBancaire;
import com.banking.entity.enums.CardRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DemandeCarteBancaireRepository extends JpaRepository<DemandeCarteBancaire, Long> {

    // Demandes par compte/client
    boolean existsByCompte_IdAndStatus(Long compteId, CardRequestStatus status);
    List<DemandeCarteBancaire> findByClient_Id(Long clientId);

    // ✅ Helpers pour nos règles métier
    boolean existsByCompte_IdAndStatusIn(Long compteId, Collection<CardRequestStatus> statuses);
    boolean existsByClient_IdAndStatusIn(Long clientId, Collection<CardRequestStatus> statuses);

    // ✅ Dernière demande pour ce compte (peu importe le statut)
    Optional<DemandeCarteBancaire> findTopByCompte_NumCompteOrderByRequestedAtDesc(String numCompte);

    // (optionnel) Dernière demande à un statut précis (ex: APPROVED)
    Optional<DemandeCarteBancaire> findTopByCompte_NumCompteAndStatusOrderByRequestedAtDesc(
            String numCompte, CardRequestStatus status
    );
}
