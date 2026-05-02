package com.banking.repositories;

import com.banking.entities.CompteBancaire;
import com.banking.entities.Operation;
import com.banking.entity.enums.TypeOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OperationRepository extends JpaRepository<Operation, Long> {

        // Historique par compte (objet) + période
        List<Operation> findByCompteBancaireAndDateOperationBetweenOrderByDateOperationDesc(
                CompteBancaire compte, LocalDateTime start, LocalDateTime end);

        // Historique par N° de compte + période
        List<Operation> findByCompteBancaire_NumCompteAndDateOperationBetweenOrderByDateOperationDesc(
                String numCompte, LocalDateTime start, LocalDateTime end);

        // Dernières opérations paginées
        Page<Operation> findByCompteBancaireOrderByDateOperationDesc(
                CompteBancaire compte, Pageable pageable);
        long countByCompteBancaire_NumCompteAndCommentaire(String numCompte, String commentaire);

        // Filtre par type
        List<Operation> findByTypeOperation(TypeOperation type);

        // Virements reçus vers un numéro
        @Query("""
        select o from Operation o
        where o.numeroCompteDestinataire = :numCompte
          and o.typeOperation = com.banking.entity.enums.TypeOperation.VIREMENT
        order by o.dateOperation desc
    """)
        List<Operation> findTransfersToAccount(@Param("numCompte") String numCompte);

        // ====== AGRÉGATS ======

        @Query("""
        select sum(o.montant) from Operation o
        where o.compteBancaire = :compte
          and o.typeOperation = com.banking.entity.enums.TypeOperation.DEPOT
    """)
        BigDecimal sumDepositsByAccount(@Param("compte") CompteBancaire compte);

        @Query("""
        select sum(o.montant) from Operation o
        where o.compteBancaire = :compte
          and o.typeOperation = com.banking.entity.enums.TypeOperation.RETRAIT
    """)
        BigDecimal sumWithdrawalsByAccount(@Param("compte") CompteBancaire compte);

        @Query("""
        select sum(o.montant) from Operation o
        where o.compteBancaire = :compte
          and o.dateOperation between :start and :end
    """)
        BigDecimal sumAmountByAccountBetween(@Param("compte") CompteBancaire compte,
                                             @Param("start") LocalDateTime start,
                                             @Param("end")   LocalDateTime end);

        @Query("""
        select sum(o.montant) from Operation o
        where o.compteBancaire = :compte
          and o.typeOperation = :type
          and o.dateOperation between :start and :end
    """)
        BigDecimal sumByAccountAndTypeBetween(@Param("compte") CompteBancaire compte,
                                              @Param("type")   TypeOperation type,
                                              @Param("start")  LocalDateTime start,
                                              @Param("end")    LocalDateTime end);
}
