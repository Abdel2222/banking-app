package com.banking.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "chats")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "contenu", nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @NotNull
    @Column(name = "date_heure", nullable = false)
    private LocalDateTime dateHeure;

    /** Réponse automatique de l'IA (Ollama) OU réponse système */
    @Column(name = "reponse", columnDefinition = "TEXT")
    private String reponse;

    /** Réponse manuelle de l'admin (séparée de la réponse auto) */
    @Column(name = "reponse_admin", columnDefinition = "TEXT")
    private String reponseAdmin;

    /** Statut de la demande */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private ChatStatut statut = ChatStatut.NORMAL;

    /** Type d'action sensible détectée */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private ChatActionType actionType = ChatActionType.NORMAL;

    /** ID du client qui a envoyé le message (depuis le JWT) */
    @Column(name = "client_id")
    private Long clientId;

    /** Nom affichable du client */
    @Column(name = "client_nom", length = 200)
    private String clientNom;

    /** Numéro de compte concerné par la demande */
    @Column(name = "num_compte", length = 50)
    private String numCompte;

    /** ID de l'admin qui a traité la demande */
    @Column(name = "admin_id")
    private Long adminId;

    /** Date de réponse de l'admin */
    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;

    public Chat() {
        this.dateHeure = LocalDateTime.now();
    }

    public Chat(String contenu) {
        this();
        this.contenu = contenu;
    }

    @PrePersist
    protected void onCreate() {
        if (dateHeure == null) dateHeure = LocalDateTime.now();
        if (statut == null) statut = ChatStatut.NORMAL;
        if (actionType == null) actionType = ChatActionType.NORMAL;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getDateHeure() { return dateHeure; }
    public void setDateHeure(LocalDateTime dateHeure) { this.dateHeure = dateHeure; }

    public String getReponse() { return reponse; }
    public void setReponse(String reponse) { this.reponse = reponse; }

    public String getReponseAdmin() { return reponseAdmin; }
    public void setReponseAdmin(String reponseAdmin) { this.reponseAdmin = reponseAdmin; }

    public ChatStatut getStatut() { return statut; }
    public void setStatut(ChatStatut statut) { this.statut = statut; }

    public ChatActionType getActionType() { return actionType; }
    public void setActionType(ChatActionType actionType) { this.actionType = actionType; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getNumCompte() { return numCompte; }
    public void setNumCompte(String numCompte) { this.numCompte = numCompte; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }

    public LocalDateTime getDateReponse() { return dateReponse; }
    public void setDateReponse(LocalDateTime dateReponse) { this.dateReponse = dateReponse; }
}