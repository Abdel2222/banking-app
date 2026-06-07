package com.banking.services;

import com.banking.dto.request.CreatePlacementRequest;
import com.banking.dto.response.PlacementResponse;
import com.banking.entities.Client;
import com.banking.entities.CompteBancaire;
import com.banking.entities.Fonds;
import com.banking.entities.Operation;
import com.banking.entities.Placement;
import com.banking.entity.enums.StatutPlacement;
import com.banking.entity.enums.TypeOperation;
import com.banking.exceptions.BusinessException;
import com.banking.exceptions.ResourceNotFoundException;
import com.banking.repositories.CompteBancaireRepository;
import com.banking.repositories.FondsRepository;
import com.banking.repositories.OperationRepository;
import com.banking.repositories.PlacementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PlacementService {

    private final PlacementRepository        placementRepository;
    private final CompteBancaireRepository   compteBancaireRepository;
    private final FondsRepository            fondsRepository;
    private final OperationRepository        operationRepository;

    public PlacementService(
            PlacementRepository placementRepository,
            CompteBancaireRepository compteBancaireRepository,
            FondsRepository fondsRepository,
            OperationRepository operationRepository
    ) {
        this.placementRepository      = placementRepository;
        this.compteBancaireRepository = compteBancaireRepository;
        this.fondsRepository          = fondsRepository;
        this.operationRepository      = operationRepository;
    }

    @Transactional
    public PlacementResponse creerPlacement(CreatePlacementRequest request) {
        CompteBancaire compte = compteBancaireRepository.findById(request.getCompteBancaireId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Compte bancaire introuvable avec l'id : " + request.getCompteBancaireId()
                ));

        Client client = compte.getClient();
        if (client == null)
            throw new BusinessException("Ce compte bancaire n'est associé à aucun client");

        Fonds fonds = fondsRepository.findById(request.getFondsId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fonds introuvable avec l'id : " + request.getFondsId()
                ));

        verifierReglesMetier(client, compte, fonds, request.getMontant());

        BigDecimal montant = request.getMontant();
        compte.setBalance(compte.getBalance().subtract(montant));

        Placement placement = new Placement(client, compte, fonds, montant);

        compteBancaireRepository.save(compte);
        Placement saved = placementRepository.save(placement);

        Operation op = new Operation(
                compte,
                montant,
                TypeOperation.PLACEMENT,
                "Placement dans le fonds " + fonds.getNomFonds()
        );
        op.setCommunication("PLACEMENT");
        operationRepository.save(op);

        return new PlacementResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PlacementResponse> getTousLesPlacements() {
        return placementRepository.findAll()
                .stream()
                .map(PlacementResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlacementResponse getPlacementById(Long id) {
        return new PlacementResponse(
                placementRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Placement introuvable avec l'id : " + id
                        ))
        );
    }

    @Transactional(readOnly = true)
    public List<PlacementResponse> getPlacementsByClient(Long clientId) {
        return placementRepository.findByClientId(clientId)
                .stream()
                .map(PlacementResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlacementResponse> getPlacementsActifsByClient(Long clientId) {
        return placementRepository.findByClientIdAndStatut(clientId, StatutPlacement.ACTIF)
                .stream()
                .map(PlacementResponse::new)
                .toList();
    }

    @Transactional
    public PlacementResponse cloturerPlacement(Long placementId) {
        Placement placement = getActifOrThrow(placementId);
        CompteBancaire compte = placement.getCompteBancaire();

        BigDecimal retour = placement.getValeurEstimee();
        compte.setBalance(compte.getBalance().add(retour));

        placement.cloturer();

        compteBancaireRepository.save(compte);
        Placement saved = placementRepository.save(placement);

        Operation op = new Operation(
                compte,
                retour,
                TypeOperation.CLOTURE_PLACEMENT,
                "Clôture du placement " + placement.getFonds().getNomFonds()
                        + " | Capital + gain : " + retour + " €"
        );
        op.setCommunication("CLOTURE_PLACEMENT");
        operationRepository.save(op);

        return new PlacementResponse(saved);
    }

    @Transactional
    public PlacementResponse sortirAvantEcheance(Long placementId) {
        Placement placement = getActifOrThrow(placementId);
        CompteBancaire compte = placement.getCompteBancaire();

        BigDecimal frais    = placement.calculerFraisSortie();
        BigDecimal interets = placement.getInteretsCourus();
        BigDecimal retour   = placement.getMontant()
                .add(interets)
                .subtract(frais);

        if (retour.signum() < 0) retour = BigDecimal.ZERO;

        compte.setBalance(compte.getBalance().add(retour));
        placement.sortirAvantEcheance();

        compteBancaireRepository.save(compte);
        Placement saved = placementRepository.save(placement);

        Operation opRetour = new Operation(
                compte,
                retour,
                TypeOperation.SORTIE_ANTICIPEE,
                "Sortie anticipée : " + placement.getFonds().getNomFonds()
                        + " | Capital : " + placement.getMontant() + " €"
                        + " | Intérêts courus : " + interets + " €"
                        + " | Frais : " + frais + " €"
                        + " | Restitué : " + retour + " €"
        );
        opRetour.setCommunication("SORTIE_ANTICIPEE");
        operationRepository.save(opRetour);

        if (frais.signum() > 0) {
            Operation opFrais = new Operation(
                    compte,
                    frais,
                    TypeOperation.FRAIS_SORTIE,
                    "Frais de sortie anticipée : " + placement.getFonds().getNomFonds()
            );
            opFrais.setCommunication("FRAIS_SORTIE");
            operationRepository.save(opFrais);
        }

        return new PlacementResponse(saved);
    }

    @Transactional
    public PlacementResponse annulerPlacement(Long placementId) {
        Placement placement = getActifOrThrow(placementId);
        CompteBancaire compte = placement.getCompteBancaire();

        BigDecimal montant = placement.getMontant();
        compte.setBalance(compte.getBalance().add(montant));

        placement.annuler();

        compteBancaireRepository.save(compte);
        Placement saved = placementRepository.save(placement);

        Operation op = new Operation(
                compte,
                montant,
                TypeOperation.ANNULATION_PLACEMENT,
                "Annulation du placement " + placement.getFonds().getNomFonds()
                        + " | Capital remboursé : " + montant + " €"
        );
        op.setCommunication("ANNULATION_PLACEMENT");
        operationRepository.save(op);

        return new PlacementResponse(saved);
    }

    private Placement getActifOrThrow(Long id) {
        Placement placement = placementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Placement introuvable avec l'id : " + id
                ));
        if (!placement.estActif())
            throw new BusinessException("Ce placement n'est pas actif");
        return placement;
    }

    private void verifierReglesMetier(
            Client client,
            CompteBancaire compte,
            Fonds fonds,
            BigDecimal montant
    ) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException("Le montant du placement doit être supérieur à 0");

        // ✅ Supprimé — plus de montant minimum requis par fonds
        // if (!fonds.montantRespecteMinimum(montant))
        //     throw new BusinessException(...);

        if (compte.getClient() == null
                || compte.getClient().getId() == null
                || !compte.getClient().getId().equals(client.getId()))
            throw new BusinessException("Ce compte bancaire n'appartient pas à ce client");

        if (!compte.isActive())
            throw new BusinessException("Le compte bancaire n'est pas actif");

        if (compte.getBalance() == null || compte.getBalance().compareTo(montant) < 0)
            throw new BusinessException("Solde insuffisant pour effectuer ce placement");
    }
}