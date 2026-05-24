package com.banking.services.impl;

import com.banking.dto.request.ChatCreateRequest;
import com.banking.dto.request.ChatReplyRequest;
import com.banking.dto.response.ChatResponse;
import com.banking.entities.Chat;
import com.banking.entities.ChatActionType;
import com.banking.entities.ChatStatut;
import com.banking.repositories.ChatRepository;
import com.banking.services.IntentDetector;
import com.banking.services.OllamaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ChatServiceImpl implements com.banking.services.ChatService {

    private final ChatRepository chatRepo;
    private final OllamaService ollamaService;
    private final IntentDetector intentDetector;

    public ChatServiceImpl(ChatRepository chatRepo,
                           OllamaService ollamaService,
                           IntentDetector intentDetector) {
        this.chatRepo       = chatRepo;
        this.ollamaService  = ollamaService;
        this.intentDetector = intentDetector;
    }

    // ===== CLIENT =====

    @Override
    public ChatResponse create(ChatCreateRequest req) {
        Chat chat = new Chat(req.getContenu());

        // Rattachement client (depuis le DTO, sinon laisse vide)
        chat.setClientId(req.getClientId());
        chat.setClientNom(req.getClientNom());
        chat.setNumCompte(req.getNumCompte());

        // 1. Détection d'intention sensible
        ChatActionType action = intentDetector.detect(req.getContenu());
        chat.setActionType(action);

        if (intentDetector.requiresAdmin(action)) {
            // → Demande sensible : on bypass Ollama, on met en attente admin
            chat.setStatut(ChatStatut.EN_ATTENTE);
            chat.setReponse(intentDetector.buildAcknowledgement(action));
        } else {
            // → Conversation normale : Ollama répond
            chat.setStatut(ChatStatut.NORMAL);

            String reponseIA = ollamaService.repondre(req.getContenu());
            chat.setReponse(reponseIA != null
                    ? reponseIA
                    : "Service IA temporairement indisponible. Un conseiller vous répondra.");
        }

        return ChatResponse.fromEntity(chatRepo.save(chat));
    }

    @Override
    public ChatResponse reply(Long chatId, ChatReplyRequest req) {
        Chat chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Message introuvable"));
        chat.setReponse(req.getReponse());
        return ChatResponse.fromEntity(chatRepo.save(chat));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatResponse> getAll() {
        return chatRepo.findAllByOrderByDateHeureDesc()
                .stream().map(ChatResponse::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChatResponse get(Long chatId) {
        Chat chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Message introuvable"));
        return ChatResponse.fromEntity(chat);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatResponse> getByClient(Long clientId) {
        return chatRepo.findByClientIdOrderByDateHeureDesc(clientId)
                .stream().map(ChatResponse::fromEntity).toList();
    }

    // ===== ADMIN =====

    @Override
    @Transactional(readOnly = true)
    public List<ChatResponse> getPendingForAdmin() {
        return chatRepo.findByStatutOrderByDateHeureDesc(ChatStatut.EN_ATTENTE)
                .stream().map(ChatResponse::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPending() {
        return chatRepo.countByStatut(ChatStatut.EN_ATTENTE);
    }

    @Override
    public ChatResponse respondAsAdmin(Long chatId, String adminMessage, Long adminId) {
        Chat chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));

        chat.setReponseAdmin(adminMessage);
        chat.setAdminId(adminId);
        chat.setDateReponse(LocalDateTime.now());
        chat.setStatut(ChatStatut.REPONDU);

        return ChatResponse.fromEntity(chatRepo.save(chat));
    }

    @Override
    public ChatResponse markTreated(Long chatId, Long adminId) {
        Chat chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));

        chat.setAdminId(adminId);
        chat.setDateReponse(LocalDateTime.now());
        chat.setStatut(ChatStatut.REPONDU);

        if (chat.getReponseAdmin() == null || chat.getReponseAdmin().isBlank()) {
            chat.setReponseAdmin("Votre demande a été traitée par un conseiller.");
        }

        return ChatResponse.fromEntity(chatRepo.save(chat));
    }

    @Override
    public ChatResponse rejectAsAdmin(Long chatId, String motif, Long adminId) {
        Chat chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));

        chat.setReponseAdmin("❌ Demande refusée. Motif : "
                + (motif != null && !motif.isBlank() ? motif : "non précisé"));
        chat.setAdminId(adminId);
        chat.setDateReponse(LocalDateTime.now());
        chat.setStatut(ChatStatut.REJETE);

        return ChatResponse.fromEntity(chatRepo.save(chat));
    }
}