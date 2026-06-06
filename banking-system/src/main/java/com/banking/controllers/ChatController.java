package com.banking.controllers;

import com.banking.dto.request.AdminChatActionRequest;
import com.banking.dto.request.ChatCreateRequest;
import com.banking.dto.request.ChatReplyRequest;
import com.banking.dto.response.ChatResponse;
import com.banking.entities.Chat;
import com.banking.entity.enums.ChatStatut;
import com.banking.repositories.ChatRepository;
import com.banking.services.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chats")
@CrossOrigin(origins = "http://localhost:4200")
public class ChatController {

    private final ChatService chatService;
    private final ChatRepository chatRepository;

    public ChatController(ChatService chatService, ChatRepository chatRepository) {
        this.chatService = chatService;
        this.chatRepository = chatRepository;
    }

    // ============ CLIENT ============

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatResponse create(@Valid @RequestBody ChatCreateRequest req) {
        return chatService.create(req);
    }

    @PutMapping("/{id}/reply")
    public ChatResponse reply(@PathVariable Long id,
                              @Valid @RequestBody ChatReplyRequest req) {
        return chatService.reply(id, req);
    }

    @GetMapping
    public List<ChatResponse> getAll() {
        return chatService.getAll();
    }

    @GetMapping("/{id}")
    public ChatResponse get(@PathVariable Long id) {
        return chatService.get(id);
    }

    @GetMapping("/client/{clientId}")
    public List<ChatResponse> getByClient(@PathVariable Long clientId) {
        return chatService.getByClient(clientId);
    }

    /**
     * Endpoint utilisé par Angular pour charger les notifications
     * des réponses admin déjà enregistrées en base.
     */
    @GetMapping("/client/{clientId}/repondus")
    public ResponseEntity<List<ChatResponse>> getRepondusByClient(@PathVariable Long clientId) {
        List<ChatResponse> responses = chatRepository
                .findByClientIdAndReponseAdminIsNotNullOrderByDateReponseDesc(clientId)
                .stream()
                .map(ChatResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(responses);
    }

    // ============ ADMIN ============

    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ChatResponse> getPending() {
        return chatService.getPendingForAdmin();
    }

    @GetMapping("/admin/pending/count")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Long> countPending() {
        return Map.of("count", chatService.countPending());
    }

    @PostMapping("/admin/{id}/respond")
    @PreAuthorize("hasRole('ADMIN')")
    public ChatResponse respond(@PathVariable Long id,
                                @Valid @RequestBody AdminChatActionRequest req,
                                @AuthenticationPrincipal UserDetails admin) {
        Long adminId = extractAdminId(admin);
        return chatService.respondAsAdmin(id, req.getMessage(), adminId);
    }

    @PostMapping("/admin/{id}/mark-treated")
    @PreAuthorize("hasRole('ADMIN')")
    public ChatResponse markTreated(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails admin) {
        Long adminId = extractAdminId(admin);
        return chatService.markTreated(id, adminId);
    }

    @PostMapping("/admin/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ChatResponse reject(@PathVariable Long id,
                               @Valid @RequestBody AdminChatActionRequest req,
                               @AuthenticationPrincipal UserDetails admin) {
        Long adminId = extractAdminId(admin);
        return chatService.rejectAsAdmin(id, req.getMessage(), adminId);
    }

    private Long extractAdminId(UserDetails admin) {
        if (admin == null) return null;

        try {
            return Long.parseLong(admin.getUsername());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}