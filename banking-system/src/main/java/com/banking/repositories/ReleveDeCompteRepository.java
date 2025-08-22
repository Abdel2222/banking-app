package com.banking.repositories;

import com.banking.entities.ReleveDeCompte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReleveDeCompteRepository extends JpaRepository<ReleveDeCompte, Long> {

    List<ReleveDeCompte> findByCompte_IdOrderByDateReleveDesc(Long compteId);

    List<ReleveDeCompte> findByCompte_NumCompteOrderByDateReleveDesc(String numCompte);

    Optional<ReleveDeCompte> findByCompte_IdAndAnneeAndMois(Long compteId, Integer annee, Integer mois);

    List<ReleveDeCompte> findByPdfGenere(boolean pdfGenere);
}
