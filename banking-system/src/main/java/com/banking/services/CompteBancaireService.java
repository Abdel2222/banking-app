package com.banking.services;

import com.banking.dto.request.CreateAccountRequest;
import com.banking.entities.CompteBancaire;
import com.banking.entities.Operation;
import com.banking.entities.CarteBancaire;
import com.banking.entities.CompteEpargne;
import com.banking.entity.enums.AccountStatus;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CompteBancaireService {

    // CRUD de base
    CompteBancaire createAccount(Long clientId);
    Optional<CompteBancaire> findById(Long id);
    Optional<CompteBancaire> findByNumCompte(String numCompte);
    List<CompteBancaire> findAll();
    CompteBancaire updateAccount(Long id, CompteBancaire accountDetails);
    void deleteAccount(Long id);

    // Opérations bancaires
    Operation deposit(String numCompte, BigDecimal montant, String description);
    Operation withdraw(String numCompte, BigDecimal montant, String description);
    Operation transfer(String numCompteSource, String numCompteDestinataire, BigDecimal montant, String communication);


    // ✅ Ajout de la méthode pour le controller
    void effectuerVirement(String sourceAccount, String destinationAccount, BigDecimal montant, String description);

    // Gestion du statut
    CompteBancaire activateAccount(String numCompte);
    CompteBancaire suspendAccount(String numCompte);
    CompteBancaire closeAccount(String numCompte);

    // Carte bancaire
    CarteBancaire issueCard(String numCompte);
    CarteBancaire blockCard(String numCompte, String raison);
    CarteBancaire unblockCard(String numCompte);
    Optional<CarteBancaire> getCard(String numCompte);
     CompteEpargne convertToSavingsAccountInternal(String numCompte, BigDecimal tauxInteret);


    // Compte épargne
    CompteEpargne convertToSavingsAccount(String numCompte, BigDecimal tauxInteret);
    CompteEpargne updateSavingsRate(String numCompte, BigDecimal nouveauTaux);
    void capitalizeInterests(String numCompte);
    BigDecimal calculateInterests(String numCompte);
    void savingsDeposit(String numCompte, BigDecimal montant);   // courant -> épargne
    void savingsWithdraw(String numCompte, BigDecimal montant);  // épargne -> courant


    // Consultation
    BigDecimal getBalance(String numCompte);
    List<Operation> getAccountHistory(String numCompte, LocalDateTime debut, LocalDateTime fin);
    List<Operation> getLastOperations(String numCompte, int nombre);
    Map<String, BigDecimal> getAccountStatistics(String numCompte);

    // Recherche et filtrage
    List<CompteBancaire> getAccountsForPrincipal(Authentication authentication);
    List<CompteBancaire> findByStatus(AccountStatus status);
    List<CompteBancaire> findAccountsWithLowBalance(BigDecimal threshold);
    List<CompteBancaire> findInactiveAccounts(int daysSinceLastActivity);
    List<CompteBancaire> findAccountsNeedingAttention();


    // Validation et vérification
    boolean accountExists(String numCompte);
    boolean canPerformOperation(String numCompte, BigDecimal montant);
    boolean hasCard(String numCompte);
    boolean isSavingsAccount(String numCompte);

    // Limites et plafonds
    void setWithdrawalLimit(String numCompte, BigDecimal dailyLimit);
    void setTransferLimit(String numCompte, BigDecimal monthlyLimit);
    BigDecimal getRemainingDailyLimit(String numCompte);

    // Notifications et alertes
    void sendLowBalanceAlert(String numCompte);
    void sendStatementNotification(String numCompte);
    List<String> getPendingNotifications(String numCompte);

    CompteBancaire createForPrincipal(Authentication authentication, CreateAccountRequest req);
}