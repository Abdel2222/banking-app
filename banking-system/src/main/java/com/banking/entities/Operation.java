package com.banking.entities;

import com.banking.entity.enums.TypeOperation;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ✅ ajoute le listener
@Entity
@Table(name = "operations")
@EntityListeners(Operation.OperationJournalListener.class)
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK vers comptes_bancaires.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_bancaire_id", nullable = false)
    private CompteBancaire compteBancaire;

    @Column(name = "numero_compte", nullable = false)
    private String numeroCompte;

    @Column(name = "numero_compte_destinataire")
    private String numeroCompteDestinataire;

    @Column(name = "nom_titulaire_destinataire")
    private String nomTitulaireDestinataire;

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

    public Operation() { }

    public Operation(CompteBancaire compte, BigDecimal montant, TypeOperation typeOperation) {
        this(compte, montant, typeOperation, null);
    }

    public Operation(CompteBancaire compte, BigDecimal montant, TypeOperation typeOperation, String description) {
        this.compteBancaire = compte;
        this.numeroCompte   = (compte != null ? compte.getNumCompte() : null);
        this.montant        = montant;
        this.typeOperation  = typeOperation;
        this.description    = description;
        this.dateOperation  = LocalDateTime.now();
    }

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

    // Alias de compatibilité
    public TypeOperation getType() { return this.typeOperation; }
    public void setType(TypeOperation type) { this.typeOperation = type; }
    public String getDescriptionOperation() { return this.description; }

    /* ============================================================
       LISTENER qui pousse une ligne dans `releves_mensuels` à chaque insert
       ============================================================ */
    public static class OperationJournalListener {

        @PostPersist
        public void afterPersist(Operation op) {
            try {
                // Récupère les beans Spring (repo) via le holder statique
                var releveRepo = com.banking.support.SpringContext.getBean(com.banking.repositories.ReleveDeCompteRepository.class);

                BigDecimal solde = null;
                try {
                    // On tente de lire le solde à jour du compte
                    var cRepo = com.banking.support.SpringContext.getBean(com.banking.repositories.CompteBancaireRepository.class);
                    com.banking.entities.CompteBancaire c =
                            (op.getCompteBancaire() != null)
                                    ? op.getCompteBancaire()
                                    : cRepo.findByNumCompte(op.getNumeroCompte()).orElse(null);
                    if (c != null) solde = c.getBalance();
                } catch (Exception ignore) { /* on ne bloque pas */ }

                // Construit la ligne "relevé"
                com.banking.entities.ReleveDeCompte r = new com.banking.entities.ReleveDeCompte();
                r.setNumCompte(op.getNumeroCompte());
                r.setTypeOperation(op.getTypeOperation());
                r.setMontant(op.getMontant());
                r.setDescription(op.getDescription());
                r.setDateOperation(op.getDateOperation());
                r.setSolde(solde);
                r.setCompte(op.getCompteBancaire());

                // Enregistre
                releveRepo.save(r);
            } catch (Throwable t) {
                // Surtout ne pas casser l'opération bancaire si le journal échoue
                // (tu peux logger ici si tu veux)
            }
        }
    }
}
