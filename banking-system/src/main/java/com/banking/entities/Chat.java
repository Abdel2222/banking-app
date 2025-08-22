package com.banking.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "chats")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le contenu du message est obligatoire")
    @Size(min = 1, max = 1000, message = "Le message doit contenir entre 1 et 1000 caractères")
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Size(max = 1000, message = "La réponse ne peut pas dépasser 1000 caractères")
    @Column(name = "reponse", columnDefinition = "TEXT")
    private String reponse;

    @NotNull(message = "La date de création est obligatoire")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;

    @Column(name = "lu_par_source", nullable = false)
    private Boolean luParSource = false;

    @Column(name = "lu_par_destinataire", nullable = false)
    private Boolean luParDestinataire = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutChat statut = StatutChat.ENVOYE;

    // Relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_source_id", nullable = false)
    private CompteBancaire compteSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_destinataire_id")
    private CompteBancaire compteDestinataire;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_id")
    private Operation operation; // Lien optionnel avec une opération

    // Enum interne pour le statut
    public enum StatutChat {
        ENVOYE,
        RECU,
        LU,
        REPONDU
    }

    // Constructeurs
    public Chat() {
        this.createdAt = LocalDateTime.now();
        this.statut = StatutChat.ENVOYE;
        this.luParSource = true; // l'émetteur a lu son propre message
        this.luParDestinataire = false;
    }

    public Chat(String content, CompteBancaire compteSource) {
        this();
        this.content = content;
        this.compteSource = compteSource;
    }

    public Chat(String content, CompteBancaire compteSource, CompteBancaire compteDestinataire) {
        this();
        this.content = content;
        this.compteSource = compteSource;
        this.compteDestinataire = compteDestinataire;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // --- Méthodes métier ---

    public boolean estMessagePrive() {
        return compteDestinataire != null;
    }

    public boolean estMessageSupport() {
        return compteDestinataire == null;
    }

    public boolean estRepondu() {
        return reponse != null && !reponse.trim().isEmpty();
    }

    public boolean estLu() {
        if (estMessagePrive()) {
            return Boolean.TRUE.equals(luParSource) && Boolean.TRUE.equals(luParDestinataire);
        }
        return Boolean.TRUE.equals(luParSource);
    }

    /** ✅ Correction: comparaison par ID pour éviter les problèmes de proxies JPA */
    public void marquerCommeLu(CompteBancaire lecteur) {
        if (lecteur == null) return;

        Long lecteurId = lecteur.getId();

        if (compteSource != null && Objects.equals(compteSource.getId(), lecteurId)) {
            this.luParSource = true;
        } else if (compteDestinataire != null && Objects.equals(compteDestinataire.getId(), lecteurId)) {
            this.luParDestinataire = true;
        }

        // Ne pas rétrograder le statut si déjà REPONDU
        if (this.statut != StatutChat.REPONDU) {
            if (estMessagePrive()) {
                if (Boolean.TRUE.equals(this.luParSource) && Boolean.TRUE.equals(this.luParDestinataire)) {
                    this.statut = StatutChat.LU;
                }
            } else {
                if (Boolean.TRUE.equals(this.luParSource)) {
                    this.statut = StatutChat.LU;
                }
            }
        }
    }

    public void repondre(String reponse) {
        if (reponse == null || reponse.trim().isEmpty()) {
            throw new IllegalArgumentException("La réponse ne peut pas être vide");
        }
        if (estRepondu()) {
            throw new IllegalStateException("Ce message a déjà reçu une réponse");
        }

        this.reponse = reponse;
        this.dateReponse = LocalDateTime.now();
        this.statut = StatutChat.REPONDU;
    }

    public String getExtraitMessage(int longueurMax) {
        if (content == null) {
            return "";
        }
        if (content.length() <= longueurMax) {
            return content;
        }
        return content.substring(0, Math.max(0, longueurMax - 3)) + "...";
    }

    public long getDelaiReponseMinutes() {
        if (!estRepondu() || dateReponse == null) {
            return -1;
        }
        return java.time.Duration.between(createdAt, dateReponse).toMinutes();
    }

    // --- equals / hashCode / toString ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Chat)) return false;
        Chat chat = (Chat) o;
        return Objects.equals(id, chat.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Chat{" +
                "id=" + id +
                ", content='" + getExtraitMessage(30) + '\'' +
                ", createdAt=" + createdAt +
                ", statut=" + statut +
                ", estPrive=" + estMessagePrive() +
                '}';
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getReponse() { return reponse; }
    public void setReponse(String reponse) { this.reponse = reponse; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDateReponse() { return dateReponse; }
    public void setDateReponse(LocalDateTime dateReponse) { this.dateReponse = dateReponse; }

    public Boolean getLuParSource() { return luParSource; }
    public void setLuParSource(Boolean luParSource) { this.luParSource = luParSource; }

    public Boolean getLuParDestinataire() { return luParDestinataire; }
    public void setLuParDestinataire(Boolean luParDestinataire) { this.luParDestinataire = luParDestinataire; }

    public StatutChat getStatut() { return statut; }
    public void setStatut(StatutChat statut) { this.statut = statut; }

    public CompteBancaire getCompteSource() { return compteSource; }
    public void setCompteSource(CompteBancaire compteSource) { this.compteSource = compteSource; }

    public CompteBancaire getCompteDestinataire() { return compteDestinataire; }
    public void setCompteDestinataire(CompteBancaire compteDestinataire) { this.compteDestinataire = compteDestinataire; }

    public Operation getOperation() { return operation; }
    public void setOperation(Operation operation) { this.operation = operation; }
}
