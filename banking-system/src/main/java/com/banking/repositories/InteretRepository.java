package com.banking.repositories;

import com.banking.entities.CompteEpargne;
import com.banking.entities.Interet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InteretRepository extends JpaRepository<Interet, Long> {

    List<Interet> findByCompteEpargne(CompteEpargne compteEpargne);

    // Dernier intérêt actif (pas encore capitalisé)
    Optional<Interet> findTopByCompteEpargneOrderByDateDebutDesc(CompteEpargne compteEpargne);

    // Par numCompte — utilisé par le controller historique
    List<Interet> findByCompteEpargne_NumCompteOrderByDateDebutDesc(String numCompte);

    // Alias sans order (compatibilité)
    List<Interet> findByCompteEpargne_NumCompte(String numCompte);

    // Intérêts non encore capitalisés (dateCapitalisation IS NULL)
    @Query("SELECT i FROM Interet i WHERE i.compteEpargne.numCompte = :numCompte AND i.dateCapitalisation IS NULL")
    Optional<Interet> findInteretActif(@Param("numCompte") String numCompte);

    // Tous les intérêts non capitalisés (pour capitaliser-tous)
    @Query("SELECT i FROM Interet i WHERE i.dateCapitalisation IS NULL")
    List<Interet> findAllNonCapitalises();
}
