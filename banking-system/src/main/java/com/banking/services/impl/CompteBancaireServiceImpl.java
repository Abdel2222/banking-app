package com.banking.services.impl;

import com.banking.dto.request.CreateAccountRequest;
import com.banking.entities.*;
import com.banking.entity.enums.AccountStatus;
import com.banking.entity.enums.TypeOperation;
import com.banking.exceptions.ResourceNotFoundException;
import com.banking.repositories.*;
import com.banking.services.CompteBancaireService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class CompteBancaireServiceImpl implements CompteBancaireService {

    @Autowired private CompteBancaireRepository compteBancaireRepository;
    @Autowired private ClientRepository        clientRepository;
    @Autowired private OperationRepository     operationRepository;
    @Autowired private CarteBancaireRepository carteBancaireRepository;
    @Autowired private CompteEpargneRepository compteEpargneRepository;

    /* ===================== Utilitaires ===================== */

    private void assertMontantPositif(BigDecimal montant) {
        if (montant == null || montant.signum() <= 0) {
            throw new IllegalArgumentException("Le montant doit être strictement positif");
        }
    }

    private CompteBancaire getCompteOrThrowByNum(String numCompte) {
        return compteBancaireRepository.findByNumCompte(numCompte)
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé: " + numCompte));
    }

    private CompteBancaire getCompteOrThrowById(Long id) {
        return compteBancaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé (id=" + id + ")"));
    }

    private void assertActif(CompteBancaire c) {
        if (!c.isActive()) {
            throw new IllegalStateException("Le compte n'est pas actif");
        }
    }

    /* ===================== CRUD / gestion compte ===================== */

    @Override
    public CompteBancaire createAccount(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));

        // Idempotent : si un compte existe déjà, renvoyer le premier
        if (compteBancaireRepository.existsByClientId(client.getId())) {
            return compteBancaireRepository.findFirstByClientIdOrderByIdAsc(client.getId())
                    .orElseThrow();
        }

        String numCompte = generateAccountNumber();
        CompteBancaire compte = new CompteBancaire(numCompte, client);
        return compteBancaireRepository.save(compte);
    }

    @Override @Transactional(readOnly = true)
    public Optional<CompteBancaire> findById(Long id) { return compteBancaireRepository.findById(id); }

    @Override @Transactional(readOnly = true)
    public Optional<CompteBancaire> findByNumCompte(String numCompte) { return compteBancaireRepository.findByNumCompte(numCompte); }

    @Override @Transactional(readOnly = true)
    public List<CompteBancaire> findAll() { return compteBancaireRepository.findAll(); }

    @Override
    public CompteBancaire updateAccount(Long id, CompteBancaire accountDetails) {
        CompteBancaire compte = getCompteOrThrowById(id);
        compte.setStatus(accountDetails.getStatus());
        return compteBancaireRepository.save(compte);
    }

    @Override
    public void deleteAccount(Long id) {
        CompteBancaire compte = getCompteOrThrowById(id);
        if (compte.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Impossible de supprimer un compte avec un solde non nul");
        }
        compteBancaireRepository.delete(compte);
    }

    /* ===================== Opérations simples ===================== */

    @Override
    public Operation deposit(String numCompte, BigDecimal montant, String description) {
        assertMontantPositif(montant);
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        assertActif(compte);

        compte.crediter(montant);
        compteBancaireRepository.save(compte);

        Operation operation = new Operation(
                compte,
                montant,
                TypeOperation.DEPOT,
                (description != null && !description.isBlank()) ? description : "Dépôt"
        );
        return operationRepository.save(operation);
    }

    @Override
    public Operation withdraw(String numCompte, BigDecimal montant, String description) {
        assertMontantPositif(montant);
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        assertActif(compte);

        compte.debiter(montant);
        compteBancaireRepository.save(compte);

        Operation operation = new Operation(
                compte,
                montant,
                TypeOperation.RETRAIT,
                (description != null && !description.isBlank()) ? description : "Retrait"
        );
        return operationRepository.save(operation);
    }

    /* ===================== Virement (atomique) ===================== */

    @Override
    public Operation transfer(String numCompteSource, String numCompteDestinataire,
                              BigDecimal montant, String communication) {
        assertMontantPositif(montant);
        if (Objects.equals(numCompteSource, numCompteDestinataire)) {
            throw new IllegalArgumentException("Source et destinataire ne peuvent pas être identiques");
        }

        CompteBancaire source = getCompteOrThrowByNum(numCompteSource);
        CompteBancaire dest   = getCompteOrThrowByNum(numCompteDestinataire);
        assertActif(source);
        assertActif(dest);

        source.debiter(montant);
        dest.crediter(montant);
        compteBancaireRepository.save(source);
        compteBancaireRepository.save(dest);

        // Journal côté source
        Operation oSrc = new Operation(source, montant, TypeOperation.VIREMENT);
        oSrc.setNumeroCompteDestinataire(numCompteDestinataire);
        oSrc.setCommunication(communication);
        oSrc.setNomTitulaireDestinataire(dest.getClient().getNomComplet());

        // Journal côté dest (réception)
        Operation oDst = new Operation(dest, montant, TypeOperation.VIREMENT);
        oDst.setNumeroCompteDestinataire(numCompteDestinataire);
        oDst.setCommunication("Réception: " + (communication != null ? communication : ""));
        oDst.setNomTitulaireDestinataire(dest.getClient().getNomComplet());

        operationRepository.save(oSrc);
        operationRepository.save(oDst);

        return oSrc; // par convention
    }

    /* ===================== Changement d'état du compte ===================== */

    @Override
    public CompteBancaire activateAccount(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        compte.activate();
        return compteBancaireRepository.save(compte);
    }

    @Override
    public CompteBancaire suspendAccount(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        compte.block();
        return compteBancaireRepository.save(compte);
    }

    /* ====== Création “pour moi” (utilisateur connecté) ====== */
    @Override
    public CompteBancaire createForPrincipal(Authentication authentication, CreateAccountRequest req) {
        if (authentication == null || authentication.getName() == null)
            throw new IllegalArgumentException("Utilisateur non authentifié");

        String email = authentication.getName();
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Client introuvable: " + email));

        // Idempotent : renvoyer un compte existant si présent
        if (compteBancaireRepository.existsByClientId(client.getId())) {
            return compteBancaireRepository.findFirstByClientIdOrderByIdAsc(client.getId())
                    .orElseThrow();
        }

        CompteBancaire compte = CompteBancaire.createNew(
                client,
                (req != null && req.getIntitule() != null && !req.getIntitule().isBlank()) ? req.getIntitule() : "Compte courant",
                (req != null && req.getDevise()   != null && !req.getDevise().isBlank())   ? req.getDevise()   : "EUR"
        );
        return compteBancaireRepository.save(compte);
    }

    @Override
    public CompteBancaire closeAccount(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        compte.close();
        return compteBancaireRepository.save(compte);
    }

    /* ===================== Cartes ===================== */

    @Override
    public CarteBancaire issueCard(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);

        if (carteBancaireRepository.findByCompteBancaire(compte).isPresent()) {
            throw new IllegalStateException("Ce compte possède déjà une carte");
        }

        String numeroCarte   = generateCardNumber();
        String cvv           = generateCVV();
        LocalDate expiration = LocalDate.now().plusYears(3);

        CarteBancaire carte = new CarteBancaire(numeroCarte, expiration, cvv, compte);
        if (carte.getPlafondJournalier() == null) carte.setPlafondJournalier(500d);
        if (carte.getPlafondMensuel()   == null) carte.setPlafondMensuel(2000d);

        return carteBancaireRepository.save(carte);
    }

    @Override
    public CarteBancaire blockCard(String numCompte, String raison) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);

        CarteBancaire carte = carteBancaireRepository.findByCompteBancaire(compte)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune carte associée à ce compte"));

        carte.bloquer();
        carteBancaireRepository.save(carte);

        Operation op = new Operation(
                compte,
                BigDecimal.ZERO,
                TypeOperation.BLOCAGE_CARTE,
                (raison != null && !raison.isBlank()) ? raison : "Blocage de carte"
        );
        operationRepository.save(op);

        return carte;
    }

    @Override
    public CarteBancaire unblockCard(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);

        CarteBancaire carte = carteBancaireRepository.findByCompteBancaire(compte)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune carte associée à ce compte"));

        carte.debloquer();
        return carteBancaireRepository.save(carte);
    }

    @Override @Transactional(readOnly = true)
    public Optional<CarteBancaire> getCard(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        return carteBancaireRepository.findByCompteBancaire(compte);
    }

    @Override
    public CompteEpargne convertToSavingsAccountInternal(String numCompte, BigDecimal tauxInteret) {
        return null;
    }

    /* ===================== Épargne ===================== */

    @Override
    public CompteEpargne convertToSavingsAccount(String numCompte, BigDecimal tauxInteret) {
        if (tauxInteret == null || tauxInteret.signum() <= 0) {
            throw new IllegalArgumentException("Le taux d'intérêt doit être positif");
        }
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);

        if (compteEpargneRepository.findByCompteBancaire(compte).isPresent()) {
            throw new IllegalStateException("Ce compte est déjà un compte épargne");
        }

        CompteEpargne epargne = new CompteEpargne();
        epargne.setCompteBancaire(compte);
        epargne.setTauxInteret(tauxInteret);
        return compteEpargneRepository.save(epargne);
    }

    @Override
    public CompteEpargne updateSavingsRate(String numCompte, BigDecimal nouveauTaux) {
        if (nouveauTaux == null || nouveauTaux.signum() <= 0) {
            throw new IllegalArgumentException("Le nouveau taux d'intérêt doit être positif");
        }
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        CompteEpargne epargne = compteEpargneRepository.findByCompteBancaire(compte)
                .orElseThrow(() -> new ResourceNotFoundException("Ce n'est pas un compte épargne"));
        epargne.setTauxInteret(nouveauTaux);
        return compteEpargneRepository.save(epargne);
    }

    @Override
    public void capitalizeInterests(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        CompteEpargne epargne = compteEpargneRepository.findByCompteBancaire(compte)
                .orElseThrow(() -> new ResourceNotFoundException("Ce n'est pas un compte épargne"));
        if (epargne.doitCapitaliser()) {
            epargne.capitaliserInterets();
            compteEpargneRepository.save(epargne);
            compteBancaireRepository.save(compte);
        }
    }

    @Override @Transactional(readOnly = true)
    public BigDecimal calculateInterests(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        CompteEpargne epargne = compteEpargneRepository.findByCompteBancaire(compte)
                .orElseThrow(() -> new ResourceNotFoundException("Ce n'est pas un compte épargne"));
        return epargne.calculerInteretsAnnuels();
    }

    /* ===================== Requêtes / stats ===================== */

    @Override @Transactional(readOnly = true)
    public BigDecimal getBalance(String numCompte) {
        return getCompteOrThrowByNum(numCompte).getBalance();
    }

    @Override @Transactional(readOnly = true)
    public List<Operation> getAccountHistory(String numCompte, LocalDateTime debut, LocalDateTime fin) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        return operationRepository.findByCompteAndDateBetween(compte, debut, fin);
    }

    @Override @Transactional(readOnly = true)
    public List<Operation> getLastOperations(String numCompte, int nombre) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        return operationRepository.findLastOperations(compte, PageRequest.of(0, nombre));
    }

    @Override @Transactional(readOnly = true)
    public Map<String, BigDecimal> getAccountStatistics(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);

        Map<String, BigDecimal> stats = new HashMap<>();
        stats.put("solde", compte.getBalance());

        BigDecimal depots   = Optional.ofNullable(operationRepository.sumDepositsByAccount(compte)).orElse(BigDecimal.ZERO);
        BigDecimal retraits = Optional.ofNullable(operationRepository.sumWithdrawalsByAccount(compte)).orElse(BigDecimal.ZERO);
        stats.put("totalDepots", depots);
        stats.put("totalRetraits", retraits);

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Operation> recentOps = operationRepository.findByCompteAndDateBetween(
                compte, thirtyDaysAgo, LocalDateTime.now());

        BigDecimal totalMouvements = recentOps.stream()
                .map(Operation::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("mouvementsMensuels", totalMouvements);
        return stats;
    }

    @Override @Transactional(readOnly = true)
    public List<CompteBancaire> findByStatus(AccountStatus status) {
        return compteBancaireRepository.findByStatus(status);
    }

    @Override @Transactional(readOnly = true)
    public List<CompteBancaire> findAccountsWithLowBalance(BigDecimal threshold) {
        return compteBancaireRepository.findAll().stream()
                .filter(compte -> compte.getBalance().compareTo(threshold) < 0)
                .toList();
    }

    @Override @Transactional(readOnly = true)
    public List<CompteBancaire> findInactiveAccounts(int daysSinceLastActivity) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysSinceLastActivity);
        return compteBancaireRepository.findInactiveAccountsOlderThan(cutoffDate);
    }

    @Override @Transactional(readOnly = true)
    public List<CompteBancaire> findAccountsNeedingAttention() {
        List<CompteBancaire> accounts = new ArrayList<>();

        accounts.addAll(findAccountsWithLowBalance(new BigDecimal("50")));
        accounts.addAll(findInactiveAccounts(90));

        LocalDate now = LocalDate.now();
        List<CarteBancaire> expiredCards = carteBancaireRepository.findExpiredCards(now);
        for (CarteBancaire carte : expiredCards) {
            if (!accounts.contains(carte.getCompteBancaire())) {
                accounts.add(carte.getCompteBancaire());
            }
        }
        return accounts;
    }

    @Override @Transactional(readOnly = true)
    public boolean accountExists(String numCompte) {
        return compteBancaireRepository.existsByNumCompte(numCompte);
    }

    @Override @Transactional(readOnly = true)
    public boolean canPerformOperation(String numCompte, BigDecimal montant) {
        return compteBancaireRepository.findByNumCompte(numCompte)
                .map(cb -> cb.isActive() && cb.getBalance().compareTo(montant) >= 0)
                .orElse(false);
    }

    @Override @Transactional(readOnly = true)
    public boolean hasCard(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        return carteBancaireRepository.findByCompteBancaire(compte).isPresent();
    }

    @Override @Transactional(readOnly = true)
    public boolean isSavingsAccount(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        return compteEpargneRepository.findByCompteBancaire(compte).isPresent();
    }

    @Override
    public void setWithdrawalLimit(String numCompte, BigDecimal dailyLimit) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        CarteBancaire carte = carteBancaireRepository.findByCompteBancaire(compte).orElse(null);
        if (carte != null) {
            carte.setPlafondJournalier(dailyLimit.doubleValue());
            carteBancaireRepository.save(carte);
        }
    }

    @Override
    public void setTransferLimit(String numCompte, BigDecimal monthlyLimit) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        CarteBancaire carte = carteBancaireRepository.findByCompteBancaire(compte).orElse(null);
        if (carte != null) {
            carte.setPlafondMensuel(monthlyLimit.doubleValue());
            carteBancaireRepository.save(carte);
        }
    }

    @Override @Transactional(readOnly = true)
    public BigDecimal getRemainingDailyLimit(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        CarteBancaire carte = carteBancaireRepository.findByCompteBancaire(compte).orElse(null);
        if (carte == null || Objects.equals(carte.getPlafondJournalier(), 0d)) {
            return BigDecimal.ZERO;
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        List<Operation> todayWithdrawals = operationRepository.findByCompteAndDateBetween(
                        compte, startOfDay, LocalDateTime.now())
                .stream()
                .filter(op -> op.getType() == TypeOperation.RETRAIT)
                .toList();

        BigDecimal totalToday = todayWithdrawals.stream()
                .map(Operation::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal limit = BigDecimal.valueOf(carte.getPlafondJournalier());
        BigDecimal remaining = limit.subtract(totalToday);
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }

    @Override
    public void sendLowBalanceAlert(String numCompte) {

    }

    @Override
    public void sendStatementNotification(String numCompte) {

    }

    @Override
    public List<String> getPendingNotifications(String numCompte) {
        return null;
    }

    /* ====== Comptes du principal (par email) ====== */
    @Override
    public List<CompteBancaire> getAccountsForPrincipal(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return Collections.emptyList();
        }
        final String email = authentication.getName();
        return compteBancaireRepository.findByClientEmail(email);
    }

    /* ===================== Générateurs ===================== */

    private String generateAccountNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) sb.append(random.nextInt(10));
        return sb.toString();
    }

    private String generateCardNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) sb.append(random.nextInt(10));
        return sb.toString();
    }

    private String generateCVV() {
        Random random = new Random();
        return String.format("%03d", random.nextInt(1000));
    }
    @Override
    public void savingsDeposit(String numCompte, BigDecimal montant) {
        assertMontantPositif(montant);
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        assertActif(compte);

        // débiter le compte courant
        compte.debiter(montant);
        compteBancaireRepository.save(compte);

        // créditer l'épargne
        CompteEpargne epargne = compteEpargneRepository.findByCompteBancaire(compte)
                .orElseThrow(() -> new ResourceNotFoundException("Ce n'est pas un compte épargne"));
        epargne.alimenter(montant);
        compteEpargneRepository.save(epargne);

        // tracer 2 opérations si tu veux (ou 1 avec type TRANSFERT_INTERNE)
        Operation o = new Operation(compte, montant, TypeOperation.VIREMENT, "Virement vers épargne");
        o.setNumeroCompteDestinataire(compte.getNumCompte());
        o.setCommunication("Courant -> Epargne");
        operationRepository.save(o);
    }

    @Override
    public void savingsWithdraw(String numCompte, BigDecimal montant) {
        assertMontantPositif(montant);
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        assertActif(compte);

        // débiter l'épargne
        CompteEpargne epargne = compteEpargneRepository.findByCompteBancaire(compte)
                .orElseThrow(() -> new ResourceNotFoundException("Ce n'est pas un compte épargne"));
        epargne.retirer(montant);
        compteEpargneRepository.save(epargne);

        // créditer le courant
        compte.crediter(montant);
        compteBancaireRepository.save(compte);

        Operation o = new Operation(compte, montant, TypeOperation.VIREMENT, "Virement depuis épargne");
        o.setNumeroCompteDestinataire(compte.getNumCompte());
        o.setCommunication("Epargne -> Courant");
        operationRepository.save(o);
    }

}
