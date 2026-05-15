package com.banking.services;

import com.banking.dto.request.CreatePlacementRequest;
import com.banking.dto.response.PlacementResponse;
import com.banking.entities.Client;
import com.banking.entities.CompteBancaire;
import com.banking.entities.Fonds;
import com.banking.entities.Placement;
import com.banking.entity.enums.StatutPlacement;
import com.banking.exceptions.BusinessException;
import com.banking.exceptions.ResourceNotFoundException;
import com.banking.repositories.CompteBancaireRepository;
import com.banking.repositories.FondsRepository;
import com.banking.repositories.PlacementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PlacementService {

    private final PlacementRepository placementRepository;
    private final CompteBancaireRepository compteBancaireRepository;
    private final FondsRepository fondsRepository;

    public PlacementService(
            PlacementRepository placementRepository,
            CompteBancaireRepository compteBancaireRepository,
            FondsRepository fondsRepository
    ) {
        this.placementRepository = placementRepository;
        this.compteBancaireRepository = compteBancaireRepository;
        this.fondsRepository = fondsRepository;
    }

    @Transactional
    public PlacementResponse creerPlacement(CreatePlacementRequest request) {
        CompteBancaire compte = compteBancaireRepository.findById(request.getCompteBancaireId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Compte bancaire introuvable avec l'id : " + request.getCompteBancaireId()
                ));

        Client client = compte.getClient();

        if (client == null) {
            throw new BusinessException("Ce compte bancaire n'est associé à aucun client");
        }

        Fonds fonds = fondsRepository.findById(request.getFondsId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fonds introuvable avec l'id : " + request.getFondsId()
                ));
        System.out.println("DEBUG PLACEMENT compteId=" + compte.getId());
        System.out.println("DEBUG PLACEMENT compteStatus=" + compte.getStatus());
        System.out.println("DEBUG PLACEMENT compteIsActive=" + compte.isActive());
        System.out.println("DEBUG PLACEMENT clientId=" + client.getId());
        System.out.println("DEBUG PLACEMENT fondsId=" + fonds.getId());
        System.out.println("DEBUG PLACEMENT fondsActif=" + fonds.getEstActif());
        System.out.println("DEBUG PLACEMENT montant=" + request.getMontant());
        System.out.println("DEBUG PLACEMENT balance=" + compte.getBalance());

        verifierReglesMetier(client, compte, fonds, request.getMontant());

        BigDecimal nouveauSolde = compte.getBalance().subtract(request.getMontant());
        compte.setBalance(nouveauSolde);

        Placement placement = new Placement(client, compte, fonds, request.getMontant());

        compteBancaireRepository.save(compte);
        Placement saved = placementRepository.save(placement);
        System.out.println("DEBUG PLACEMENT savedId=" + saved.getId());

        return new PlacementResponse(saved);
    }

    private void verifierReglesMetier(
            Client client,
            CompteBancaire compte,
            Fonds fonds,
            BigDecimal montant
    ) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant du placement doit être supérieur à 0");
        }

        if (!fonds.estDisponible()) {
            throw new BusinessException("Ce fonds n'est pas disponible actuellement");
        }

        if (!fonds.montantRespecteMinimum(montant)) {
            throw new BusinessException("Le montant du placement est inférieur au montant minimum du fonds");
        }

        if (compte.getClient() == null ||
                compte.getClient().getId() == null ||
                !compte.getClient().getId().equals(client.getId())) {
            throw new BusinessException("Ce compte bancaire n'appartient pas à ce client");
        }

        if (!compte.isActive()) {
            throw new BusinessException("Le compte bancaire n'est pas actif");
        }

        if (compte.getBalance() == null || compte.getBalance().compareTo(montant) < 0) {
            throw new BusinessException("Solde insuffisant pour effectuer ce placement");
        }
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
        Placement placement = placementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Placement introuvable avec l'id : " + id
                ));

        return new PlacementResponse(placement);
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
        Placement placement = placementRepository.findById(placementId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Placement introuvable avec l'id : " + placementId
                ));

        if (!placement.estActif()) {
            throw new BusinessException("Ce placement n'est pas actif");
        }

        CompteBancaire compte = placement.getCompteBancaire();

        BigDecimal montantARembourser = placement.getValeurEstimee();
        compte.setBalance(compte.getBalance().add(montantARembourser));

        placement.cloturer();

        compteBancaireRepository.save(compte);
        Placement saved = placementRepository.save(placement);

        return new PlacementResponse(saved);
    }

    @Transactional
    public PlacementResponse annulerPlacement(Long placementId) {
        Placement placement = placementRepository.findById(placementId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Placement introuvable avec l'id : " + placementId
                ));

        if (!placement.estActif()) {
            throw new BusinessException("Ce placement n'est pas actif");
        }

        CompteBancaire compte = placement.getCompteBancaire();
        compte.setBalance(compte.getBalance().add(placement.getMontant()));

        placement.annuler();

        compteBancaireRepository.save(compte);
        Placement saved = placementRepository.save(placement);

        return new PlacementResponse(saved);
    }
}