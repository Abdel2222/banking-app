package com.banking.services.impl;

import com.banking.entities.Client;
import com.banking.entities.CompteBancaire;
import com.banking.entities.FraisDeGestion;
import com.banking.entities.Operation;
import com.banking.entity.enums.TypeOperation;
import com.banking.exceptions.BusinessException;
import com.banking.exceptions.ResourceNotFoundException;
import com.banking.repositories.CompteBancaireRepository;
import com.banking.repositories.FraisDeGestionRepository;
import com.banking.repositories.OperationRepository;
import com.banking.services.ClientService;
import com.banking.services.FraisDeGestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class FraisDeGestionServiceImpl implements FraisDeGestionService {

    private static final Logger logger = LoggerFactory.getLogger(FraisDeGestionServiceImpl.class);

    @Autowired
    private FraisDeGestionRepository fraisRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private OperationRepository operationRepository;

    @Autowired
    private CompteBancaireRepository compteBancaireRepository;

    @Override
    public FraisDeGestion creerFrais(FraisDeGestion frais) {
        logger.info("Création d'un nouveau frais de gestion pour le client ID: {}",
                frais.getClient() != null ? frais.getClient().getId() : null);

        validerFrais(frais);

        if (verifierDoublon(frais)) {
            throw new BusinessException("Un frais de ce type existe déjà pour cette période");
        }

        FraisDeGestion fraisCree = fraisRepository.save(frais);
        logger.info("Frais créé avec succès, ID: {}", fraisCree.getId());
        return fraisCree;
    }

    @Override
    @Transactional(readOnly = true)
    public FraisDeGestion obtenirFrais(Long id) {
        return fraisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Frais non trouvé avec l'ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraisDeGestion> obtenirTousFrais() {
        return fraisRepository.findAll();
    }

    @Override
    public FraisDeGestion mettreAJourFrais(Long id, FraisDeGestion fraisModifie) {
        logger.info("Mise à jour du frais ID: {}", id);

        FraisDeGestion fraisExistant = obtenirFrais(id);

        if (fraisModifie.getMontant() != null) {
            fraisExistant.setMontant(fraisModifie.getMontant());
        }
        if (fraisModifie.getDescription() != null) {
            fraisExistant.setDescription(fraisModifie.getDescription());
        }
        if (fraisModifie.getDateFin() != null) {
            fraisExistant.setDateFin(fraisModifie.getDateFin());
        }
        if (fraisModifie.getPeriodicite() != null) {
            fraisExistant.setPeriodicite(fraisModifie.getPeriodicite());
        }

        validerFrais(fraisExistant);

        FraisDeGestion fraisMisAJour = fraisRepository.save(fraisExistant);
        logger.info("Frais mis à jour avec succès");
        return fraisMisAJour;
    }

    @Override
    public void supprimerFrais(Long id) {
        logger.info("Suppression du frais ID: {}", id);

        FraisDeGestion frais = obtenirFrais(id);

        if (frais.getMontantTotalFacture().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Impossible de supprimer un frais déjà facturé. Désactivez-le plutôt.");
        }

        fraisRepository.delete(frais);
        logger.info("Frais supprimé avec succès");
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraisDeGestion> obtenirFraisClient(Long clientId) {
        Client client = clientService.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + clientId));
        return fraisRepository.findByClient(client);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraisDeGestion> obtenirFraisActifsClient(Long clientId) {
        Client client = clientService.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + clientId));
        return fraisRepository.findByClient(client).stream()
                .filter(FraisDeGestion::getEstActif)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraisDeGestion> obtenirFraisEnCoursClient(Long clientId) {
        Client client = clientService.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + clientId));
        return fraisRepository.findByClient(client).stream()
                .filter(FraisDeGestion::estEnCours)
                .toList();
    }

    @Override
    public FraisDeGestion creerFraisClient(Long clientId, FraisDeGestion frais) {
        Client client = clientService.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + clientId));
        frais.setClient(client);
        return creerFrais(frais);
    }

    @Override
    public List<FraisDeGestion> obtenirFraisAFacturer() {
        return fraisRepository.findAll().stream()
                .filter(FraisDeGestion::doitEtreFacture)
                .toList();
    }

    @Override
    public void facturerFrais(Long fraisId) {
        logger.info("Facturation du frais ID: {}", fraisId);

        FraisDeGestion frais = obtenirFrais(fraisId);

        if (!frais.doitEtreFacture()) {
            throw new BusinessException("Ce frais ne peut pas être facturé maintenant");
        }

        debiterCompteClientPourFrais(frais);

        frais.facturer();
        fraisRepository.save(frais);

        logger.info("Frais facturé avec succès. Montant: {}", frais.getMontant());
    }

    @Override
    public void facturerFraisClient(Long clientId) {
        logger.info("Facturation de tous les frais du client ID: {}", clientId);

        Client client = clientService.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + clientId));

        List<FraisDeGestion> fraisAFacturer = fraisRepository.findByClient(client).stream()
                .filter(FraisDeGestion::doitEtreFacture)
                .toList();

        BigDecimal montantTotal = BigDecimal.ZERO;
        int nombreFacturations = 0;

        for (FraisDeGestion frais : fraisAFacturer) {
            try {
                facturerFrais(frais.getId());
                montantTotal = montantTotal.add(frais.getMontant());
                nombreFacturations++;
            } catch (Exception e) {
                logger.error("Erreur lors de la facturation du frais ID: {}", frais.getId(), e);
            }
        }

        logger.info("Facturation terminée pour le client {}. {} frais facturés pour un montant total de {}",
                clientId, nombreFacturations, montantTotal);
    }

    @Override
    @Scheduled(cron = "0 0 6 * * *")
    public void facturationAutomatique() {
        logger.info("Début de la facturation automatique");

        List<FraisDeGestion> fraisAFacturer = obtenirFraisAFacturer();

        BigDecimal montantTotal = BigDecimal.ZERO;
        int nombreFacturations = 0;
        int nombreEchecs = 0;

        for (FraisDeGestion frais : fraisAFacturer) {
            try {
                facturerFrais(frais.getId());
                montantTotal = montantTotal.add(frais.getMontant());
                nombreFacturations++;
            } catch (Exception e) {
                logger.error("Erreur lors de la facturation automatique du frais ID: {}", frais.getId(), e);
                nombreEchecs++;
            }
        }

        logger.info("Facturation automatique terminée. {} succès, {} échecs, montant total: {}",
                nombreFacturations, nombreEchecs, montantTotal);
    }

    @Override
    public void activerFrais(Long fraisId) {
        logger.info("Activation du frais ID: {}", fraisId);
        FraisDeGestion frais = obtenirFrais(fraisId);
        frais.reactiver();
        fraisRepository.save(frais);
        logger.info("Frais activé avec succès");
    }

    @Override
    public void desactiverFrais(Long fraisId) {
        logger.info("Désactivation du frais ID: {}", fraisId);
        FraisDeGestion frais = obtenirFrais(fraisId);
        frais.desactiver();
        fraisRepository.save(frais);
        logger.info("Frais désactivé avec succès");
    }

    @Override
    @Scheduled(cron = "0 30 6 * * *")
    public void desactiverFraisExpires() {
        logger.info("Désactivation automatique des frais expirés");

        List<FraisDeGestion> fraisExpires = fraisRepository.findAll().stream()
                .filter(f -> f.getEstActif() && f.estEchu())
                .toList();

        int nombreDesactives = 0;
        for (FraisDeGestion frais : fraisExpires) {
            frais.desactiver();
            fraisRepository.save(frais);
            nombreDesactives++;
        }

        logger.info("{} frais expirés ont été désactivés automatiquement", nombreDesactives);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculerMontantTotalActifClient(Long clientId) {
        Client client = clientService.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + clientId));

        return fraisRepository.findByClient(client).stream()
                .filter(FraisDeGestion::getEstActif)
                .filter(FraisDeGestion::estEnCours)
                .map(FraisDeGestion::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculerMontantTotalFactureClient(Long clientId) {
        Client client = clientService.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + clientId));

        return fraisRepository.findByClient(client).stream()
                .map(FraisDeGestion::getMontantTotalFacture)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public Long compterFraisActifsClient(Long clientId) {
        Client client = clientService.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + clientId));

        return fraisRepository.findByClient(client).stream()
                .filter(FraisDeGestion::getEstActif)
                .count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraisDeGestion> rechercherFrais(FraisDeGestion.TypeFrais typeFrais,
                                                FraisDeGestion.Periodicite periodicite,
                                                Boolean estActif,
                                                LocalDate dateDebut,
                                                LocalDate dateFin) {
        List<FraisDeGestion> resultats = fraisRepository.findAll();

        return resultats.stream()
                .filter(f -> typeFrais == null || f.getTypeFrais().equals(typeFrais))
                .filter(f -> periodicite == null || f.getPeriodicite().equals(periodicite))
                .filter(f -> estActif == null || f.getEstActif().equals(estActif))
                .filter(f -> dateDebut == null || !f.getDateDebut().isBefore(dateDebut))
                .filter(f -> dateFin == null || !f.getDateFin().isAfter(dateFin))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraisDeGestion> obtenirHistoriqueFacturationClient(Long clientId) {
        Client client = clientService.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + clientId));

        return fraisRepository.findByClient(client).stream()
                .filter(f -> f.getDerniereFacturation() != null)
                .sorted(Comparator.comparing(FraisDeGestion::getDerniereFacturation).reversed())
                .toList();
    }

    @Override
    public void appliquerFraisOuverture(String numCompte) {
        final BigDecimal FEE = new BigDecimal("2.00");
        final String COMMENT = "FRAIS_OUVERTURE_COMPTE";

        if (operationRepository.countByCompteBancaire_NumCompteAndCommentaire(numCompte, COMMENT) > 0) {
            logger.info("Frais d'ouverture déjà appliqué pour {}", numCompte);
            return;
        }

        CompteBancaire compte = compteBancaireRepository.findByNumCompte(numCompte)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable: " + numCompte));

        BigDecimal balance = compte.getBalance() == null ? BigDecimal.ZERO : compte.getBalance();
        if (balance.compareTo(new BigDecimal("20.00")) < 0) {
            logger.info("Frais d'ouverture non appliqué (solde < 20€) pour {}", numCompte);
            return;
        }

        compte.debiter(FEE);
        compteBancaireRepository.save(compte);

        Operation op = new Operation();
        op.setCompteBancaire(compte);
        op.setNumeroCompte(compte.getNumCompte());
        op.setTypeOperation(TypeOperation.FRAIS);
        op.setMontant(FEE.negate());
        op.setDescription("Frais d'ouverture de compte");
        op.setCommentaire(COMMENT);
        op.setDateOperation(LocalDateTime.now());
        operationRepository.save(op);

        try {
            FraisDeGestion fg = new FraisDeGestion();
            fg.setClient(compte.getClient());
            fg.setMontant(FEE);
            fg.setDescription("Frais d'ouverture de compte");
            fg.setTypeFrais(FraisDeGestion.TypeFrais.TENUE_COMPTE);
            fg.setPeriodicite(FraisDeGestion.Periodicite.PONCTUEL);
            fg.setDateDebut(LocalDate.now());
            fg.setDateFin(LocalDate.now());
            fg.setEstActif(false);
            fg.setMontantTotalFacture(FEE);
            fg.setDerniereFacturation(LocalDate.now());
            fraisRepository.save(fg);
        } catch (Exception e) {
            logger.warn("Trace frais_de_gestion non créée (non bloquant): {}", e.getMessage());
        }

        logger.info("Frais d'ouverture appliqué sur {}", numCompte);
    }

    private void validerFrais(FraisDeGestion frais) {
        if (frais.getDateDebut().isAfter(frais.getDateFin())) {
            throw new BusinessException("La date de début ne peut pas être postérieure à la date de fin");
        }
        if (frais.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant doit être positif");
        }
        if (frais.getClient() == null) {
            throw new BusinessException("Le client est obligatoire");
        }
    }

    private boolean verifierDoublon(FraisDeGestion frais) {
        if (frais.getTypeFrais() == FraisDeGestion.TypeFrais.TENUE_COMPTE
                || frais.getTypeFrais() == FraisDeGestion.TypeFrais.CARTE_BANCAIRE) {
            List<FraisDeGestion> existants = fraisRepository
                    .findByClientAndTypeFrais(frais.getClient(), frais.getTypeFrais()).stream()
                    .filter(FraisDeGestion::getEstActif)
                    .toList();
            return existants.stream().anyMatch(f -> periodesSeRecoupent(f, frais));
        }
        return false;
    }

    private boolean periodesSeRecoupent(FraisDeGestion a, FraisDeGestion b) {
        return !a.getDateFin().isBefore(b.getDateDebut()) &&
                !b.getDateFin().isBefore(a.getDateDebut());
    }

    private void debiterCompteClientPourFrais(FraisDeGestion frais) {
        Client client = frais.getClient();
        if (client == null || client.getComptes() == null || client.getComptes().isEmpty()) {
            throw new BusinessException("Aucun compte trouvé pour le client");
        }

        CompteBancaire compte = client.getComptes().get(0);

        BigDecimal montant = frais.getMontant();
        if (montant == null || montant.signum() <= 0) {
            throw new BusinessException("Montant de frais invalide");
        }

        compte.debiter(montant);

        Operation op = new Operation();
        op.setCompteBancaire(compte);
        op.setNumeroCompte(compte.getNumCompte());
        op.setTypeOperation(TypeOperation.FRAIS);
        op.setMontant(montant.negate());
        op.setDescription("Frais: " + (frais.getDescription() != null
                ? frais.getDescription()
                : String.valueOf(frais.getTypeFrais())));
        op.setCommentaire("FACTURATION_FRAIS");
        op.setDateOperation(LocalDateTime.now());

        compteBancaireRepository.save(compte);
        operationRepository.save(op);
    }
}