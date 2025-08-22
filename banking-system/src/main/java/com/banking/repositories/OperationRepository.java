package com.banking.repositories;

import com.banking.entities.CompteBancaire;
import com.banking.entities.Operation;
import com.banking.entity.enums.TypeOperation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OperationRepository extends JpaRepository<Operation, Long> {

    // Filtrer par type
    List<Operation> findByTypeOperation(TypeOperation typeOperation);

    // Somme des dépôts
    @Query("""
           select coalesce(sum(o.montant), 0)
           from Operation o
           where o.compteBancaire = :compte
             and o.typeOperation = com.banking.entity.enums.TypeOperation.DEPOT
           """)
    BigDecimal sumDepositsByAccount(@Param("compte") CompteBancaire compte);

    // Somme des retraits
    @Query("""
           select coalesce(sum(o.montant), 0)
           from Operation o
           where o.compteBancaire = :compte
             and o.typeOperation = com.banking.entity.enums.TypeOperation.RETRAIT
           """)
    BigDecimal sumWithdrawalsByAccount(@Param("compte") CompteBancaire compte);

    // Virements reçus par numéro de compte destinataire
    @Query("""
           select o
           from Operation o
           where o.typeOperation = com.banking.entity.enums.TypeOperation.VIREMENT
             and o.numeroCompteDestinataire = :num
           order by o.dateOperation asc
           """)
    List<Operation> findTransfersToAccount(@Param("num") String numeroCompteDestinataire);

    // Relevé entre deux dates par entité CompteBancaire
    @Query("""
           select o
           from Operation o
           where o.compteBancaire = :compte
             and o.dateOperation between :debut and :fin
           order by o.dateOperation asc
           """)
    List<Operation> findByCompteAndDateBetween(@Param("compte") CompteBancaire compte,
                                               @Param("debut") LocalDateTime debut,
                                               @Param("fin") LocalDateTime fin);

    // Dernières opérations (par date décroissante)
    @Query("""
           select o
           from Operation o
           where o.compteBancaire = :compte
           order by o.dateOperation desc
           """)
    List<Operation> findLastOperations(@Param("compte") CompteBancaire compte, Pageable pageable);

    // Toutes les opérations avant une date, par numéro de compte
    @Query("""
           select o
           from Operation o
           where o.compteBancaire.numCompte = :num
             and o.dateOperation < :limite
           order by o.dateOperation asc
           """)
    List<Operation> findAllForAccountBefore(@Param("num") String numeroCompte,
                                            @Param("limite") LocalDateTime dateLimite);

    // Toutes les opérations entre deux dates, par numéro de compte
    @Query("""
           select o
           from Operation o
           where o.compteBancaire.numCompte = :num
             and o.dateOperation between :debut and :fin
           order by o.dateOperation asc
           """)
    List<Operation> findAllForAccountBetween(@Param("num") String numeroCompte,
                                             @Param("debut") LocalDateTime debut,
                                             @Param("fin") LocalDateTime fin);
}
