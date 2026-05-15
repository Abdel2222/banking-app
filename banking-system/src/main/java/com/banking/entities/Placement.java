package com.banking.entities;

import com.banking.entity.enums.StatutPlacement;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "placements")
public class Placement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Le client est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @NotNull(message = "Le compte bancaire est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_bancaire_id", nullable = false)
    private CompteBancaire compteBancaire;

    @NotNull(message = "Le fonds est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fonds_id", nullable = false)
    private Fonds fonds;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "1.00", message = "Le montant du placement doit être supérieur à 0")
    @Column(name = "montant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(name = "rendement_prevu", precision = 5, scale = 2)
    private BigDecimal rendementPrevu;

    @Column(name = "gain_prevu", precision = 15, scale = 2)
    private BigDecimal gainPrevu;

    @Column(name = "date_placement", nullable = false)
    private LocalDateTime datePlacement;

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutPlacement statut = StatutPlacement.ACTIF;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Placement() {
    }

    public Placement(Client client, CompteBancaire compteBancaire, Fonds fonds, BigDecimal montant) {
        this.client = client;
        this.compteBancaire = compteBancaire;
        this.fonds = fonds;
        this.montant = montant;
        this.rendementPrevu = fonds != null ? fonds.getRendement() : BigDecimal.ZERO;
        this.gainPrevu = fonds != null ? fonds.calculerGainPrevu(montant) : BigDecimal.ZERO;
        this.datePlacement = LocalDateTime.now();
        this.statut = StatutPlacement.ACTIF;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (datePlacement == null) {
            datePlacement = LocalDateTime.now();
        }
        if (statut == null) {
            statut = StatutPlacement.ACTIF;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean estActif() {
        return StatutPlacement.ACTIF.equals(statut);
    }

    public void cloturer() {
        this.statut = StatutPlacement.CLOTURE;
        this.dateCloture = LocalDateTime.now();
    }

    public void annuler() {
        this.statut = StatutPlacement.ANNULE;
        this.dateCloture = LocalDateTime.now();
    }

    public BigDecimal getValeurEstimee() {
        if (montant == null) return BigDecimal.ZERO;
        if (gainPrevu == null) return montant;
        return montant.add(gainPrevu);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public CompteBancaire getCompteBancaire() {
        return compteBancaire;
    }

    public void setCompteBancaire(CompteBancaire compteBancaire) {
        this.compteBancaire = compteBancaire;
    }

    public Fonds getFonds() {
        return fonds;
    }

    public void setFonds(Fonds fonds) {
        this.fonds = fonds;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Placement{" +
                "id=" + id +
                ", montant=" + montant +
                ", rendementPrevu=" + rendementPrevu +
                ", gainPrevu=" + gainPrevu +
                ", statut=" + statut +
                ", datePlacement=" + datePlacement +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Placement placement)) return false;
        return id != null && Objects.equals(id, placement.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
