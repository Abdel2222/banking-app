package com.banking.repositories;

import com.banking.entities.CompteEpargne;
import com.banking.entities.Interet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InteretRepository extends JpaRepository<Interet, Long> {

    List<Interet> findByCompteEpargne(CompteEpargne compteEpargne);

    // Dernier intérêt actif (pas encore capitalisé)
    Optional<Interet> findTopByCompteEpargneOrderByDateDebutDesc(CompteEpargne compteEpargne);

    List<Interet> findByCompteEpargne_NumCompte(String numCompte);
}
