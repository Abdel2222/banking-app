package com.banking.controllers;

import com.banking.dto.request.AdminChatActionRequest;
import com.banking.dto.request.ChatCreateRequest;
import com.banking.dto.request.ChatReplyRequest;
import com.banking.dto.response.ChatResponse;
import com.banking.services.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
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

    /** Récupère l'historique d'un client (pour la persistance côté frontend) */
    @GetMapping("/client/{clientId}")
    public List<ChatResponse> getByClient(@PathVariable Long clientId) {
        return chatService.getByClient(clientId);
    }

    // ============ ADMIN ============

    /** Liste des demandes en attente */
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ChatResponse> getPending() {
        return chatService.getPendingForAdmin();
    }

    /** Compteur des demandes en attente (pour le badge) */
    @GetMapping("/admin/pending/count")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Long> countPending() {
        return Map.of("count", chatService.countPending());
    }

    /** Admin répond à une demande */
    @PostMapping("/admin/{id}/respond")
    @PreAuthorize("hasRole('ADMIN')")
    public ChatResponse respond(@PathVariable Long id,
                                @Valid @RequestBody AdminChatActionRequest req,
                                @AuthenticationPrincipal UserDetails admin) {
        Long adminId = extractAdminId(admin);
        return chatService.respondAsAdmin(id, req.getMessage(), adminId);
    }

    /** Admin marque comme traité sans message particulier */
    @PostMapping("/admin/{id}/mark-treated")
    @PreAuthorize("hasRole('ADMIN')")
    public ChatResponse markTreated(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails admin) {
        Long adminId = extractAdminId(admin);
        return chatService.markTreated(id, adminId);
    }

    /** Admin refuse la demande avec un motif */
    @PostMapping("/admin/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ChatResponse reject(@PathVariable Long id,
                               @Valid @RequestBody AdminChatActionRequest req,
                               @AuthenticationPrincipal UserDetails admin) {
        Long adminId = extractAdminId(admin);
        return chatService.rejectAsAdmin(id, req.getMessage(), adminId);
    }

    /**
     * Extrait l'ID admin depuis le principal.
     * Adapte cette méthode si ton UserDetails a un champ id différent.
     */
    private Long extractAdminId(UserDetails admin) {
        if (admin == null) return null;
        try {
            return Long.parseLong(admin.getUsername());
        } catch (NumberFormatException e) {
            return null; // Username n'est pas un ID numérique → laisser null
        }
    }
}