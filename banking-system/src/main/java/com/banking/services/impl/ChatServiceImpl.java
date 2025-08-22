package com.banking.services.impl;

import com.banking.dto.request.ChatCreateRequest;
import com.banking.dto.request.ChatReplyRequest;
import com.banking.dto.response.ChatResponse;
import com.banking.entities.Chat;
import com.banking.entities.CompteBancaire;
import com.banking.repositories.ChatRepository;
import com.banking.repositories.CompteBancaireRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ChatServiceImpl implements com.banking.services.ChatService {

    private final ChatRepository chatRepo;
    private final CompteBancaireRepository compteRepo;

    public ChatServiceImpl(ChatRepository chatRepo, CompteBancaireRepository compteRepo) {
        this.chatRepo = chatRepo;
        this.compteRepo = compteRepo;
    }

    @Override
    public ChatResponse create(ChatCreateRequest req) {
        CompteBancaire source = compteRepo.findById(req.getCompteSourceId())
                .orElseThrow(() -> new EntityNotFoundException("Compte source introuvable"));

        Chat chat = new Chat();
        chat.setCompteSource(source);
        chat.setContent(req.getContent());

        if (req.getCompteDestinataireId() != null) {
            CompteBancaire dest = compteRepo.findById(req.getCompteDestinataireId())
                    .orElseThrow(() -> new EntityNotFoundException("Compte destinataire introuvable"));
            chat.setCompteDestinataire(dest);
        }

        // Operation optionnelle : à relier si tu as OperationRepository
        // if (req.getOperationId() != null) { chat.setOperation(operationRepo.getReferenceById(req.getOperationId())); }

        return toDto(chatRepo.save(chat));
    }

    @Override @Transactional(readOnly = true)
    public List<ChatResponse> inbox(Long compteId) {
        return chatRepo.findByCompteDestinataire_IdOrderByCreatedAtDesc(compteId)
                .stream().map(this::toDto).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<ChatResponse> sent(Long compteId) {
        return chatRepo.findByCompteSource_IdOrderByCreatedAtDesc(compteId)
                .stream().map(this::toDto).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<ChatResponse> forAccount(Long compteId) {
        return chatRepo.findByCompteSource_IdOrCompteDestinataire_IdOrderByCreatedAtDesc(compteId, compteId)
                .stream().map(this::toDto).toList();
    }

    @Override
    public ChatResponse markRead(Long chatId, Long lecteurCompteId) {
        Chat chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat introuvable"));

        CompteBancaire lecteur = (lecteurCompteId == null) ? null :
                compteRepo.findById(lecteurCompteId)
                        .orElseThrow(() -> new EntityNotFoundException("Compte lecteur introuvable"));

        chat.marquerCommeLu(lecteur);
        return toDto(chatRepo.save(chat));
    }

    @Override
    public ChatResponse reply(Long chatId, ChatReplyRequest req) {
        Chat chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat introuvable"));

        chat.repondre(req.getReponse());
        return toDto(chatRepo.save(chat));
    }

    @Override @Transactional(readOnly = true)
    public ChatResponse get(Long chatId) {
        Chat chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat introuvable"));
        return toDto(chat);
    }

    private ChatResponse toDto(Chat chat) {
        ChatResponse dto = new ChatResponse();
        dto.id = chat.getId();
        dto.compteSourceId = chat.getCompteSource() != null ? chat.getCompteSource().getId() : null;
        dto.compteDestinataireId = chat.getCompteDestinataire() != null ? chat.getCompteDestinataire().getId() : null;
        dto.operationId = chat.getOperation() != null ? chat.getOperation().getId() : null;
        dto.content = chat.getContent();
        dto.reponse = chat.getReponse();
        dto.statut = chat.getStatut() != null ? chat.getStatut().name() : null;
        dto.luParSource = chat.getLuParSource();
        dto.luParDestinataire = chat.getLuParDestinataire();
        dto.createdAt = chat.getCreatedAt();
        dto.dateReponse = chat.getDateReponse();
        return dto;
    }
}
