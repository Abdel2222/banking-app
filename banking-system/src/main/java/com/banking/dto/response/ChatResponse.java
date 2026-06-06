package com.banking.dto.response;

import com.banking.entities.Chat;
import com.banking.entity.enums.ChatActionType;
import com.banking.entity.enums.ChatStatut;

import java.time.LocalDateTime;

public class ChatResponse {

    public Long id;
    public String contenu;
    public LocalDateTime dateHeure;
    public String reponse;
    public String reponseAdmin;
    public ChatStatut statut;

    // ✅ actionType calculé à la volée depuis le contenu — pas stocké en base
    public ChatActionType actionType;

    public Long clientId;
    public String clientNom;
    public String numCompte;
    public Long adminId;
    public LocalDateTime dateReponse;

    public ChatResponse() {}

    public static ChatResponse fromEntity(Chat chat) {
        ChatResponse dto = new ChatResponse();
        dto.id           = chat.getId();
        dto.contenu      = chat.getContenu();
        dto.dateHeure    = chat.getDateHeure();
        dto.reponse      = chat.getReponse();
        dto.reponseAdmin = chat.getReponseAdmin();
        dto.statut       = chat.getStatut();
        dto.clientId     = chat.getClientId();
        dto.clientNom    = chat.getClientNom();
        dto.numCompte    = chat.getNumCompte();
        dto.adminId      = chat.getAdminId();
        dto.dateReponse  = chat.getDateReponse();

        // ✅ Détection d'intention à la volée depuis le contenu
        dto.actionType   = detectActionType(chat.getContenu());
        return dto;
    }

    /**
     * Détecte le type d'action depuis le contenu du message.
     * Aucune colonne en base — calculé dynamiquement.
     */
    private static ChatActionType detectActionType(String contenu) {
        if (contenu == null || contenu.isBlank()) return ChatActionType.NORMAL;
        String m = contenu.toLowerCase();

        if (containsAny(m, "fraude", "frauduleux", "transaction frauduleuse",
                "paiement inconnu", "cvv", "carte compromise", "piratage",
                "quelqu'un utilise ma carte", "achat que je n'ai pas fait"))
            return ChatActionType.FRAUDE_CVV;

        if (containsAny(m, "bloquer ma carte", "bloquer carte", "carte perdue",
                "carte volée", "j'ai perdu ma carte", "suspendre ma carte",
                "désactiver ma carte", "je veux bloquer"))
            return ChatActionType.BLOCAGE_CARTE;

        if (containsAny(m, "débloquer ma carte", "débloquer carte",
                "réactiver ma carte", "activer ma carte",
                "ma carte ne fonctionne pas"))
            return ChatActionType.DEBLOCAGE_CARTE;

        if (containsAny(m, "changer cvv", "nouveau cvv", "cvv oublié",
                "renouveler ma carte", "remplacer ma carte", "nouvelle carte",
                "carte abîmée", "carte endommagée", "carte expirée"))
            return ChatActionType.REMPLACEMENT_CARTE;

        if (containsAny(m, "virement", "transfert", "virement bloqué",
                "virement échoué", "problème de virement", "virement en attente"))
            return ChatActionType.PROBLEME_VIREMENT;

        if (containsAny(m, "compte bloqué", "compte suspendu", "accès refusé",
                "solde incorrect", "erreur sur mon compte", "problème compte"))
            return ChatActionType.PROBLEME_COMPTE;

        return ChatActionType.NORMAL;
    }

    private static boolean containsAny(String message, String... keywords) {
        for (String kw : keywords) {
            if (message.contains(kw)) return true;
        }
        return false;
    }
}