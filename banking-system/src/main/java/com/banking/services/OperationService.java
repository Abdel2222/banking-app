package com.banking.services;

import com.banking.entities.Operation;
import com.banking.entity.enums.TypeOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OperationService {

    // CRUD de base
    Operation createOperation(String numCompte, BigDecimal montant, TypeOperation type, String description);
    Optional<Operation> findById(Long id);
    List<Operation> findAll();
    Page<Operation> findAllPaginated(Pageable pageable);

    // Opérations bancaires principales (déléguées au service compte)
    Operation executeDeposit(String numCompte, BigDecimal montant, String description);
    Operation executeWithdrawal(String numCompte, BigDecimal montant, String description);
    Operation executeTransfer(String fromAccount, String toAccount, BigDecimal montant, String communication);
    Operation executeCardBlock(String numCompte, String raison);

    // Recherche et filtrage
    List<Operation> findByAccount(String numCompte);
    Page<Operation> findByAccountPaginated(String numCompte, Pageable pageable);
    List<Operation> findByType(TypeOperation type);
    List<Operation> findByDateRange(LocalDateTime start, LocalDateTime end);
    List<Operation> findByAccountAndDateRange(String numCompte, LocalDateTime start, LocalDateTime end);
    List<Operation> getOperationsByAccountNumber(String accountNumber);

    // Recherche avancée
    List<Operation> findLargeTransactions(BigDecimal minAmount);
    List<Operation> findSuspiciousTransactions();
    List<Operation> findTransfersToAccount(String numCompte);
    List<Operation> findTransfersFromAccount(String numCompte);
    List<Operation> findRecurringTransactions(String numCompte);

    // Statistiques et rapports
    Map<TypeOperation, BigDecimal> getStatisticsByType(LocalDateTime start, LocalDateTime end);
    BigDecimal getTotalDeposits(String numCompte, LocalDateTime start, LocalDateTime end);
    BigDecimal getTotalWithdrawals(String numCompte, LocalDateTime start, LocalDateTime end);
    BigDecimal getTotalTransfers(String numCompte, LocalDateTime start, LocalDateTime end);
    Map<String, Object> getMonthlyReport(String numCompte, int month, int year);

    // Validation et vérification
    boolean validateOperation(Operation operation);
    boolean canExecuteOperation(String numCompte, BigDecimal montant, TypeOperation type);
    boolean isDuplicateOperation(Operation operation);

    // Annulation et correction (si besoin)
    Operation cancelOperation(Long operationId, String raison);
    Operation reverseOperation(Long operationId);
    Operation correctOperation(Long operationId, BigDecimal newAmount, String raison);

    // Export & reçu
    byte[] exportOperationsToPdf(String numCompte, LocalDateTime start, LocalDateTime end);
    byte[] exportOperationsToExcel(String numCompte, LocalDateTime start, LocalDateTime end);
    String generateOperationReceipt(Long operationId);
}
