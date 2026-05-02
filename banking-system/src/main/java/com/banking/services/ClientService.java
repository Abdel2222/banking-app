package com.banking.services;

import com.banking.entities.Client;
import com.banking.entities.CompteBancaire;
import com.banking.entities.FraisDeGestion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ClientService {

    // CRUD de base
    Client createClient(String prenom, String nom, String email, String motDePasse);
    Client updateClient(Long id, Client clientDetails);
    Optional<Client> findById(Long id);
    Optional<Client> findByEmail(String email);
    List<Client> findAll();
    void deleteClient(Long id);

    // Authentification
    Optional<Client> authenticate(String email, String motDePasse);
    boolean changePassword(Long clientId, String oldPassword, String newPassword);

    // Gestion des comptes
    CompteBancaire openAccount(Long clientId, String typeCompte);
    List<CompteBancaire> getClientAccounts(Long clientId);
    List<CompteBancaire> getActiveAccounts(Long clientId);
    BigDecimal getTotalBalance(Long clientId);

    // Gestion des frais
    FraisDeGestion addFees(Long clientId, FraisDeGestion frais);
    List<FraisDeGestion> getClientFees(Long clientId);
    BigDecimal getTotalActiveFees(Long clientId);
    void payFees(Long clientId, Long fraisId);

    // Recherche et filtrage
    List<Client> searchByName(String searchTerm);
    List<Client> findClientsWithActiveAccounts();
    List<Client> findClientsWithUnpaidFees();
    List<Client> findWealthyClients(BigDecimal minimumBalance);

    // Statistiques
    Long countActiveClients();
    Long countTotalAccounts(Long clientId);
    boolean hasUnpaidFees(Long clientId);

    // Validation
    boolean emailExists(String email);
    boolean canOpenNewAccount(Long clientId);
    boolean isEligibleForLoan(Long clientId);
}