package com.banking.repositories;

import com.banking.entities.Client;
import com.banking.entities.CompteBancaire;
import com.banking.entities.Fonds;
import com.banking.entities.Placement;
import com.banking.entity.enums.StatutPlacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlacementRepository extends JpaRepository<Placement, Long> {

    List<Placement> findByClient(Client client);

    List<Placement> findByClientId(Long clientId);

    List<Placement> findByCompteBancaire(CompteBancaire compteBancaire);

    List<Placement> findByFonds(Fonds fonds);

    List<Placement> findByStatut(StatutPlacement statut);

    List<Placement> findByClientIdAndStatut(Long clientId, StatutPlacement statut);
}