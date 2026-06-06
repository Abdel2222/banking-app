package com.banking.entities;

import com.banking.entity.enums.ChatStatut;
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

    @Column(name = "reponse", columnDefinition = "TEXT")
    private String reponse;

    @Column(name = "reponse_admin", columnDefinition = "TEXT")
    private String reponseAdmin;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private ChatStatut statut = ChatStatut.NORMAL;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_nom", length = 200)
    private String clientNom;

    @Column(name = "num_compte", length = 50)
    private String numCompte;

    @Column(name = "admin_id")
    private Long adminId;

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
    }

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