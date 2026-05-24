package com.banking.services.impl;

import com.banking.entities.Client;
import com.banking.entities.CompteBancaire;
import com.banking.entities.FraisDeGestion;
import com.banking.entities.Operation;
import com.banking.entity.enums.Periodicite;
import com.banking.entity.enums.TypeFrais;
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

    @Autowired private FraisDeGestionRepository fraisRepository;
    @Autowired private ClientService            clientService;
    @Autowired private OperationRepository      operationRepository;
    @Autowired private CompteBancaireRepository compteBancaireRepository;

    // ===================== CRUD =====================

    @Override
    public FraisDeGestion creerFrais(FraisDeGestion frais) {
        logger.info("Création frais client ID: {}",
                frais.getClient() != null ? frais.getClient().getId() : null);
        validerFrais(frais);
        if (verifierDoublon(frais))
            throw new BusinessException("Un frais de ce type existe déjà pour cette période");
        FraisDeGestion fraisCree = fraisRepository.save(frais);
        logger.info("Frais créé ID: {}", fraisCree.getId());
        return fraisCree;
    }

    @Override
    @Transactional(readOnly = true)
    public FraisDeGestion obtenirFrais(Long id) {
        return fraisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Frais non trouvé ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraisDeGestion> obtenirTousFrais() {
        return fraisRepository.findAll();
    }

    @Override
    public FraisDeGestion mettreAJourFrais(Long id, FraisDeGestion fraisModifie) {
        FraisDeGestion fraisExistant = obtenirFrais(id);
        if (fraisModifie.getMontant()     != null) fraisExistant.setMontant(fraisModifie.getMontant());
        if (fraisModifie.getDescription() != null) fraisExistant.setDescription(fraisModifie.getDescription());
        if (fraisModifie.getDateFin()     != null) fraisExistant.setDateFin(fraisModifie.getDateFin());
        if (fraisModifie.getPeriodicite() != null) fraisExistant.setPeriodicite(fraisModifie.getPeriodicite());
        validerFrais(fraisExistant);
        return fraisRepository.save(fraisExistant);
    }

    @Override
    public void supprimerFrais(Long id) {
        FraisDeGestion frais = obtenirFrais(id);
        if (frais.getMontantTotalFacture().compareTo(BigDecimal.ZERO) > 0)
            throw new BusinessException("Impossible de supprimer un frais déjà facturé.");
        fraisRepository.delete(frais);
    }

    // ===================== Frais par client =====================

    @Override
    @Transactional(readOnly = true)
    public List<FraisDeGestion> obtenirFraisClient(Long clientId) {
        return fraisRepository.findByClient(getClientOrThrow(clientId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraisDeGestion> obtenirFraisActifsClient(Long clientId) {
        return fraisRepository.findByClient(getClientOrThrow(clientId)).stream()
                .filter(FraisDeGestion::getEstActif).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraisDeGestion> obtenirFraisEnCoursClient(Long clientId) {
        return fraisRepository.findByClient(getClientOrThrow(clientId)).stream()
                .filter(FraisDeGestion::estEnCours).toList();
    }

    @Override
    public FraisDeGestion creerFraisClient(Long clientId, FraisDeGestion frais) {
        frais.setClient(getClientOrThrow(clientId));
        return creerFrais(frais);
    }

    // ===================== Facturation =====================

    @Override
    public List<FraisDeGestion> obtenirFraisAFacturer() {
        return fraisRepository.findAll().stream()
                .filter(FraisDeGestion::doitEtreFacture).toList();
    }

    @Override
    public void facturerFrais(Long fraisId) {
        FraisDeGestion frais = obtenirFrais(fraisId);
        if (!frais.doitEtreFacture())
            throw new BusinessException("Ce frais ne peut pas être facturé maintenant");
        debiterCompteClientPourFrais(frais);
        frais.facturer();
        fraisRepository.save(frais);
        logger.info("Frais {} facturé. Montant: {}", fraisId, frais.getMontant());
    }

    @Override
    public void facturerFraisClient(Long clientId) {
        fraisRepository.findByClient(getClientOrThrow(clientId)).stream()
                .filter(FraisDeGestion::doitEtreFacture)
                .forEach(frais -> {
                    try { facturerFrais(frais.getId()); }
                    catch (Exception e) { logger.error("Erreur facturation frais {}", frais.getId(), e); }
                });
    }

    @Override
    @Scheduled(cron = "0 0 6 * * *")
    public void facturationAutomatique() {
        logger.info("Facturation automatique démarrée");
        obtenirFraisAFacturer().forEach(f -> {
            try { facturerFrais(f.getId()); }
            catch (Exception e) { logger.error("Echec frais {}", f.getId(), e); }
        });
    }

    // ===================== Activation =====================

    @Override
    public void activerFrais(Long fraisId) {
        FraisDeGestion frais = obtenirFrais(fraisId);
        frais.reactiver();
        fraisRepository.save(frais);
    }

    @Override
    public void desactiverFrais(Long fraisId) {
        FraisDeGestion frais = obtenirFrais(fraisId);
        frais.desactiver();
        fraisRepository.save(frais);
    }

    @Override
    @Scheduled(cron = "0 30 6 * * *")
    public void desactiverFraisExpires() {
        fraisRepository.findAll().stream()
                .filter(f -> f.getEstActif() && f.estEchu())
                .forEach(f -> { f.desactiver(); fraisRepository.save(f); });
    }

    // ===================== Calculs =====================

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculerMontantTotalActifClient(Long clientId) {
        return fraisRepository.findByClient(getClientOrThrow(clientId)).stream()
                .filter(f -> f.getEstActif() && f.estEnCours())
                .map(FraisDeGestion::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculerMontantTotalFactureClient(Long clientId) {
        return fraisRepository.findByClient(getClientOrThrow(clientId)).stream()
                .map(FraisDeGestion::getMontantTotalFacture)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public Long compterFraisActifsClient(Long clientId) {
        return fraisRepository.findByClient(getClientOrThrow(clientId)).stream()
                .filter(FraisDeGestion::getEstActif).count();
    }

    // ===================== Recherche =====================

    @Override
    @Transactional(readOnly = true)
    public List<FraisDeGestion> rechercherFrais(TypeFrais typeFrais,
                                                Periodicite periodicite,
                                                Boolean estActif,
                                                LocalDate dateDebut,
                                                LocalDate dateFin) {
        return fraisRepository.findAll().stream()
                .filter(f -> typeFrais   == null || typeFrais.equals(f.getTypeFrais()))
                .filter(f -> periodicite == null || periodicite.equals(f.getPeriodicite()))
                .filter(f -> estActif    == null || estActif.equals(f.getEstActif()))
                .filter(f -> dateDebut   == null || !f.getDateDebut().isBefore(dateDebut))
                .filter(f -> dateFin     == null || f.getDateFin() == null || !f.getDateFin().isAfter(dateFin))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraisDeGestion> obtenirHistoriqueFacturationClient(Long clientId) {
        return fraisRepository.findByClient(getClientOrThrow(clientId)).stream()
                .filter(f -> f.getDerniereFacturation() != null)
                .sorted(Comparator.comparing(FraisDeGestion::getDerniereFacturation).reversed())
                .toList();
    }

    // ===================== Frais ouverture =====================

    @Override
    public void appliquerFraisOuverture(String numCompte) {
        final BigDecimal FEE = new BigDecimal("2.00");
        final String COMMENT = "FRAIS_OUVERTURE_COMPTE";

        if (operationRepository.countByCompteBancaire_NumCompteAndCommentaire(numCompte, COMMENT) > 0) {
            logger.info("Frais ouverture déjà appliqué pour {}", numCompte);
            return;
        }

        CompteBancaire compte = compteBancaireRepository.findByNumCompte(numCompte)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable: " + numCompte));

        if (compte.getBalance().compareTo(new BigDecimal("20.00")) < 0) {
            logger.info("Solde insuffisant pour frais ouverture sur {}", numCompte);
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
            fg.setTypeFrais(TypeFrais.TENUE_COMPTE);
            fg.setPeriodicite(Periodicite.PONCTUELLE);
            fg.setDateDebut(LocalDate.now());
            fg.setDateFin(LocalDate.now());
            fg.setEstActif(false);
            fg.setMontantTotalFacture(FEE);
            fg.setDerniereFacturation(LocalDate.now());
            fraisRepository.save(fg);
        } catch (Exception e) {
            logger.warn("Trace frais_de_gestion non créée: {}", e.getMessage());
        }

        logger.info("Frais ouverture appliqué sur {}", numCompte);
    }

    // ===================== Helpers privés =====================

    private Client getClientOrThrow(Long clientId) {
        return clientService.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé ID: " + clientId));
    }

    private void validerFrais(FraisDeGestion frais) {
        if (frais.getClient() == null)
            throw new BusinessException("Le client est obligatoire");
        if (frais.getMontant() == null || frais.getMontant().compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException("Le montant doit être positif");
        if (frais.getDateDebut() != null && frais.getDateFin() != null
                && frais.getDateDebut().isAfter(frais.getDateFin()))
            throw new BusinessException("La date de début ne peut pas être postérieure à la date de fin");
    }

    private boolean verifierDoublon(FraisDeGestion frais) {
        TypeFrais type = frais.getTypeFrais();
        if (type == TypeFrais.TENUE_COMPTE || type == TypeFrais.CARTE_BANCAIRE) {
            // ✅ Filtre Java stream — findByClientAndTypeFrais supprimé du repository
            return fraisRepository.findByClient(frais.getClient()).stream()
                    .filter(f -> type.equals(f.getTypeFrais()))
                    .filter(FraisDeGestion::getEstActif)
                    .anyMatch(f -> periodesSeRecoupent(f, frais));
        }
        return false;
    }

    private boolean periodesSeRecoupent(FraisDeGestion a, FraisDeGestion b) {
        if (a.getDateFin() == null || b.getDateFin() == null) return true;
        return !a.getDateFin().isBefore(b.getDateDebut()) &&
                !b.getDateFin().isBefore(a.getDateDebut());
    }

    private void debiterCompteClientPourFrais(FraisDeGestion frais) {
        Client client = frais.getClient();
        if (client == null || client.getComptes().isEmpty())
            throw new BusinessException("Aucun compte trouvé pour le client");

        CompteBancaire compte = client.getComptes().get(0);
        BigDecimal montant = frais.getMontant();

        compte.debiter(montant);

        Operation op = new Operation();
        op.setCompteBancaire(compte);
        op.setNumeroCompte(compte.getNumCompte());
        op.setTypeOperation(TypeOperation.FRAIS);
        op.setMontant(montant.negate());
        op.setDescription("Frais: " + (frais.getDescription() != null
                ? frais.getDescription() : String.valueOf(frais.getTypeFrais())));
        op.setCommentaire("FACTURATION_FRAIS");
        op.setDateOperation(LocalDateTime.now());

        compteBancaireRepository.save(compte);
        operationRepository.save(op);
    }
}
