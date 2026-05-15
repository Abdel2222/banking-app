package com.banking.dto.response;



import com.banking.entities.Placement;
import com.banking.entity.enums.StatutPlacement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PlacementResponse {

    private Long id;
    private Long clientId;
    private String clientNomComplet;
    private Long compteBancaireId;
    private String numeroCompte;
    private Long fondsId;
    private String nomFonds;
    private BigDecimal montant;
    private BigDecimal rendementPrevu;
    private BigDecimal gainPrevu;
    private BigDecimal valeurEstimee;
    private LocalDateTime datePlacement;
    private LocalDateTime dateCloture;
    private StatutPlacement statut;

    public PlacementResponse() {
    }

    public PlacementResponse(Placement placement) {
        this.id = placement.getId();
        this.clientId = placement.getClient() != null ? placement.getClient().getId() : null;
        this.clientNomComplet = placement.getClient() != null ? placement.getClient().getNomComplet() : null;
        this.compteBancaireId = placement.getCompteBancaire() != null ? placement.getCompteBancaire().getId() : null;
        this.numeroCompte = placement.getCompteBancaire() != null ? placement.getCompteBancaire().getNumCompte() : null;
        this.fondsId = placement.getFonds() != null ? placement.getFonds().getId() : null;
        this.nomFonds = placement.getFonds() != null ? placement.getFonds().getNomFonds() : null;
        this.montant = placement.getMontant();
        this.rendementPrevu = placement.getRendementPrevu();
        this.gainPrevu = placement.getGainPrevu();
        this.valeurEstimee = placement.getValeurEstimee();
        this.datePlacement = placement.getDatePlacement();
        this.dateCloture = placement.getDateCloture();
        this.statut = placement.getStatut();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getClientNomComplet() {
        return clientNomComplet;
    }

    public void setClientNomComplet(String clientNomComplet) {
        this.clientNomComplet = clientNomComplet;
    }

    public Long getCompteBancaireId() {
        return compteBancaireId;
    }

    public void setCompteBancaireId(Long compteBancaireId) {
        this.compteBancaireId = compteBancaireId;
    }

    public String getNumeroCompte() {
        return numeroCompte;
    }

    public void setNumeroCompte(String numeroCompte) {
        this.numeroCompte = numeroCompte;
    }

    public Long getFondsId() {
        return fondsId;
    }

    public void setFondsId(Long fondsId) {
        this.fondsId = fondsId;
    }

    public String getNomFonds() {
        return nomFonds;
    }

    public void setNomFonds(String nomFonds) {
        this.nomFonds = nomFonds;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public BigDecimal getRendementPrevu() {
        return rendementPrevu;
    }

    public void setRendementPrevu(BigDecimal rendementPrevu) {
        this.rendementPrevu = rendementPrevu;
    }

    public BigDecimal getGainPrevu() {
        return gainPrevu;
    }

    public void setGainPrevu(BigDecimal gainPrevu) {
        this.gainPrevu = gainPrevu;
    }

    public BigDecimal getValeurEstimee() {
        return valeurEstimee;
    }

    public void setValeurEstimee(BigDecimal valeurEstimee) {
        this.valeurEstimee = valeurEstimee;
    }

    public LocalDateTime getDatePlacement() {
        return datePlacement;
    }

    public void setDatePlacement(LocalDateTime datePlacement) {
        this.datePlacement = datePlacement;
    }

    public LocalDateTime getDateCloture() {
        return dateCloture;
    }

    public void setDateCloture(LocalDateTime dateCloture) {
        this.dateCloture = dateCloture;
    }

    public StatutPlacement getStatut() {
        return statut;
    }

    public void setStatut(StatutPlacement statut) {
        this.statut = statut;
    }
}