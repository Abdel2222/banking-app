package com.banking.entities;

import com.banking.entity.enums.TypeOperation;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "operations")
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK vers comptes_bancaires.id (aligné sur ta BDD)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_bancaire_id", nullable = false)
    private CompteBancaire compteBancaire;

    // Numéro du compte source (présent dans ta BDD)
    @Column(name = "numero_compte", nullable = false)
    private String numeroCompte;

    // Numéro du compte destinataire (présent dans ta BDD)
    @Column(name = "numero_compte_destinataire")
    private String numeroCompteDestinataire;

    // Nom du titulaire destinataire (utilisé par ton service)
    @Column(name = "nom_titulaire_destinataire")
    private String nomTitulaireDestinataire;

    // Type d’opération (aligné sur ta BDD: type_operation)
    @Enumerated(EnumType.STRING)
    @Column(name = "type_operation", nullable = false)
    private TypeOperation typeOperation;

    @Column(name = "montant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(name = "date_operation", nullable = false)
    private LocalDateTime dateOperation = LocalDateTime.now();

    @Column(name = "description")
    private String description;

    @Column(name = "communication")
    private String communication;

    @Column(name = "commentaire")
    private String commentaire;

    /* ================== Constructeurs ================== */

    public Operation() { }

    // 3 paramètres (utilisé par tes services) -> délègue au 4 paramètres
    public Operation(CompteBancaire compte, BigDecimal montant, TypeOperation typeOperation) {
        this(compte, montant, typeOperation, null);
    }

    public Operation(CompteBancaire compte, BigDecimal montant, TypeOperation typeOperation, String description) {
        this.compteBancaire = compte;
        this.numeroCompte = (compte != null ? compte.getNumCompte() : null);
        this.montant = montant;
        this.typeOperation = typeOperation;
        this.description = description;
        this.dateOperation = LocalDateTime.now();
    }

    /* ================== Getters / Setters ================== */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CompteBancaire getCompteBancaire() { return compteBancaire; }
    public void setCompteBancaire(CompteBancaire compteBancaire) { this.compteBancaire = compteBancaire; }

    public String getNumeroCompte() { return numeroCompte; }
    public void setNumeroCompte(String numeroCompte) { this.numeroCompte = numeroCompte; }

    public String getNumeroCompteDestinataire() { return numeroCompteDestinataire; }
    public void setNumeroCompteDestinataire(String numeroCompteDestinataire) { this.numeroCompteDestinataire = numeroCompteDestinataire; }

    public String getNomTitulaireDestinataire() { return nomTitulaireDestinataire; }
    public void setNomTitulaireDestinataire(String nomTitulaireDestinataire) { this.nomTitulaireDestinataire = nomTitulaireDestinataire; }

    public TypeOperation getTypeOperation() { return typeOperation; }
    public void setTypeOperation(TypeOperation typeOperation) { this.typeOperation = typeOperation; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public LocalDateTime getDateOperation() { return dateOperation; }
    public void setDateOperation(LocalDateTime dateOperation) { this.dateOperation = dateOperation; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCommunication() { return communication; }
    public void setCommunication(String communication) { this.communication = communication; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    /* ======== ALIAS pour compatibilité (évite de changer tout le code existant) ======== */

    // Plusieurs classes appellent getType() au lieu de getTypeOperation()
    public TypeOperation getType() { return this.typeOperation; }
    public void setType(TypeOperation type) { this.typeOperation = type; }

    // Certains DTOs appellent getDescriptionOperation()
    public String getDescriptionOperation() { return this.description; }
}


