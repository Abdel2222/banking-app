package com.banking.services.impl;

import com.banking.entities.Client;
import com.banking.entities.CompteBancaire;
import com.banking.entities.FraisDeGestion;
import com.banking.entity.enums.AccountStatus;
import com.banking.repositories.ClientRepository;
import com.banking.repositories.CompteBancaireRepository;
import com.banking.repositories.FraisDeGestionRepository;
import com.banking.services.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class ClientServiceImpl implements ClientService {

    @Autowired private ClientRepository          clientRepository;
    @Autowired private CompteBancaireRepository  compteBancaireRepository;
    @Autowired private FraisDeGestionRepository  fraisDeGestionRepository;
    @Autowired private PasswordEncoder           passwordEncoder;

    /* ===================== Utilitaire ===================== */

    private Client getClientOrThrow(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
    }

    /* ===================== CRUD Client ===================== */

    @Override
    public Client createClient(String prenom, String nom, String email, String motDePasse) {
        if (emailExists(email))
            throw new IllegalArgumentException("Un client avec cet email existe déjà");
        Client client = new Client(prenom, nom, email, passwordEncoder.encode(motDePasse));
        return clientRepository.save(client);
    }

    @Override
    public Client updateClient(Long id, Client clientDetails) {
        Client client = getClientOrThrow(id);
        client.setPrenom(clientDetails.getPrenom());
        client.setNom(clientDetails.getNom());

        if (!client.getEmail().equals(clientDetails.getEmail())) {
            if (emailExists(clientDetails.getEmail()))
                throw new IllegalArgumentException("Cet email est déjà utilisé");
            client.setEmail(clientDetails.getEmail());
        }
        return clientRepository.save(client);
    }

    @Override @Transactional(readOnly = true)
    public Optional<Client> findById(Long id) {
        return clientRepository.findById(id);
    }

    @Override @Transactional(readOnly = true)
    public Optional<Client> findByEmail(String email) {
        return clientRepository.findByEmail(email);
    }

    @Override @Transactional(readOnly = true)
    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    @Override
    public void deleteClient(Long id) {
        Client client = getClientOrThrow(id);
        if (!client.getComptesActifs().isEmpty())
            throw new IllegalStateException("Impossible de supprimer un client avec des comptes actifs");
        clientRepository.delete(client);
    }

    /* ===================== Auth ===================== */

    @Override
    public Optional<Client> authenticate(String email, String motDePasse) {
        Optional<Client> client = clientRepository.findByEmail(email);
        if (client.isPresent() &&
                passwordEncoder.matches(motDePasse, client.get().getMotDePasse()))
            return client;
        return Optional.empty();
    }

    @Override
    public boolean changePassword(Long clientId, String oldPassword, String newPassword) {
        Client client = getClientOrThrow(clientId);
        if (!passwordEncoder.matches(oldPassword, client.getMotDePasse()))
            return false;
        client.setMotDePasse(passwordEncoder.encode(newPassword));
        clientRepository.save(client);
        return true;
    }

    /* ===================== Comptes ===================== */

    @Override
    public CompteBancaire openAccount(Long clientId, String typeCompte) {
        Client client = getClientOrThrow(clientId);

        if (!canOpenNewAccount(clientId))
            throw new IllegalStateException("Le client ne peut pas ouvrir de nouveau compte");

        String numCompte = generateAccountNumber();
        while (compteBancaireRepository.existsByNumCompte(numCompte))
            numCompte = generateAccountNumber();

        CompteBancaire compte = new CompteBancaire(numCompte, client);
        compte = compteBancaireRepository.save(compte);

        // ✅ FraisDeGestion avec compte bancaire obligatoire
        FraisDeGestion fraisTenue = new FraisDeGestion(
                new BigDecimal("5.00"),
                "Frais de tenue de compte mensuel",
                FraisDeGestion.TypeFrais.TENUE_COMPTE,
                LocalDate.now().plusYears(1),
                client,
                compte  // ✅ ajouté
        );
        fraisTenue.setPeriodicite(FraisDeGestion.Periodicite.MENSUEL);
        fraisDeGestionRepository.save(fraisTenue);

        return compte;
    }

    @Override @Transactional(readOnly = true)
    public List<CompteBancaire> getClientAccounts(Long clientId) {
        Client client = getClientOrThrow(clientId);
        return compteBancaireRepository.findByClient(client);
    }

    @Override @Transactional(readOnly = true)
    public List<CompteBancaire> getActiveAccounts(Long clientId) {
        return getClientOrThrow(clientId).getComptesActifs();
    }

    @Override @Transactional(readOnly = true)
    public BigDecimal getTotalBalance(Long clientId) {
        return getClientOrThrow(clientId).getSoldeTotal();
    }

    /* ===================== Frais ===================== */

    @Override

    public FraisDeGestion addFees(Long clientId, FraisDeGestion frais) {
        Client client = getClientOrThrow(clientId);
        frais.setClient(client);

        // ✅ Supprime la vérification si getCompteBancaire() n'existe pas encore
        // if (frais.getCompteBancaire() == null)
        //     throw new IllegalArgumentException("...");

        return fraisDeGestionRepository.save(frais);
    }

    @Override @Transactional(readOnly = true)
    public List<FraisDeGestion> getClientFees(Long clientId) {
        Client client = getClientOrThrow(clientId);
        return fraisDeGestionRepository.findByClient(client);
    }

    @Override @Transactional(readOnly = true)
    public BigDecimal getTotalActiveFees(Long clientId) {
        return getClientOrThrow(clientId).getTotalFraisActifs();
    }

    @Override
    public void payFees(Long clientId, Long fraisId) {
        Client client = getClientOrThrow(clientId);
        FraisDeGestion frais = fraisDeGestionRepository.findById(fraisId)
                .orElseThrow(() -> new RuntimeException("Frais non trouvé"));

        if (!frais.getClient().equals(client))
            throw new IllegalArgumentException("Ces frais n'appartiennent pas à ce client");

        frais.facturer();
        fraisDeGestionRepository.save(frais);
    }

    /* ===================== Recherche / stats ===================== */

    @Override @Transactional(readOnly = true)
    public List<Client> searchByName(String searchTerm) {
        return clientRepository.findByNomOrPrenom(searchTerm, searchTerm);
    }

    @Override @Transactional(readOnly = true)
    public List<Client> findClientsWithActiveAccounts() {
        return clientRepository.findClientsWithActiveAccounts();
    }

    @Override @Transactional(readOnly = true)
    public List<Client> findClientsWithUnpaidFees() {
        return clientRepository.findClientsWithUnpaidFees();
    }

    @Override @Transactional(readOnly = true)
    public List<Client> findWealthyClients(BigDecimal minimumBalance) {
        return clientRepository.findClientsBySoldeTotalGreaterThan(minimumBalance);
    }

    @Override @Transactional(readOnly = true)
    public Long countActiveClients() {
        return clientRepository.countActiveClients();
    }

    @Override @Transactional(readOnly = true)
    public Long countTotalAccounts(Long clientId) {
        return (long) getClientOrThrow(clientId).getNombreComptes();
    }

    @Override @Transactional(readOnly = true)
    public boolean hasUnpaidFees(Long clientId) {
        return getClientOrThrow(clientId).aDesFraisImpayes();
    }

    @Override @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return clientRepository.existsByEmail(email);
    }

    @Override @Transactional(readOnly = true)
    public boolean canOpenNewAccount(Long clientId) {
        Client client = getClientOrThrow(clientId);
        return client.getNombreComptes() < 5 && !client.aDesFraisImpayes();
    }

    @Override @Transactional(readOnly = true)
    public boolean isEligibleForLoan(Long clientId) {
        Client client = getClientOrThrow(clientId);
        return !client.getComptesActifs().isEmpty()
                && client.getSoldeTotal().compareTo(new BigDecimal("1000")) >= 0
                && !client.aDesFraisImpayes();
    }

    /* ===================== Générateur ===================== */

    private String generateAccountNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) sb.append(random.nextInt(10));
        return sb.toString();
    }
}