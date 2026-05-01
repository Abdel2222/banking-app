package com.banking.services.impl;

import com.banking.entities.Client;
import com.banking.entities.CompteBancaire;
import com.banking.entities.DemandeCarteBancaire;
import com.banking.entity.enums.CardRequestStatus;
import com.banking.exceptions.AccountNotActiveException;
import com.banking.exceptions.DuplicateResourceException;
import com.banking.exceptions.InvalidOperationException;
import com.banking.exceptions.ResourceNotFoundException;
import com.banking.exceptions.UnauthorizedAccessException;
import com.banking.repositories.ClientRepository;
import com.banking.repositories.CompteBancaireRepository;
import com.banking.repositories.DemandeCarteBancaireRepository;
import com.banking.utils.SecurityUtils;
import com.banking.services.DemandeCarteBancaireService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.banking.entities.CarteBancaire;
import com.banking.repositories.CarteBancaireRepository;
import java.time.LocalDateTime;

import java.util.List;

@Service
@Transactional
public class DemandeCarteBancaireServiceImpl implements DemandeCarteBancaireService {

    private static final Logger log = LoggerFactory.getLogger(DemandeCarteBancaireServiceImpl.class);

    private final DemandeCarteBancaireRepository demandeRepo;
    private final CompteBancaireRepository compteRepo;
    private final ClientRepository clientRepo;


    public DemandeCarteBancaireServiceImpl(
            DemandeCarteBancaireRepository demandeRepo,
            CompteBancaireRepository compteRepo,
            ClientRepository clientRepo
    ) {
        this.demandeRepo = demandeRepo;
        this.compteRepo = compteRepo;
        this.clientRepo = clientRepo;
    }

    @Override
    public DemandeCarteBancaire demanderCarte(Long compteId) {
        log.debug("\uD83D\uDCC5 DÃ©but de demande de carte pour compte ID = {}", compteId);

        Long clientId = SecurityUtils.currentClientId();
        log.debug("\uD83D\uDD10 Client connectÃ© ID = {}", clientId);

        Client client = clientRepo.findById(clientId)
                .orElseThrow(() -> {
                    log.error("Client introuvable: ID = {}", clientId);
                    return new ResourceNotFoundException("Client introuvable");
                });

        CompteBancaire compte = compteRepo.findById(compteId)
                .orElseThrow(() -> {
                    log.error("Compte introuvable: ID = {}", compteId);
                    return new ResourceNotFoundException("Compte introuvable");
                });

        if (!compte.getClient().getId().equals(clientId)) {
            log.warn("AccÃ¨s interdit: compte ID = {} n'appartient pas au client ID = {}", compteId, clientId);
            throw new UnauthorizedAccessException("Ce compte n'appartient pas au client courant");
        }

        String status = (compte.getStatus() == null) ? null : compte.getStatus().toString();
        if (!"ACTIVATED".equalsIgnoreCase(status)) {
            log.warn("Compte inactif: status = {}", compte.getStatus());
            throw new AccountNotActiveException("Le compte n'est pas activÃ©");
        }

        if (compte.getCarteBancaire() != null) {
            log.warn("Le compte ID = {} possÃ¨de dÃ©jÃ  une carte", compteId);
            throw new DuplicateResourceException("Ce compte possÃ¨de dÃ©jÃ  une carte");
        }

        boolean existeDemandeEnCours =
                demandeRepo.existsByCompte_IdAndStatus(compteId, CardRequestStatus.PENDING);
        if (existeDemandeEnCours) {
            log.warn("Une demande en cours existe dÃ©jÃ  pour le compte ID = {}", compteId);
            throw new DuplicateResourceException("Une demande de carte est dÃ©jÃ  en attente pour ce compte");
        }

        DemandeCarteBancaire demande = new DemandeCarteBancaire();
        demande.setClient(client);
        demande.setCompte(compte);
        demande.setStatus(CardRequestStatus.PENDING);

        DemandeCarteBancaire saved = demandeRepo.save(demande);
        log.info("\u2705 Demande crÃ©Ã©e: ID = {}, compte ID = {}, client ID = {}", saved.getId(), compteId, clientId);

        return saved;
    }

    @Override
    public DemandeCarteBancaire approuverDemande(Long demandeId) {
        DemandeCarteBancaire demande = demandeRepo.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        if (demande.getStatus() != CardRequestStatus.PENDING) {
            throw new InvalidOperationException("Cette demande a dÃ©jÃ  Ã©tÃ© traitÃ©e");
        }

        demande.setStatus(CardRequestStatus.APPROVED);
        return demandeRepo.save(demande);
    }

    @Override
    public DemandeCarteBancaire rejeterDemande(Long demandeId, String raison) {
        DemandeCarteBancaire demande = demandeRepo.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        if (demande.getStatus() != CardRequestStatus.PENDING) {
            throw new InvalidOperationException("Cette demande a dÃ©jÃ  Ã©tÃ© traitÃ©e");
        }

        demande.setStatus(CardRequestStatus.REJECTED);
        // Si tu as un champ rejectedReason : demande.setRejectedReason(raison);
        return demandeRepo.save(demande);
    }

    @Override
    public List<DemandeCarteBancaire> listerDemandes() {
        return demandeRepo.findAll();
    }

    @Override
    public List<DemandeCarteBancaire> listerDemandesParClient(Long clientId) {
        return demandeRepo.findByClient_Id(clientId);
    }
}