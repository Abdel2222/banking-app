package com.banking.entities;

import com.banking.entity.enums.TypeOperation;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "releves_mensuels",
        indexes = {
                @Index(name = "idx_releve_num_compte", columnList = "num_compte"),
                @Index(name = "idx_releve_date",       columnList = "date_operation")
        })
public class ReleveDeCompte{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "num_compte", nullable = false, length = 32)
    private String numCompte;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_operation", nullable = false, length = 30)
    private TypeOperation typeOperation;

    @Column(name = "montant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "date_operation", nullable = false)
    private LocalDateTime dateOperation;

    @Column(name = "solde", precision = 15, scale = 2)
    private BigDecimal solde; // solde du compte après l'opération

    // FK optionnelle (ta DDL la commente, mais JPA peut mapper sans contrainte DB)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "compte_id", referencedColumnName = "id")
    private CompteBancaire compte;

    // Getters/Setters
    public Long getId() { return id; }
    public String getNumCompte() { return numCompte; }
    public void setNumCompte(String numCompte) { this.numCompte = numCompte; }
    public TypeOperation getTypeOperation() { return typeOperation; }
    public void setTypeOperation(TypeOperation typeOperation) { this.typeOperation = typeOperation; }
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDateOperation() { return dateOperation; }
    public void setDateOperation(LocalDateTime dateOperation) { this.dateOperation = dateOperation; }
    public BigDecimal getSolde() { return solde; }
    public void setSolde(BigDecimal solde) { this.solde = solde; }
    public CompteBancaire getCompte() { return compte; }
    public void setCompte(CompteBancaire compte) { this.compte = compte; }
}
