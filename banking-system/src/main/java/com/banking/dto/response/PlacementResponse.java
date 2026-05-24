package com.banking.dto.response;

import com.banking.entities.Placement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PlacementResponse {

    private Long id;
    private String type;
    private String codeIdentification;
    private String nomFonds;
    private String codeFonds;
    private String numeroCompte;
    private String clientNomComplet;
    private BigDecimal montant;
    private BigDecimal rendement;
    private BigDecimal gainPrevu;
    private BigDecimal valeurEstimee;
    private BigDecimal fraisSortie;
    private LocalDateTime datePlacement;
    private LocalDateTime dateCloture;
    private LocalDateTime dateSortie;
    private String statut;

    public PlacementResponse() {}

    public PlacementResponse(Placement p) {
        this.id = p.getId();
        this.type = p.getType();
        this.codeIdentification = p.getCodeIdentification();
        this.nomFonds = p.getFonds() != null ? p.getFonds().getNomFonds() : null;
        this.codeFonds = p.getFonds() != null ? p.getFonds().getCodeIdentification() : null;
        this.numeroCompte = p.getCompteBancaire() != null ? p.getCompteBancaire().getNumCompte() : null;
        this.clientNomComplet = p.getClient() != null
                ? p.getClient().getPrenom() + " " + p.getClient().getNom()
                : null;
        this.montant = p.getMontant();
        this.rendement = p.getRendement();              // ⭐ renommé
        this.gainPrevu = p.getGainPrevu();              // toujours dispo (méthode calculée)
        this.valeurEstimee = p.getValeurEstimee();
        this.fraisSortie = p.getFraisSortie();
        this.datePlacement = p.getDatePlacement();
        this.dateCloture = p.getDateCloture();
        this.dateSortie = p.getDateSortie();
        this.statut = p.getStatut() != null ? p.getStatut().name() : null;
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getCodeIdentification() { return codeIdentification; }
    public String getNomFonds() { return nomFonds; }
    public String getCodeFonds() { return codeFonds; }
    public String getNumeroCompte() { return numeroCompte; }
    public String getClientNomComplet() { return clientNomComplet; }
    public BigDecimal getMontant() { return montant; }
    public BigDecimal getRendement() { return rendement; }
    public BigDecimal getGainPrevu() { return gainPrevu; }
    public BigDecimal getValeurEstimee() { return valeurEstimee; }
    public BigDecimal getFraisSortie() { return fraisSortie; }
    public LocalDateTime getDatePlacement() { return datePlacement; }
    public LocalDateTime getDateCloture() { return dateCloture; }
    public LocalDateTime getDateSortie() { return dateSortie; }
    public String getStatut() { return statut; }
}