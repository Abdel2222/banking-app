// com/banking/repositories/CompteEpargneRepository.java
package com.banking.repositories;

import com.banking.entities.CompteBancaire;
import com.banking.entities.CompteEpargne;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompteEpargneRepository extends JpaRepository<CompteEpargne, Long> {

    // <-- pour SavingsNumberService
    boolean existsByNumCompteEpargne(String numCompteEpargne);

    // pratiques ailleurs
    Optional<CompteEpargne> findByNumCompteEpargne(String numCompteEpargne);
    Optional<CompteEpargne> findByCompteBancaire(CompteBancaire compteBancaire);
    Optional<CompteEpargne> findByCompteBancaire_Id(Long compteBancaireId);
    Optional<CompteEpargne> findByCompteBancaire_NumCompte(String numCompteBancaire);

}

