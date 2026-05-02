package com.banking.repositories;

import com.banking.entities.ReleveDeCompte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReleveDeCompteRepository extends JpaRepository<ReleveDeCompte, Long> {

    List<ReleveDeCompte> findByNumCompteAndDateOperationBetweenOrderByDateOperationAsc(
            String numCompte, LocalDateTime start, LocalDateTime end
    );

    List<ReleveDeCompte> findByNumCompteOrderByDateOperationDesc(String numCompte);
}
