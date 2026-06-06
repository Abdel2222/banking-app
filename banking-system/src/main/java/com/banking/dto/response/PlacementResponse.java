package com.banking.dto.response;

import com.banking.entities.CompteBancaire;
import com.banking.entities.CompteEpargne;
import com.banking.entities.Placement;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PlacementResponse {

    private Long          id;
    private String        type;
    private String        codeIdentification;
    private String        nomFonds;
    private String        codeFonds;
    private String        numeroCompte;
    private String        typeCompte;
    private String        clientNomComplet;
    private BigDecimal    montant;
    private BigDecimal    rendement;
    private BigDecimal    gainPrevu;
    private BigDecimal    valeurEstimee;
    private BigDecimal    fraisSortie;
    private BigDecimal    interetsCourus;
    private LocalDateTime datePlacement;
    private LocalDateTime dateCloture;
    private LocalDateTime dateSortie;
    private String        statut;

    public PlacementResponse() {}

    public PlacementResponse(Placement p) {
        this.id                 = p.getId();
        this.type               = p.getType();
        this.codeIdentification = p.getCodeIdentification();
        this.nomFonds           = p.getFonds() != null ? p.getFonds().getNomFonds()           : null;
        this.codeFonds          = p.getFonds() != null ? p.getFonds().getCodeIdentification() : null;

        if (p.getCompteBancaire() != null) {
            // ✅ Déproxifie pour résoudre le vrai sous-type JPA (JOINED inheritance)
            CompteBancaire cb = (CompteBancaire) Hibernate.unproxy(p.getCompteBancaire());
            this.numeroCompte = cb.getNumCompte();
            this.typeCompte   = (cb instanceof CompteEpargne) ? "EPARGNE" : "COURANT";
        }

        this.clientNomComplet = p.getClient() != null
                ? p.getClient().getPrenom() + " " + p.getClient().getNom()
                : null;

        this.montant        = p.getMontant();
        this.rendement      = p.getRendement();
        this.gainPrevu      = p.getGainPrevu();
        this.valeurEstimee  = p.getValeurEstimee();
        this.fraisSortie    = p.getFraisSortie();
        this.interetsCourus = p.getInteretsCourus();
        this.datePlacement  = p.getDatePlacement();
        this.dateCloture    = p.getDateCloture();
        this.dateSortie     = p.getDateSortie();
        this.statut         = p.getStatut() != null ? p.getStatut().name() : null;
    }

    /* ====== Getters ====== */

    public Long          getId()                 { return id; }
    public String        getType()               { return type; }
    public String        getCodeIdentification() { return codeIdentification; }
    public String        getNomFonds()           { return nomFonds; }
    public String        getCodeFonds()          { return codeFonds; }
    public String        getNumeroCompte()       { return numeroCompte; }
    public String        getTypeCompte()         { return typeCompte; }
    public String        getClientNomComplet()   { return clientNomComplet; }
    public BigDecimal    getMontant()            { return montant; }
    public BigDecimal    getRendement()          { return rendement; }
    public BigDecimal    getGainPrevu()          { return gainPrevu; }
    public BigDecimal    getValeurEstimee()      { return valeurEstimee; }
    public BigDecimal    getFraisSortie()        { return fraisSortie; }
    public BigDecimal    getInteretsCourus()     { return interetsCourus; }
    public LocalDateTime getDatePlacement()      { return datePlacement; }
    public LocalDateTime getDateCloture()        { return dateCloture; }
    public LocalDateTime getDateSortie()         { return dateSortie; }
    public String        getStatut()             { return statut; }
}