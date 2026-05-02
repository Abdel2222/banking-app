package com.banking.dto.response;

import com.banking.entities.FraisDeGestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO pour la réponse d'un frais de gestion
 */
public class FraisDeGestionResponse {

    private Long id;
    private BigDecimal montant;
    private String description;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private FraisDeGestion.TypeFrais typeFrais;
    private String typeFraisLibelle;
    private FraisDeGestion.Periodicite periodicite;
    private Boolean estActif;
    private BigDecimal montantTotalFacture;
    private LocalDate derniereFacturation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Informations calculées
    private boolean estEchu;
    private boolean estEnCours;
    private int joursRestants;
    private boolean doitEtreFacture;
    private BigDecimal montantPeriode;

    // Informations client (partielles)
    private Long clientId;
    private String clientNom;
    private String clientEmail;

    // Constructeurs
    public FraisDeGestionResponse() {}

    public FraisDeGestionResponse(FraisDeGestion frais) {
        this.id = frais.getId();
        this.montant = frais.getMontant();
        this.description = frais.getDescription();
        this.dateDebut = frais.getDateDebut();
        this.dateFin = frais.getDateFin();
        this.typeFrais = frais.getTypeFrais();
        this.typeFraisLibelle = frais.getTypeFrais().getLibelle();
        this.periodicite = frais.getPeriodicite();
        this.estActif = frais.getEstActif();
        this.montantTotalFacture = frais.getMontantTotalFacture();
        this.derniereFacturation = frais.getDerniereFacturation();
        this.createdAt = frais.getCreatedAt();
        this.updatedAt = frais.getUpdatedAt();

        // Informations calculées
        this.estEchu = frais.estEchu();
        this.estEnCours = frais.estEnCours();
        this.joursRestants = frais.joursRestants();
        this.doitEtreFacture = frais.doitEtreFacture();
        this.montantPeriode = frais.calculerMontantPeriode();

        // Informations client
        if (frais.getClient() != null) {
            this.clientId = frais.getClient().getId();
            this.clientNom = frais.getClient().getNom() + " " + frais.getClient().getPrenom();
            this.clientEmail = frais.getClient().getEmail();
        }
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public FraisDeGestion.TypeFrais getTypeFrais() { return typeFrais; }
    public void setTypeFrais(FraisDeGestion.TypeFrais typeFrais) { this.typeFrais = typeFrais; }

    public String getTypeFraisLibelle() { return typeFraisLibelle; }
    public void setTypeFraisLibelle(String typeFraisLibelle) { this.typeFraisLibelle = typeFraisLibelle; }

    public FraisDeGestion.Periodicite getPeriodicite() { return periodicite; }
    public void setPeriodicite(FraisDeGestion.Periodicite periodicite) { this.periodicite = periodicite; }

    public Boolean getEstActif() { return estActif; }
    public void setEstActif(Boolean estActif) { this.estActif = estActif; }

    public BigDecimal getMontantTotalFacture() { return montantTotalFacture; }
    public void setMontantTotalFacture(BigDecimal montantTotalFacture) { this.montantTotalFacture = montantTotalFacture; }

    public LocalDate getDerniereFacturation() { return derniereFacturation; }
    public void setDerniereFacturation(LocalDate derniereFacturation) { this.derniereFacturation = derniereFacturation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isEstEchu() { return estEchu; }
    public void setEstEchu(boolean estEchu) { this.estEchu = estEchu; }

    public boolean isEstEnCours() { return estEnCours; }
    public void setEstEnCours(boolean estEnCours) { this.estEnCours = estEnCours; }

    public int getJoursRestants() { return joursRestants; }
    public void setJoursRestants(int joursRestants) { this.joursRestants = joursRestants; }

    public boolean isDoitEtreFacture() { return doitEtreFacture; }
    public void setDoitEtreFacture(boolean doitEtreFacture) { this.doitEtreFacture = doitEtreFacture; }

    public BigDecimal getMontantPeriode() { return montantPeriode; }
    public void setMontantPeriode(BigDecimal montantPeriode) { this.montantPeriode = montantPeriode; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
}