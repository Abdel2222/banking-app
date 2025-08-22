package com.banking.repositories;

import com.banking.entities.DemandeCarteBancaire;
import com.banking.entity.enums.CardRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeCarteBancaireRepository extends JpaRepository<DemandeCarteBancaire, Long> {

    // ✅ pointe sur le champ 'compte' (ManyToOne) de l'entité
    boolean existsByCompte_IdAndStatus(Long compteId, CardRequestStatus status);

    // ✅ pointe sur le champ 'client' (ManyToOne) de l'entité
    List<DemandeCarteBancaire> findByClient_Id(Long clientId);
}
