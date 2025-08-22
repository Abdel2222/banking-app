package com.banking.repositories;

import com.banking.entities.CompteBancaire;
import com.banking.entities.CompteEpargne;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompteEpargneRepository extends JpaRepository<CompteEpargne, Long> {
    Optional<CompteEpargne> findByCompteBancaire(CompteBancaire compte);
}
