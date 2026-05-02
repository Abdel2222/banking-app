package com.banking.entities;

import com.banking.entity.enums.CardRequestStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "demandes_cartes")
public class DemandeCarteBancaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Client client;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private CompteBancaire compte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardRequestStatus status = CardRequestStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column
    private String rejectedReason;

    @PrePersist
    public void prePersist() {
        this.requestedAt = LocalDateTime.now();
    }

    // --- Getters/Setters ---

    public Long getId() { return id; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public CompteBancaire getCompte() { return compte; }
    public void setCompte(CompteBancaire compte) { this.compte = compte; }

    public CardRequestStatus getStatus() { return status; }
    public void setStatus(CardRequestStatus status) { this.status = status; }

    public LocalDateTime getRequestedAt() { return requestedAt; }

    public String getRejectedReason() { return rejectedReason; }
    public void setRejectedReason(String rejectedReason) { this.rejectedReason = rejectedReason; }

    @Override
    public String toString() {
        return "DemandeCarteBancaire{" +
                "id=" + id +
                ", compte=" + (compte != null ? compte.getNumCompte() : "null") +
                ", client=" + (client != null ? client.getEmail() : "null") +
                ", status=" + status +
                ", requestedAt=" + requestedAt +
                '}';
    }
}
