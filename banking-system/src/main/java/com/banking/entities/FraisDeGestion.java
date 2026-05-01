package com.banking.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import static com.banking.entities.FraisDeGestion.TypeFrais.MONTANT_FRAIS_OUVERTURE;

@Entity
@Table(name = "frais_de_gestion")
public class FraisDeGestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.0", message = "Le montant ne peut pas être négatif")
    @Column(name = "montant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @NotBlank(message = "La description est obligatoire")
    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @NotNull(message = "La date de début est obligatoire")
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_frais", nullable = false)
    private TypeFrais typeFrais;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodicite", nullable = false)
    private Periodicite periodicite = Periodicite.MENSUEL;

    @Column(name = "est_actif", nullable = false)
    private Boolean estActif = true;

    @Column(name = "montant_total_facture", precision = 15, scale = 2)
    private BigDecimal montantTotalFacture = BigDecimal.ZERO;

    @Column(name = "derniere_facturation")
    private LocalDate derniereFacturation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    // Enums internes
    public enum TypeFrais {
        TENUE_COMPTE("Frais de tenue de compte"),
        CARTE_BANCAIRE("Frais de carte bancaire"),
        COMMISSION_INTERVENTION("Commission d'intervention"),
        AGIOS("Agios"),
        VIREMENT_INTERNATIONAL("Frais de virement international"),
        ASSURANCE("Assurance"),
        PACKAGE_BANCAIRE("Package bancaire"),
        INCIDENT_PAIEMENT("Frais d'incident de paiement"),
        AUTRE("Autres frais");

        private final String libelle;
        public static final java.math.BigDecimal MONTANT_FRAIS_OUVERTURE = new java.math.BigDecimal("2.00");

        TypeFrais(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }
    }

    public enum Periodicite {
        QUOTIDIEN(1),
        HEBDOMADAIRE(7),
        MENSUEL(30),
        TRIMESTRIEL(90),
        SEMESTRIEL(180),
        ANNUEL(365),
        PONCTUEL(0);

        private final int jours;

        Periodicite(int jours) {
            this.jours = jours;
        }

        public int getJours() {
            return jours;
        }
    }

    // Constructeurs
    public FraisDeGestion() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.dateDebut = LocalDate.now();
        this.estActif = true;
    }

    public FraisDeGestion(BigDecimal montant, String description, TypeFrais typeFrais,
                          LocalDate dateFin, Client client) {
        this();
        this.montant = montant;
        this.description = description;
        this.typeFrais = typeFrais;
        this.dateFin = dateFin;
        this.client = client;
    }
    /**
     * Crée un frais d'ouverture de compte (2,00 €, ponctuel, facturé immédiatement).
     * Usage: FraisDeGestion fg = FraisDeGestion.creerFraisOuverture(compte.getClient());
     */
    public static FraisDeGestion creerFraisOuverture(Client client) {
        FraisDeGestion f = new FraisDeGestion();
        f.setClient(client);
        f.setMontant(MONTANT_FRAIS_OUVERTURE);
        f.setDescription("Frais d'ouverture de compte");
        f.setTypeFrais(TypeFrais.TENUE_COMPTE);           // adapte si tu préfères AUTRE/CARTE_BANCAIRE
        f.setPeriodicite(Periodicite.PONCTUEL);           // frais one-shot
        f.setDateDebut(java.time.LocalDate.now());
        f.setDateFin(java.time.LocalDate.now());          // même jour = ponctuel
        f.setEstActif(false);                             // déjà facturé, pas “actif” en continu
        f.setMontantTotalFacture(MONTANT_FRAIS_OUVERTURE);
        f.setDerniereFacturation(java.time.LocalDate.now());
        return f;
    }


    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
        if (dateDebut == null) {
            dateDebut = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Méthodes métier
    public boolean estEchu() {
        return LocalDate.now().isAfter(dateFin);
    }

    public boolean estEnCours() {
        LocalDate now = LocalDate.now();
        return !now.isBefore(dateDebut) && !now.isAfter(dateFin) && estActif;
    }

    public int joursRestants() {
        if (estEchu()) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dateFin);
    }

    public int dureeEnJours() {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(dateDebut, dateFin);
    }

    public BigDecimal calculerMontantPeriode() {
        if (periodicite == Periodicite.PONCTUEL) {
            return montant;
        }

        int nombrePeriodes = dureeEnJours() / periodicite.getJours();
        return montant.multiply(new BigDecimal(nombrePeriodes));
    }

    public boolean doitEtreFacture() {
        if (!estEnCours() || periodicite == Periodicite.PONCTUEL) {
            return false;
        }

        if (derniereFacturation == null) {
            return true;
        }

        int joursDepuisDerniereFacturation =
                (int) java.time.temporal.ChronoUnit.DAYS.between(derniereFacturation, LocalDate.now());

        return joursDepuisDerniereFacturation >= periodicite.getJours();
    }

    public void facturer() {
        if (!doitEtreFacture()) {
            throw new IllegalStateException("Ces frais ne peuvent pas être facturés maintenant");
        }

        this.montantTotalFacture = this.montantTotalFacture.add(montant);
        this.derniereFacturation = LocalDate.now();
    }

    public void desactiver() {
        this.estActif = false;
    }

    public void reactiver() {
        if (!estEchu()) {
            this.estActif = true;
        } else {
            throw new IllegalStateException("Impossible de réactiver des frais échus");
        }
    }

    // equals, hashCode et toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FraisDeGestion)) return false;
        FraisDeGestion that = (FraisDeGestion) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "FraisDeGestion{" +
                "id=" + id +
                ", montant=" + montant +
                ", typeFrais=" + typeFrais.getLibelle() +
                ", periodicite=" + periodicite +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", estActif=" + estActif +
                ", estEchu=" + estEchu() +
                '}';
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public TypeFrais getTypeFrais() {
        return typeFrais;
    }

    public void setTypeFrais(TypeFrais typeFrais) {
        this.typeFrais = typeFrais;
    }

    public Periodicite getPeriodicite() {
        return periodicite;
    }

    public void setPeriodicite(Periodicite periodicite) {
        this.periodicite = periodicite;
    }

    public Boolean getEstActif() {
        return estActif;
    }

    public void setEstActif(Boolean estActif) {
        this.estActif = estActif;
    }

    public BigDecimal getMontantTotalFacture() {
        return montantTotalFacture;
    }

    public void setMontantTotalFacture(BigDecimal montantTotalFacture) {
        this.montantTotalFacture = montantTotalFacture;
    }

    public LocalDate getDerniereFacturation() {
        return derniereFacturation;
    }

    public void setDerniereFacturation(LocalDate derniereFacturation) {
        this.derniereFacturation = derniereFacturation;
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

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

}