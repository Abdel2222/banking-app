package com.banking.services;

import com.banking.dto.request.ChatCreateRequest;
import com.banking.dto.request.ChatReplyRequest;
import com.banking.dto.response.ChatResponse;

import java.util.List;

public interface ChatService {

    // ===== CLIENT =====
    ChatResponse create(ChatCreateRequest req);
    ChatResponse reply(Long chatId, ChatReplyRequest req);
    List<ChatResponse> getAll();
    ChatResponse get(Long chatId);

    /** Historique des messages d'un client donné */
    List<ChatResponse> getByClient(Long clientId);

    // ===== ADMIN =====

    /** Liste des demandes sensibles en attente de traitement */
    List<ChatResponse> getPendingForAdmin();

    /** Compteur des demandes en attente (pour le badge) */
    long countPending();

    /** Admin répond manuellement à une demande */
    ChatResponse respondAsAdmin(Long chatId, String adminMessage, Long adminId);

    /** Admin marque comme traité sans message particulier */
    ChatResponse markTreated(Long chatId, Long adminId);

    /** Admin refuse la demande */
    ChatResponse rejectAsAdmin(Long chatId, String motif, Long adminId);
}