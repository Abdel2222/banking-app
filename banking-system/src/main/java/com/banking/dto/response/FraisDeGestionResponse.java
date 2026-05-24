package com.banking.dto.response;

import com.banking.entities.FraisDeGestion;
import com.banking.entity.enums.Periodicite;  // ✅ corrigé
import com.banking.entity.enums.TypeFrais;     // ✅ corrigé

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class FraisDeGestionResponse {

    private Long id;
    private BigDecimal montant;
    private String description;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private TypeFrais typeFrais;           // ✅ corrigé
    private String typeFraisLibelle;
    private Periodicite periodicite;       // ✅ corrigé
    private Boolean estActif;
    private BigDecimal montantTotalFacture;
    private LocalDate derniereFacturation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Informations calculées
    private boolean estEchu;
    private boolean estEnCours;
    private long joursRestants;            // ✅ long au lieu de int
    private boolean doitEtreFacture;
    private BigDecimal montantPeriode;

    // Informations client
    private Long clientId;
    private String clientNom;
    private String clientEmail;

    // ===== Constructeurs =====

    public FraisDeGestionResponse() {}

    public FraisDeGestionResponse(FraisDeGestion frais) {
        this.id                  = frais.getId();
        this.montant             = frais.getMontant();
        this.description         = frais.getDescription();
        this.dateDebut           = frais.getDateDebut();
        this.dateFin             = frais.getDateFin();
        this.typeFrais           = frais.getTypeFrais();
        this.periodicite         = frais.getPeriodicite();
        this.estActif            = frais.getEstActif();
        this.montantTotalFacture = frais.getMontantTotalFacture();
        this.derniereFacturation = frais.getDerniereFacturation();
        this.createdAt           = frais.getCreatedAt();
        this.updatedAt           = frais.getUpdatedAt();

        // ✅ libelle — calculé ici au lieu d'une méthode inexistante dans l'entité
        this.typeFraisLibelle = frais.getTypeFrais() != null
                ? frais.getTypeFrais().name() : null;

        // ✅ Informations calculées — méthodes existantes dans l'entité
        this.estEchu        = frais.estEchu();
        this.estEnCours     = frais.estEnCours();
        this.doitEtreFacture = frais.doitEtreFacture();

        // ✅ joursRestants — calculé ici (pas de méthode dans l'entité)
        this.joursRestants = frais.getDateFin() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), frais.getDateFin())
                : 0;

        // ✅ montantPeriode — calculé ici (pas de méthode dans l'entité)
        this.montantPeriode = frais.getMontant() != null ? frais.getMontant() : BigDecimal.ZERO;

        // Informations client
        if (frais.getClient() != null) {
            this.clientId    = frais.getClient().getId();
            this.clientNom   = frais.getClient().getNom() + " " + frais.getClient().getPrenom();
            this.clientEmail = frais.getClient().getEmail();
        }
    }

    // ===== Getters / Setters =====

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

    public TypeFrais getTypeFrais() { return typeFrais; }
    public void setTypeFrais(TypeFrais typeFrais) { this.typeFrais = typeFrais; }

    public String getTypeFraisLibelle() { return typeFraisLibelle; }
    public void setTypeFraisLibelle(String typeFraisLibelle) { this.typeFraisLibelle = typeFraisLibelle; }

    public Periodicite getPeriodicite() { return periodicite; }
    public void setPeriodicite(Periodicite periodicite) { this.periodicite = periodicite; }

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

    public long getJoursRestants() { return joursRestants; }
    public void setJoursRestants(long joursRestants) { this.joursRestants = joursRestants; }

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