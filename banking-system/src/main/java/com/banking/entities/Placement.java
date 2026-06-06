package com.banking.entities;

import com.banking.entity.enums.StatutPlacement;
import com.banking.entity.enums.TypeFonds;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    /* ================================================
       CONSTRUCTEURS
    ================================================ */

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

            // Résolution du type : code d'abord, nom en fallback
            TypeFonds tf = TypeFonds.fromCode(fonds.getCodeIdentification());
            if (tf == TypeFonds.SECURITE
                    && fonds.getCodeIdentification() != null
                    && !fonds.getCodeIdentification().toUpperCase().contains("SEC")) {
                TypeFonds tfParNom = TypeFonds.fromNom(fonds.getNomFonds());
                if (tfParNom != TypeFonds.SECURITE) tf = tfParNom;
            }

            this.type        = tf.name();
            this.dateCloture = this.datePlacement.plusDays(tf.getDureeJours());

            System.out.println("✅ Placement créé — Fonds: " + fonds.getNomFonds()
                    + " | Type: " + tf.name()
                    + " | Durée: " + tf.getDureeJours() + "j"
                    + " | Clôture: " + this.dateCloture);
        } else {
            this.rendement = BigDecimal.ZERO;
        }
    }

    /* ================================================
       LIFECYCLE JPA
    ================================================ */

    @PrePersist
    protected void onCreate() {
        if (datePlacement == null) datePlacement = LocalDateTime.now();
        if (statut == null)        statut        = StatutPlacement.ACTIF;
        if (dateCloture == null && fonds != null) {
            TypeFonds tf = TypeFonds.fromCode(fonds.getCodeIdentification());
            dateCloture  = datePlacement.plusDays(tf.getDureeJours());
            if (type == null)               type               = tf.name();
            if (codeIdentification == null) codeIdentification = fonds.getCodeIdentification();
            System.out.println("⚠️ @PrePersist — date_cloture recalculée : " + dateCloture);
        }
    }

    /* ================================================
       MÉTHODES MÉTIER
    ================================================ */

    public boolean estActif() {
        return StatutPlacement.ACTIF.equals(statut);
    }

    public boolean estSortieAnticipee() {
        return dateSortie != null && dateCloture != null && dateSortie.isBefore(dateCloture);
    }

    /**
     * Clôture normale à échéance.
     * Restitue : capital + gain total (valeurEstimee).
     * Le crédit du compte est géré dans PlacementService.
     */
    public void cloturer() {
        this.statut     = StatutPlacement.CLOTURE;
        this.dateSortie = LocalDateTime.now();
    }

    /**
     * Sortie anticipée avec frais bancaires dégressifs.
     * Restitue : capital + intérêts courus - frais.
     * Le crédit du compte est géré dans PlacementService.
     */
    public void sortirAvantEcheance() {
        if (!estActif())
            throw new IllegalStateException("Ce placement n'est plus actif");

        this.fraisSortie = calculerFraisSortie();
        this.dateSortie  = LocalDateTime.now();
        this.statut      = StatutPlacement.CLOTURE;

        System.out.println("💸 Sortie anticipée — Capital: " + montant
                + " | Intérêts courus: " + getInteretsCourus()
                + " | Frais: " + this.fraisSortie);
    }

    public void annuler() {
        this.statut     = StatutPlacement.ANNULE;
        this.dateSortie = LocalDateTime.now();
    }

    /* ================================================
       CALCULS FINANCIERS
    ================================================ */

    /**
     * Frais de sortie anticipée — logique bancaire dégressive.
     *
     * Formule :
     *   ratio_restant  = joursRestants / joursTotaux
     *   frais_degressifs = capital × 2% × ratio_restant
     *   frais_minimum    = capital × 0.25%
     *   frais_appliques  = max(frais_degressifs, frais_minimum)
     *
     * Exemples :
     *   Sort au jour 1   → ~2.00% du capital
     *   Sort à mi-chemin → ~1.00% du capital
     *   Sort à J-1       → 0.25% du capital (minimum garanti)
     */
    public BigDecimal calculerFraisSortie() {
        if (montant == null) return BigDecimal.ZERO;

        BigDecimal fraisMinimum = montant
                .multiply(BigDecimal.valueOf(0.0025))
                .setScale(2, RoundingMode.HALF_UP);

        if (datePlacement == null || dateCloture == null) {
            return montant
                    .multiply(BigDecimal.valueOf(0.02))
                    .setScale(2, RoundingMode.HALF_UP)
                    .max(fraisMinimum);
        }

        long joursTotaux   = ChronoUnit.DAYS.between(datePlacement, dateCloture);
        long joursRestants = ChronoUnit.DAYS.between(LocalDateTime.now(), dateCloture);

        if (joursTotaux <= 0) return fraisMinimum;

        double ratioRestant = Math.max(0.0, Math.min(1.0,
                (double) joursRestants / joursTotaux));

        BigDecimal fraisDegressifs = montant
                .multiply(BigDecimal.valueOf(0.02))
                .multiply(BigDecimal.valueOf(ratioRestant))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal fraisFinaux = fraisDegressifs.max(fraisMinimum);

        System.out.println("📊 Frais — Capital: " + montant
                + " | Jours totaux: " + joursTotaux
                + " | Jours restants: " + joursRestants
                + " | Ratio: " + String.format("%.1f%%", ratioRestant * 100)
                + " | Dégressifs: " + fraisDegressifs
                + " | Minimum: " + fraisMinimum
                + " | Appliqués: " + fraisFinaux);

        return fraisFinaux;
    }

    /**
     * Intérêts courus jusqu'à aujourd'hui (pro-rata temporis).
     * Formule : montant × (rendement/100) × (joursEcoules / joursTotaux)
     */
    public BigDecimal getInteretsCourus() {
        if (montant == null || rendement == null
                || datePlacement == null || dateCloture == null)
            return BigDecimal.ZERO;

        LocalDateTime now           = LocalDateTime.now();
        LocalDateTime dateSortieEff = now.isBefore(dateCloture) ? now : dateCloture;

        long joursTotaux  = ChronoUnit.DAYS.between(datePlacement, dateCloture);
        long joursEcoules = ChronoUnit.DAYS.between(datePlacement, dateSortieEff);

        if (joursTotaux <= 0 || joursEcoules <= 0) return BigDecimal.ZERO;

        return montant
                .multiply(rendement.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
                .multiply(BigDecimal.valueOf(joursEcoules))
                .divide(BigDecimal.valueOf(joursTotaux), 2, RoundingMode.HALF_UP);
    }

    /**
     * Gain total prévu si tenu jusqu'à l'échéance.
     * Formule : montant × rendement / 100
     */
    public BigDecimal getGainPrevu() {
        if (montant == null || rendement == null) return BigDecimal.ZERO;
        return montant
                .multiply(rendement)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Valeur estimée à l'échéance = capital + gain total.
     */
    public BigDecimal getValeurEstimee() {
        if (montant == null) return BigDecimal.ZERO;
        return montant.add(getGainPrevu());
    }

    /**
     * Montant récupérable si sortie anticipée aujourd'hui.
     * = capital + intérêts courus - frais dégressifs
     */
    public BigDecimal getMontantRecuperableAnticipe() {
        return montant
                .add(getInteretsCourus())
                .subtract(calculerFraisSortie());
    }

    /* ================================================
       GETTERS / SETTERS
    ================================================ */

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
        return "Placement{id=" + id + ", type='" + type + "', montant=" + montant
                + ", rendement=" + rendement + ", statut=" + statut
                + ", datePlacement=" + datePlacement + ", dateCloture=" + dateCloture + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Placement p)) return false;
        return id != null && Objects.equals(id, p.id);
    }

    @Override
    public int hashCode() { return id != null ? id.hashCode() : 0; }
}