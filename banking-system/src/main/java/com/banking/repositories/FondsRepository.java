package com.banking.repositories;

import com.banking.entities.Fonds;
import com.banking.entity.enums.NiveauRisque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FondsRepository extends JpaRepository<Fonds, Long> {

    Optional<Fonds> findByCodeIdentification(String codeIdentification);

    boolean existsByCodeIdentification(String codeIdentification);

}