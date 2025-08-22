package com.banking.controllers;

import com.banking.dto.request.ChatCreateRequest;
import com.banking.dto.request.ChatReplyRequest;
import com.banking.dto.response.ChatResponse;
import com.banking.dto.response.ApiResponse;
import com.banking.services.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // Créer un chat
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> create(@Valid @RequestBody ChatCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(chatService.create(req)));
    }

    // Tous les chats où un compte est impliqué (source OU destinataire)
    // GET /api/chats?compteId=11
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatResponse>>> forAccount(@RequestParam(name = "compteId", required = false) Long compteId) {
        if (compteId == null) {
            // si pas de filtre envoyé, on peut retourner une liste vide ou tout (selon ton besoin)
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
        return ResponseEntity.ok(ApiResponse.success(chatService.forAccount(compteId)));
    }

    // Inbox (messages reçus) d’un compte
    @GetMapping("/inbox/{compteId}")
    public ResponseEntity<ApiResponse<List<ChatResponse>>> inbox(@PathVariable Long compteId) {
        return ResponseEntity.ok(ApiResponse.success(chatService.inbox(compteId)));
    }

    // Messages envoyés par un compte
    @GetMapping("/sent/{compteId}")
    public ResponseEntity<ApiResponse<List<ChatResponse>>> sent(@PathVariable Long compteId) {
        return ResponseEntity.ok(ApiResponse.success(chatService.sent(compteId)));
    }

    // Marquer un chat comme lu par un compte (lecteur)
    // PATCH /api/chats/{chatId}/read?lecteurCompteId=2
    @PatchMapping("/{chatId}/read")
    public ResponseEntity<ApiResponse<ChatResponse>> markRead(@PathVariable Long chatId,
                                                              @RequestParam("lecteurCompteId") Long lecteurCompteId) {
        return ResponseEntity.ok(ApiResponse.success(chatService.markRead(chatId, lecteurCompteId)));
    }

    // Répondre à un chat
    @PostMapping("/{chatId}/reply")
    public ResponseEntity<ApiResponse<ChatResponse>> reply(@PathVariable Long chatId,
                                                           @RequestBody(required = false) ChatReplyRequest req,
                                                           @RequestParam(value = "reponse", required = false) String reponse) {
        // On récupère la réponse depuis le body JSON ou le paramètre URL
        String payload = (req != null ? req.getReponse() : null);
        if (payload == null) payload = reponse;

        // Vérification basique
        if (payload == null || payload.trim().isEmpty()) {
            throw new IllegalArgumentException("La réponse ne peut pas être vide");
        }

        ChatReplyRequest effective = new ChatReplyRequest();
        effective.setReponse(payload);

        return ResponseEntity.ok(ApiResponse.success(chatService.reply(chatId, effective)));
    }


    // Récupérer un chat par id
    @GetMapping("/{chatId}")
    public ResponseEntity<ApiResponse<ChatResponse>> get(@PathVariable Long chatId) {
        return ResponseEntity.ok(ApiResponse.success(chatService.get(chatId)));
    }
}
