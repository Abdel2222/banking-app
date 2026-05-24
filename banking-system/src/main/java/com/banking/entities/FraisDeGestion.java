package com.banking.entities;

import com.banking.entity.enums.Periodicite;
import com.banking.entity.enums.TypeFrais;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "frais_de_gestion")
public class FraisDeGestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "montant", precision = 19, scale = 4, nullable = false)
    private BigDecimal montant;

    @Column(name = "montant_total_facture", precision = 19, scale = 4)
    private BigDecimal montantTotalFacture = BigDecimal.ZERO;

    @Column(name = "description")
    private String description;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "derniere_facturation")
    private LocalDate derniereFacturation;

    @Column(name = "est_actif")
    private Boolean estActif = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ✅ Pas en DB — uniquement utilisés dans le code Java
    @Transient
    private TypeFrais typeFrais;

    @Transient
    private Periodicite periodicite;

    // ===== Relations =====

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonBackReference("client-frais")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_bancaire_id")
    @JsonBackReference("compte-frais")
    private CompteBancaire compteBancaire;

    // ===== Constructeurs =====

    public FraisDeGestion() {}

    public FraisDeGestion(BigDecimal montant, LocalDate dateDebut,
                          Client client, CompteBancaire compte) {
        this.montant             = montant;
        this.dateDebut           = dateDebut;
        this.client              = client;
        this.compteBancaire      = compte;
        this.estActif            = true;
        this.montantTotalFacture = BigDecimal.ZERO;
    }

    // ===== Hooks JPA =====

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (estActif            == null) estActif            = true;
        if (montantTotalFacture == null) montantTotalFacture = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== Méthodes métier =====

    public boolean estEnCours() {
        LocalDate today = LocalDate.now();
        return Boolean.TRUE.equals(estActif)
                && dateDebut != null
                && !today.isBefore(dateDebut)
                && (dateFin == null || !today.isAfter(dateFin));
    }

    public boolean estEchu() {
        return dateFin != null && LocalDate.now().isAfter(dateFin);
    }

    public boolean doitEtreFacture() {
        return Boolean.TRUE.equals(estActif) && !estEchu();
    }

    public void facturer() {
        this.montantTotalFacture = (this.montantTotalFacture == null ? BigDecimal.ZERO : this.montantTotalFacture)
                .add(this.montant);
        this.derniereFacturation = LocalDate.now();
    }

    public void reactiver() {
        this.estActif = true;
    }

    public void desactiver() {
        this.estActif = false;
    }

    // ===== Getters / Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public BigDecimal getMontantTotalFacture() {
        return montantTotalFacture == null ? BigDecimal.ZERO : montantTotalFacture;
    }
    public void setMontantTotalFacture(BigDecimal v) { this.montantTotalFacture = v; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public LocalDate getDerniereFacturation() { return derniereFacturation; }
    public void setDerniereFacturation(LocalDate v) { this.derniereFacturation = v; }

    public Boolean getEstActif() { return estActif; }
    public void setEstActif(Boolean estActif) { this.estActif = estActif; }

    public TypeFrais getTypeFrais() { return typeFrais; }
    public void setTypeFrais(TypeFrais typeFrais) { this.typeFrais = typeFrais; }

    public Periodicite getPeriodicite() { return periodicite; }
    public void setPeriodicite(Periodicite periodicite) { this.periodicite = periodicite; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public CompteBancaire getCompteBancaire() { return compteBancaire; }
    public void setCompteBancaire(CompteBancaire c) { this.compteBancaire = c; }

    // ===== toString / equals / hashCode =====

    @Override
    public String toString() {
        return "FraisDeGestion{id=" + id +
                ", montant=" + montant +
                ", description=" + description +
                ", estActif=" + estActif + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FraisDeGestion other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}