package com.banking.services.impl;

import com.banking.dto.request.CreateAccountRequest;
import com.banking.entities.*;
import com.banking.entity.enums.AccountStatus;
import com.banking.entity.enums.TypeOperation;
import com.banking.exceptions.ResourceNotFoundException;
import com.banking.repositories.*;
import com.banking.services.CompteBancaireService;
import com.banking.services.FraisDeGestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CompteBancaireServiceImpl.class);

    @Autowired private CompteBancaireRepository compteBancaireRepository;
    @Autowired private ClientRepository         clientRepository;
    @Autowired private OperationRepository      operationRepository;
    @Autowired private CarteBancaireRepository  carteBancaireRepository;
    @Autowired private CompteEpargneRepository  compteEpargneRepository;
    @Autowired private FraisDeGestionService    fraisDeGestionService;

    /* ===================== Utilitaires ===================== */

    private void assertMontantPositif(BigDecimal montant) {
        if (montant == null || montant.signum() <= 0)
            throw new IllegalArgumentException("Le montant doit être strictement positif");
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
        if (!c.isActive())
            throw new IllegalStateException("Le compte n'est pas actif");
    }

    /* ===================== CRUD ===================== */

    @Override
    public CompteBancaire createAccount(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));

        if (compteBancaireRepository.existsByClientId(client.getId())) {
            return compteBancaireRepository.findFirstByClientIdOrderByIdAsc(client.getId())
                    .orElseThrow();
        }

        CompteBancaire compte = new CompteBancaire(generateAccountNumber(), client);
        CompteBancaire saved  = compteBancaireRepository.save(compte);

        try {
            fraisDeGestionService.appliquerFraisOuverture(saved.getNumCompte());
        } catch (Exception e) {
            log.warn("Frais d'ouverture non appliqué sur {}", saved.getNumCompte(), e);
        }

        return saved;
    }

    @Override @Transactional(readOnly = true)
    public Optional<CompteBancaire> findById(Long id) {
        return compteBancaireRepository.findById(id);
    }

    @Override @Transactional(readOnly = true)
    public Optional<CompteBancaire> findByNumCompte(String numCompte) {
        return compteBancaireRepository.findByNumCompte(numCompte);
    }

    @Override @Transactional(readOnly = true)
    public List<CompteBancaire> findAll() {
        return compteBancaireRepository.findAll();
    }

    @Override
    public CompteBancaire updateAccount(Long id, CompteBancaire accountDetails) {
        CompteBancaire compte = getCompteOrThrowById(id);
        compte.setStatus(accountDetails.getStatus());
        return compteBancaireRepository.save(compte);
    }

    @Override
    public void deleteAccount(Long id) {
        CompteBancaire compte = getCompteOrThrowById(id);
        if (compte.getBalance().compareTo(BigDecimal.ZERO) != 0)
            throw new IllegalStateException("Impossible de supprimer un compte avec un solde non nul");
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

        Operation op = new Operation(compte, montant, TypeOperation.DEPOT,
                (description != null && !description.isBlank()) ? description : "Dépôt");
        return operationRepository.save(op);
    }

    @Override
    public Operation withdraw(String numCompte, BigDecimal montant, String description) {
        assertMontantPositif(montant);
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        assertActif(compte);

        compte.debiter(montant);
        compteBancaireRepository.save(compte);

        Operation op = new Operation(compte, montant, TypeOperation.RETRAIT,
                (description != null && !description.isBlank()) ? description : "Retrait");
        return operationRepository.save(op);
    }

    /* ===================== Virement ===================== */

    @Override
    public Operation transfer(String numCompteSource, String numCompteDestinataire,
                              BigDecimal montant, String communication) {
        log.info(">>> Virement : {} € de [{}] vers [{}]", montant, numCompteSource, numCompteDestinataire);

        assertMontantPositif(montant);
        if (Objects.equals(numCompteSource, numCompteDestinataire))
            throw new IllegalArgumentException("Source et destinataire ne peuvent pas être identiques");

        CompteBancaire source = getCompteOrThrowByNum(numCompteSource);
        CompteBancaire dest   = getCompteOrThrowByNum(numCompteDestinataire);

        if (dest.getClient() == null)
            throw new IllegalStateException("Le compte destinataire n'est pas lié à un client.");

        assertActif(source);
        assertActif(dest);

        source.debiter(montant);
        dest.crediter(montant);
        compteBancaireRepository.save(source);
        compteBancaireRepository.save(dest);
        compteBancaireRepository.flush();

        String nomClientDest = dest.getClient().getNomComplet();

        Operation oSrc = new Operation(source, montant, TypeOperation.VIREMENT);
        oSrc.setNumeroCompteDestinataire(numCompteDestinataire);
        oSrc.setCommunication(communication);
        oSrc.setNomTitulaireDestinataire(nomClientDest);

        Operation oDst = new Operation(dest, montant, TypeOperation.VIREMENT);
        oDst.setNumeroCompteDestinataire(numCompteDestinataire);
        oDst.setCommunication("Réception: " + (communication != null ? communication : ""));
        oDst.setNomTitulaireDestinataire(nomClientDest);

        operationRepository.save(oSrc);
        operationRepository.save(oDst);

        return oSrc;
    }

    @Override
    public void effectuerVirement(String sourceAccount, String destinationAccount,
                                  BigDecimal montant, String description) {
        this.transfer(sourceAccount, destinationAccount, montant, description);
    }

    /* ===================== État du compte ===================== */

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

    @Override
    public CompteBancaire closeAccount(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        compte.close();
        return compteBancaireRepository.save(compte);
    }

    /* ===================== createForPrincipal ===================== */

    @Override
    public CompteBancaire createForPrincipal(Authentication authentication, CreateAccountRequest req) {
        if (authentication == null || authentication.getName() == null)
            throw new IllegalArgumentException("Utilisateur non authentifié");

        String email = authentication.getName();
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Client introuvable: " + email));

        if (compteBancaireRepository.existsByClientId(client.getId())) {
            return compteBancaireRepository.findFirstByClientIdOrderByIdAsc(client.getId())
                    .orElseThrow();
        }

        CompteBancaire compte = CompteBancaire.createNew(
                client,
                (req != null && req.getIntitule() != null && !req.getIntitule().isBlank())
                        ? req.getIntitule() : "Compte courant",
                (req != null && req.getDevise() != null && !req.getDevise().isBlank())
                        ? req.getDevise() : "EUR"
        );
        return compteBancaireRepository.save(compte);
    }

    /* ===================== Cartes ===================== */

    @Override
    public CarteBancaire issueCard(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);

        if (carteBancaireRepository.findByCompteBancaire(compte).isPresent())
            throw new IllegalStateException("Ce compte possède déjà une carte");

        CarteBancaire carte = new CarteBancaire(
                generateCardNumber(), LocalDate.now().plusYears(3), generateCVV(), compte);
        if (carte.getPlafondJournalier() == null) carte.setPlafondJournalier(500d);
        if (carte.getPlafondMensuel()    == null) carte.setPlafondMensuel(2000d);
        return carteBancaireRepository.save(carte);
    }

    @Override
    public CarteBancaire blockCard(String numCompte, String raison) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        CarteBancaire carte = carteBancaireRepository.findByCompteBancaire(compte)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune carte associée à ce compte"));
        carte.bloquer();
        carteBancaireRepository.save(carte);

        Operation op = new Operation(compte, BigDecimal.ZERO, TypeOperation.BLOCAGE_CARTE,
                (raison != null && !raison.isBlank()) ? raison : "Blocage de carte");
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

    /* ===================== Épargne ===================== */

    @Override
    public CompteEpargne convertToSavingsAccount(String numCompte, BigDecimal premierMontant) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);

        // ✅ instanceof au lieu de findByCompteBancaire
        if (compte instanceof CompteEpargne)
            throw new IllegalStateException("Ce compte est déjà un compte épargne");
        if (compteEpargneRepository.existsByNumCompte(numCompte))
            throw new IllegalStateException("Un compte épargne existe déjà pour ce numéro");

        BigDecimal montant = (premierMontant != null && premierMontant.signum() >= 0)
                ? premierMontant : BigDecimal.ZERO;

        CompteEpargne epargne = new CompteEpargne();
        epargne.setNumCompte(generateAccountNumber());
        epargne.setClient(compte.getClient());
        epargne.setDevise(compte.getDevise());
        epargne.setIntitule("Compte épargne");
        epargne.setBalance(montant);
        epargne.setPremierMontant(montant);
        epargne.activate();
        return compteEpargneRepository.save(epargne);
    }

    @Override
    public CompteEpargne convertToSavingsAccountInternal(String numCompte, BigDecimal premierMontant) {
        return convertToSavingsAccount(numCompte, premierMontant);
    }

    @Override
    public CompteEpargne updateSavingsRate(String numCompte, BigDecimal nouveauTaux) {
        throw new UnsupportedOperationException(
                "Le taux est géré dans Interet, pas dans CompteEpargne");
    }

    @Override
    public void capitalizeInterests(String numCompte) {
        throw new UnsupportedOperationException(
                "La capitalisation est gérée dans InteretService");
    }

    @Override
    public BigDecimal calculateInterests(String numCompte) {
        throw new UnsupportedOperationException(
                "Le calcul des intérêts est géré dans InteretService");
    }

    @Override
    public void savingsDeposit(String numCompte, BigDecimal montant) {
        assertMontantPositif(montant);
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        assertActif(compte);

        compte.debiter(montant);
        compteBancaireRepository.save(compte);

        // ✅ findByNumCompte au lieu de findByCompteBancaire
        CompteEpargne epargne = compteEpargneRepository.findByNumCompte(numCompte)
                .orElseThrow(() -> new ResourceNotFoundException("Compte épargne introuvable"));
        epargne.crediter(montant);
        compteEpargneRepository.save(epargne);

        Operation o = new Operation(compte, montant, TypeOperation.VIREMENT, "Virement vers épargne");
        o.setNumeroCompteDestinataire(epargne.getNumCompte());
        o.setCommunication("Courant -> Epargne");
        operationRepository.save(o);
    }

    @Override
    public void savingsWithdraw(String numCompte, BigDecimal montant) {
        assertMontantPositif(montant);
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        assertActif(compte);

        // ✅ findByNumCompte au lieu de findByCompteBancaire
        CompteEpargne epargne = compteEpargneRepository.findByNumCompte(numCompte)
                .orElseThrow(() -> new ResourceNotFoundException("Compte épargne introuvable"));
        epargne.debiter(montant);
        compteEpargneRepository.save(epargne);

        compte.crediter(montant);
        compteBancaireRepository.save(compte);

        Operation o = new Operation(compte, montant, TypeOperation.VIREMENT, "Virement depuis épargne");
        o.setNumeroCompteDestinataire(compte.getNumCompte());
        o.setCommunication("Epargne -> Courant");
        operationRepository.save(o);
    }

    @Override
    public boolean isSavingsAccount(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        // ✅ instanceof au lieu de findByCompteBancaire
        return compte instanceof CompteEpargne
                || compteEpargneRepository.existsByNumCompte(numCompte);
    }

    /* ===================== Requêtes / stats ===================== */

    @Override @Transactional(readOnly = true)
    public BigDecimal getBalance(String numCompte) {
        return getCompteOrThrowByNum(numCompte).getBalance();
    }

    @Override @Transactional(readOnly = true)
    public List<Operation> getAccountHistory(String numCompte,
                                             LocalDateTime debut, LocalDateTime fin) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        return operationRepository
                .findByCompteBancaireAndDateOperationBetweenOrderByDateOperationDesc(
                        compte, debut, fin);
    }

    @Override @Transactional(readOnly = true)
    public List<Operation> getLastOperations(String numCompte, int nombre) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        return operationRepository
                .findByCompteBancaireOrderByDateOperationDesc(compte, PageRequest.of(0, nombre))
                .getContent();
    }

    @Override @Transactional(readOnly = true)
    public Map<String, BigDecimal> getAccountStatistics(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);

        Map<String, BigDecimal> stats = new HashMap<>();
        stats.put("solde", compte.getBalance());

        LocalDateTime now     = LocalDateTime.now();
        LocalDateTime veryOld = now.minusYears(20);

        List<Operation> allOps = operationRepository
                .findByCompteBancaire_NumCompteAndDateOperationBetweenOrderByDateOperationDesc(
                        numCompte, veryOld, now);

        BigDecimal depots = allOps.stream()
                .filter(o -> o.getTypeOperation() == TypeOperation.DEPOT)
                .map(Operation::getMontant).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal retraits = allOps.stream()
                .filter(o -> o.getTypeOperation() == TypeOperation.RETRAIT)
                .map(Operation::getMontant).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("totalDepots",   depots);
        stats.put("totalRetraits", retraits);

        List<Operation> recentOps = operationRepository
                .findByCompteBancaireAndDateOperationBetweenOrderByDateOperationDesc(
                        compte, now.minusDays(30), now);

        BigDecimal mouvements = recentOps.stream()
                .map(Operation::getMontant).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("mouvementsMensuels", mouvements);
        return stats;
    }

    @Override @Transactional(readOnly = true)
    public List<CompteBancaire> findByStatus(AccountStatus status) {
        return compteBancaireRepository.findByStatus(status);
    }

    @Override @Transactional(readOnly = true)
    public List<CompteBancaire> findAccountsWithLowBalance(BigDecimal threshold) {
        return compteBancaireRepository.findAll().stream()
                .filter(c -> c.getBalance().compareTo(threshold) < 0)
                .toList();
    }

    @Override @Transactional(readOnly = true)
    public List<CompteBancaire> findInactiveAccounts(int daysSinceLastActivity) {
        return compteBancaireRepository
                .findInactiveAccountsOlderThan(LocalDateTime.now().minusDays(daysSinceLastActivity));
    }

    @Override @Transactional(readOnly = true)
    public List<CompteBancaire> findAccountsNeedingAttention() {
        List<CompteBancaire> accounts = new ArrayList<>();
        accounts.addAll(findAccountsWithLowBalance(new BigDecimal("50")));
        accounts.addAll(findInactiveAccounts(90));

        for (CarteBancaire carte : carteBancaireRepository.findExpiredCards(LocalDate.now())) {
            if (!accounts.contains(carte.getCompteBancaire()))
                accounts.add(carte.getCompteBancaire());
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

    /* ===================== Limites ===================== */

    @Override
    public void setWithdrawalLimit(String numCompte, BigDecimal dailyLimit) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        carteBancaireRepository.findByCompteBancaire(compte).ifPresent(c -> {
            c.setPlafondJournalier(dailyLimit.doubleValue());
            carteBancaireRepository.save(c);
        });
    }

    @Override
    public void setTransferLimit(String numCompte, BigDecimal monthlyLimit) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        carteBancaireRepository.findByCompteBancaire(compte).ifPresent(c -> {
            c.setPlafondMensuel(monthlyLimit.doubleValue());
            carteBancaireRepository.save(c);
        });
    }

    @Override @Transactional(readOnly = true)
    public BigDecimal getRemainingDailyLimit(String numCompte) {
        CompteBancaire compte = getCompteOrThrowByNum(numCompte);
        CarteBancaire carte = carteBancaireRepository.findByCompteBancaire(compte).orElse(null);
        if (carte == null || Objects.equals(carte.getPlafondJournalier(), 0d))
            return BigDecimal.ZERO;

        List<Operation> todayOps = operationRepository
                .findByCompteBancaireAndDateOperationBetweenOrderByDateOperationDesc(
                        compte, LocalDate.now().atStartOfDay(), LocalDateTime.now())
                .stream()
                .filter(op -> op.getTypeOperation() == TypeOperation.RETRAIT)
                .toList();

        BigDecimal total     = todayOps.stream().map(Operation::getMontant)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = BigDecimal.valueOf(carte.getPlafondJournalier()).subtract(total);
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }

    /* ===================== Notifications ===================== */

    @Override public void sendLowBalanceAlert(String numCompte) { /* TODO */ }
    @Override public void sendStatementNotification(String numCompte) { /* TODO */ }
    @Override public List<String> getPendingNotifications(String numCompte) {
        return Collections.emptyList();
    }

    /* ===================== Principal ===================== */

    @Override
    public List<CompteBancaire> getAccountsForPrincipal(Authentication authentication) {
        if (authentication == null || authentication.getName() == null
                || authentication.getName().isBlank())
            return Collections.emptyList();
        return compteBancaireRepository.findByClientEmail(authentication.getName());
    }

    /* ===================== Générateurs ===================== */

    private String generateAccountNumber() {
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) sb.append(r.nextInt(10));
        return sb.toString();
    }

    private String generateCardNumber() {
        return generateAccountNumber();
    }

    private String generateCVV() {
        return String.format("%03d", new Random().nextInt(1000));
    }



    private void appliquerFraisOuverture(String numCompte) {
        fraisDeGestionService.appliquerFraisOuverture(numCompte);
    }

}