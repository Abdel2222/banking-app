package com.banking.entities;

import com.banking.entity.enums.TypeOperation;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "operations")
@EntityListeners(Operation.OperationJournalListener.class)
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_bancaire_id", nullable = false)
    @JsonBackReference("compte-operations")  // ✅ coupe la récursion côté enfant
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

    // ===== Constructeurs =====

    public Operation() {}

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

    // ===== Getters / Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CompteBancaire getCompteBancaire() { return compteBancaire; }
    public void setCompteBancaire(CompteBancaire compteBancaire) { this.compteBancaire = compteBancaire; }

    public String getNumeroCompte() { return numeroCompte; }
    public void setNumeroCompte(String numeroCompte) { this.numeroCompte = numeroCompte; }

    public String getNumeroCompteDestinataire() { return numeroCompteDestinataire; }
    public void setNumeroCompteDestinataire(String v) { this.numeroCompteDestinataire = v; }

    public String getNomTitulaireDestinataire() { return nomTitulaireDestinataire; }
    public void setNomTitulaireDestinataire(String v) { this.nomTitulaireDestinataire = v; }

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

    // ===== Listener JPA =====

    public static class OperationJournalListener {

        @PostPersist
        public void afterPersist(Operation op) {
            try {
                var releveRepo = com.banking.support.SpringContext.getBean(
                        com.banking.repositories.ReleveDeCompteRepository.class);

                BigDecimal solde = null;
                try {
                    var cRepo = com.banking.support.SpringContext.getBean(
                            com.banking.repositories.CompteBancaireRepository.class);
                    CompteBancaire c = (op.getCompteBancaire() != null)
                            ? op.getCompteBancaire()
                            : cRepo.findByNumCompte(op.getNumeroCompte()).orElse(null);
                    if (c != null) solde = c.getBalance();
                } catch (Exception ignore) {}

                com.banking.entities.ReleveDeCompte r = new com.banking.entities.ReleveDeCompte();
                r.setNumCompte(op.getNumeroCompte());
                r.setTypeOperation(op.getTypeOperation());
                r.setMontant(op.getMontant());
                r.setDescription(op.getDescription());
                r.setDateOperation(op.getDateOperation());
                r.setSolde(solde);
                r.setCompte(op.getCompteBancaire());

                releveRepo.save(r);
            } catch (Throwable t) {
                // Ne pas bloquer l'opération si le journal échoue
            }
        }
    }
}