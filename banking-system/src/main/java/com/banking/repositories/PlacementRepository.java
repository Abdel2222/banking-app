package com.banking.repositories;

import com.banking.entities.Placement;
import com.banking.entity.enums.StatutPlacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PlacementRepository extends JpaRepository<Placement, Long> {

    List<Placement> findByClientId(Long clientId);

    List<Placement> findByClientIdAndStatut(Long clientId, StatutPlacement statut);

    /** Placements ACTIF dont la date de clôture est dépassée — utilisé par le job auto. */
    List<Placement> findByStatutAndDateClotureBefore(StatutPlacement statut, LocalDateTime date);

    /** Placements ACTIF d'un client donné dont l'échéance est dépassée. */
    List<Placement> findByClientIdAndStatutAndDateClotureBefore(
            Long clientId, StatutPlacement statut, LocalDateTime date);
}