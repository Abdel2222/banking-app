package com.banking.dto.response;

import com.banking.entities.Chat;
import com.banking.entities.ChatActionType;
import com.banking.entities.ChatStatut;

import java.time.LocalDateTime;

public class ChatResponse {

    public Long id;
    public String contenu;
    public LocalDateTime dateHeure;

    /** Réponse automatique (Ollama ou système) */
    public String reponse;

    /** Réponse manuelle de l'admin */
    public String reponseAdmin;

    public ChatStatut statut;
    public ChatActionType actionType;

    public Long clientId;
    public String clientNom;
    public String numCompte;

    public Long adminId;
    public LocalDateTime dateReponse;

    public ChatResponse() {}

    /** Constructeur de mapping depuis l'entité */
    public static ChatResponse fromEntity(Chat chat) {
        ChatResponse dto = new ChatResponse();
        dto.id           = chat.getId();
        dto.contenu      = chat.getContenu();
        dto.dateHeure    = chat.getDateHeure();
        dto.reponse      = chat.getReponse();
        dto.reponseAdmin = chat.getReponseAdmin();
        dto.statut       = chat.getStatut();
        dto.actionType   = chat.getActionType();
        dto.clientId     = chat.getClientId();
        dto.clientNom    = chat.getClientNom();
        dto.numCompte    = chat.getNumCompte();
        dto.adminId      = chat.getAdminId();
        dto.dateReponse  = chat.getDateReponse();
        return dto;
    }
}