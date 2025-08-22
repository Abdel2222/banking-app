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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class ClientServiceImpl implements ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private CompteBancaireRepository compteBancaireRepository;

    @Autowired
    private FraisDeGestionRepository fraisDeGestionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Client createClient(String prenom, String nom, String email, String motDePasse) {
        if (emailExists(email)) {
            throw new IllegalArgumentException("Un client avec cet email existe déjà");
        }

        Client client = new Client(prenom, nom, email, passwordEncoder.encode(motDePasse));
        return clientRepository.save(client);
    }

    @Override
    public Client updateClient(Long id, Client clientDetails) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        client.setPrenom(clientDetails.getPrenom());
        client.setNom(clientDetails.getNom());

        // Vérifier si l'email change et s'il n'est pas déjà utilisé
        if (!client.getEmail().equals(clientDetails.getEmail())) {
            if (emailExists(clientDetails.getEmail())) {
                throw new IllegalArgumentException("Cet email est déjà utilisé");
            }
            client.setEmail(clientDetails.getEmail());
        }

        return clientRepository.save(client);
    }

    @Override
    public Optional<Client> findById(Long id) {
        return clientRepository.findById(id);
    }

    @Override
    public Optional<Client> findByEmail(String email) {
        return clientRepository.findByEmail(email);
    }

    @Override
    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    @Override
    public void deleteClient(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        // Vérifier qu'il n'a pas de comptes actifs
        if (!client.getComptesActifs().isEmpty()) {
            throw new IllegalStateException("Impossible de supprimer un client avec des comptes actifs");
        }

        clientRepository.delete(client);
    }

    @Override
    public Optional<Client> authenticate(String email, String motDePasse) {
        Optional<Client> client = clientRepository.findByEmail(email);
        if (client.isPresent() && passwordEncoder.matches(motDePasse, client.get().getMotDePasse())) {
            return client;
        }
        return Optional.empty();
    }

    @Override
    public boolean changePassword(Long clientId, String oldPassword, String newPassword) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        if (!passwordEncoder.matches(oldPassword, client.getMotDePasse())) {
            return false;
        }

        client.setMotDePasse(passwordEncoder.encode(newPassword));
        clientRepository.save(client);
        return true;
    }

    @Override
    public CompteBancaire openAccount(Long clientId, String typeCompte) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        if (!canOpenNewAccount(clientId)) {
            throw new IllegalStateException("Le client ne peut pas ouvrir de nouveau compte");
        }

        // Générer un numéro de compte unique
        String numCompte = generateAccountNumber();
        while (compteBancaireRepository.existsByNumCompte(numCompte)) {
            numCompte = generateAccountNumber();
        }

        CompteBancaire compte = new CompteBancaire(numCompte, client);
        compte = compteBancaireRepository.save(compte);

        // Ajouter des frais de tenue de compte
        FraisDeGestion fraisTenue = new FraisDeGestion(
                new BigDecimal("5.00"),
                "Frais de tenue de compte mensuel",
                FraisDeGestion.TypeFrais.TENUE_COMPTE,
                LocalDate.now().plusYears(1),
                client
        );
        fraisTenue.setPeriodicite(FraisDeGestion.Periodicite.MENSUEL);
        fraisDeGestionRepository.save(fraisTenue);

        return compte;
    }

    @Override
    public List<CompteBancaire> getClientAccounts(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        return compteBancaireRepository.findByClient(client);
    }

    @Override
    public List<CompteBancaire> getActiveAccounts(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        return client.getComptesActifs();
    }

    @Override
    public BigDecimal getTotalBalance(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        return client.getSoldeTotal();
    }

    @Override
    public FraisDeGestion addFees(Long clientId, FraisDeGestion frais) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        frais.setClient(client);
        return fraisDeGestionRepository.save(frais);
    }

    @Override
    public List<FraisDeGestion> getClientFees(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        return fraisDeGestionRepository.findByClient(client);
    }

    @Override
    public BigDecimal getTotalActiveFees(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        return client.getTotalFraisActifs();
    }

    @Override
    public void payFees(Long clientId, Long fraisId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        FraisDeGestion frais = fraisDeGestionRepository.findById(fraisId)
                .orElseThrow(() -> new RuntimeException("Frais non trouvé"));

        if (!frais.getClient().equals(client)) {
            throw new IllegalArgumentException("Ces frais n'appartiennent pas à ce client");
        }

        frais.facturer();
        fraisDeGestionRepository.save(frais);
    }

    @Override
    public List<Client> searchByName(String searchTerm) {
        return clientRepository.findByNomOrPrenom(searchTerm, searchTerm);
    }

    @Override
    public List<Client> findClientsWithActiveAccounts() {
        return clientRepository.findClientsWithActiveAccounts();
    }

    @Override
    public List<Client> findClientsWithUnpaidFees() {
        return clientRepository.findClientsWithUnpaidFees();
    }

    @Override
    public List<Client> findWealthyClients(BigDecimal minimumBalance) {
        return clientRepository.findClientsBySoldeTotalGreaterThan(minimumBalance);
    }

    @Override
    public Long countActiveClients() {
        return clientRepository.countActiveClients();
    }

    @Override
    public Long countTotalAccounts(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        return (long) client.getNombreComptes();
    }

    @Override
    public boolean hasUnpaidFees(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        return client.aDesFraisImpayes();
    }

    @Override
    public boolean emailExists(String email) {
        return clientRepository.existsByEmail(email);
    }

    @Override
    public boolean canOpenNewAccount(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        // Règles métier : max 5 comptes, pas de frais impayés
        return client.getNombreComptes() < 5 && !client.aDesFraisImpayes();
    }

    @Override
    public boolean isEligibleForLoan(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        // Règles métier pour l'éligibilité au prêt
        BigDecimal minBalance = new BigDecimal("1000");
        boolean hasActiveAccount = !client.getComptesActifs().isEmpty();
        boolean hasGoodBalance = client.getSoldeTotal().compareTo(minBalance) >= 0;
        boolean noUnpaidFees = !client.aDesFraisImpayes();

        return hasActiveAccount && hasGoodBalance && noUnpaidFees;
    }

    private String generateAccountNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}