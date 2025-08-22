package com.banking.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "cartes_bancaires",
        indexes = {
                @Index(name = "idx_numero_carte", columnList = "numero_carte", unique = true)
        }
)
public class CarteBancaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_carte", nullable = false, unique = true, length = 16)
    @Size(min = 16, max = 16, message = "Le numéro de carte doit contenir 16 chiffres")
    private String numeroCarte;

    @Column(name = "date_expiration", nullable = false)
    private LocalDate dateExpiration;

    @Column(name = "cvv", nullable = false, length = 3)
    @Size(min = 3, max = 3, message = "Le CVV doit contenir 3 chiffres")
    private String cvv;

    @Column(name = "est_active", nullable = false)
    private boolean estActive;

    @Column(name = "plafond_journalier", nullable = false)
    private Double plafondJournalier;

    @Column(name = "plafond_mensuel", nullable = false)
    private Double plafondMensuel;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_bancaire_id", nullable = false, unique = true)
    private CompteBancaire compteBancaire;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CarteBancaire() {}

    public CarteBancaire(String numeroCarte, LocalDate dateExpiration, String cvv,
                         boolean estActive, Double plafondJournalier, Double plafondMensuel,
                         CompteBancaire compteBancaire) {
        this.numeroCarte = numeroCarte;
        this.dateExpiration = dateExpiration;
        this.cvv = cvv;
        this.estActive = estActive;
        this.plafondJournalier = plafondJournalier;
        this.plafondMensuel = plafondMensuel;
        this.compteBancaire = compteBancaire;
    }

    public CarteBancaire(String numeroCarte, LocalDate dateExpiration, String cvv,
                         CompteBancaire compteBancaire) {
        this(numeroCarte, dateExpiration, cvv, true, 500d, 2000d, compteBancaire);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters / Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroCarte() { return numeroCarte; }
    public void setNumeroCarte(String numeroCarte) { this.numeroCarte = numeroCarte; }

    public LocalDate getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDate dateExpiration) { this.dateExpiration = dateExpiration; }

    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }

    public boolean isEstActive() { return estActive; }
    public void setEstActive(boolean estActive) { this.estActive = estActive; }

    public Double getPlafondJournalier() { return plafondJournalier; }
    public void setPlafondJournalier(Double plafondJournalier) { this.plafondJournalier = plafondJournalier; }

    public Double getPlafondMensuel() { return plafondMensuel; }
    public void setPlafondMensuel(Double plafondMensuel) { this.plafondMensuel = plafondMensuel; }

    public CompteBancaire getCompteBancaire() { return compteBancaire; }
    public void setCompteBancaire(CompteBancaire compteBancaire) { this.compteBancaire = compteBancaire; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Utilitaires
    public void bloquer() { this.estActive = false; }
    public void debloquer() { this.estActive = true; }
    public boolean estExpiree() { return dateExpiration.isBefore(LocalDate.now()); }
}
