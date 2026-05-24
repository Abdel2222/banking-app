package com.banking.entities;

import com.banking.entity.enums.StatutPlacement;
import com.banking.entity.enums.TypeFonds;
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

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "code_identification", nullable = false, length = 50)
    private String codeIdentification;

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
    @DecimalMin(value = "1.00", message = "Le montant doit être supérieur à 0")
    @Column(name = "montant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(name = "rendement", precision = 5, scale = 2)
    private BigDecimal rendement;

    @Column(name = "date_placement", nullable = false)
    private LocalDateTime datePlacement;

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    @Column(name = "date_sortie")
    private LocalDateTime dateSortie;

    @Column(name = "frais_sortie", precision = 15, scale = 2)
    private BigDecimal fraisSortie;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutPlacement statut = StatutPlacement.ACTIF;

    public Placement() {}

    public Placement(Client client, CompteBancaire compteBancaire, Fonds fonds, BigDecimal montant) {
        this.client         = client;
        this.compteBancaire = compteBancaire;
        this.fonds          = fonds;
        this.montant        = montant;
        this.datePlacement  = LocalDateTime.now();
        this.statut         = StatutPlacement.ACTIF;

        if (fonds != null) {
            this.rendement          = fonds.getRendement();
            this.codeIdentification = fonds.getCodeIdentification();
            TypeFonds tf            = TypeFonds.fromCode(fonds.getCodeIdentification());
            this.type               = tf.name();
            this.dateCloture        = this.datePlacement.plusDays(tf.getDureeJours());
        } else {
            this.rendement = BigDecimal.ZERO;
        }
    }

    @PrePersist
    protected void onCreate() {
        if (datePlacement == null) datePlacement = LocalDateTime.now();
        if (statut == null)        statut        = StatutPlacement.ACTIF;
        if (dateCloture == null && fonds != null) {
            TypeFonds tf = TypeFonds.fromCode(fonds.getCodeIdentification());
            dateCloture = datePlacement.plusDays(tf.getDureeJours());
            if (type == null)               type               = tf.name();
            if (codeIdentification == null) codeIdentification = fonds.getCodeIdentification();
        }
    }

    public boolean estActif() {
        return StatutPlacement.ACTIF.equals(statut);
    }

    public void cloturer() {
        this.statut     = StatutPlacement.CLOTURE;
        this.dateSortie = LocalDateTime.now();
        BigDecimal retour = getValeurEstimee();
        if (this.compteBancaire != null && retour.signum() > 0)
            this.compteBancaire.crediter(retour);
    }

    public void sortirAvantEcheance(BigDecimal frais) {
        if (!estActif())
            throw new IllegalStateException("Ce placement n'est plus actif");

        this.fraisSortie = (frais != null && frais.signum() > 0) ? frais : BigDecimal.ZERO;
        this.dateSortie  = LocalDateTime.now();
        this.statut      = StatutPlacement.CLOTURE;

        BigDecimal retour = this.montant.subtract(this.fraisSortie);
        if (retour.signum() < 0) retour = BigDecimal.ZERO;

        if (this.compteBancaire != null)
            this.compteBancaire.crediter(retour);
    }

    public void annuler() {
        this.statut     = StatutPlacement.ANNULE;
        this.dateSortie = LocalDateTime.now();
    }

    public boolean estSortieAnticipee() {
        return dateSortie != null && dateCloture != null && dateSortie.isBefore(dateCloture);
    }

    public BigDecimal getGainPrevu() {
        if (montant == null || rendement == null) return BigDecimal.ZERO;
        return montant.multiply(rendement).divide(BigDecimal.valueOf(100));
    }

    public BigDecimal getValeurEstimee() {
        if (montant == null) return BigDecimal.ZERO;
        return montant.add(getGainPrevu());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCodeIdentification() { return codeIdentification; }
    public void setCodeIdentification(String codeIdentification) { this.codeIdentification = codeIdentification; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public CompteBancaire getCompteBancaire() { return compteBancaire; }
    public void setCompteBancaire(CompteBancaire compteBancaire) { this.compteBancaire = compteBancaire; }

    public Fonds getFonds() { return fonds; }
    public void setFonds(Fonds fonds) { this.fonds = fonds; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public BigDecimal getRendement() { return rendement; }
    public void setRendement(BigDecimal rendement) { this.rendement = rendement; }

    public LocalDateTime getDatePlacement() { return datePlacement; }
    public void setDatePlacement(LocalDateTime datePlacement) { this.datePlacement = datePlacement; }

    public LocalDateTime getDateCloture() { return dateCloture; }
    public void setDateCloture(LocalDateTime dateCloture) { this.dateCloture = dateCloture; }

    public LocalDateTime getDateSortie() { return dateSortie; }
    public void setDateSortie(LocalDateTime dateSortie) { this.dateSortie = dateSortie; }

    public BigDecimal getFraisSortie() { return fraisSortie; }
    public void setFraisSortie(BigDecimal fraisSortie) { this.fraisSortie = fraisSortie; }

    public StatutPlacement getStatut() { return statut; }
    public void setStatut(StatutPlacement statut) { this.statut = statut; }

    @Override
    public String toString() {
        return "Placement{id=" + id + ", type='" + type + "', code='" + codeIdentification +
                "', montant=" + montant + ", rendement=" + rendement +
                ", statut=" + statut + ", datePlacement=" + datePlacement +
                ", dateCloture=" + dateCloture + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Placement placement)) return false;
        return id != null && Objects.equals(id, placement.id);
    }

    @Override
    public int hashCode() { return id != null ? id.hashCode() : 0; }
}