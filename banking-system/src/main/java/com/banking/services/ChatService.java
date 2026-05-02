package com.banking.services;

import com.banking.dto.request.ChatCreateRequest;
import com.banking.dto.request.ChatReplyRequest;
import com.banking.dto.response.ChatResponse;

import java.util.List;

public interface ChatService {
    ChatResponse create(ChatCreateRequest req);
    List<ChatResponse> inbox(Long compteId);
    List<ChatResponse> sent(Long compteId);
    List<ChatResponse> forAccount(Long compteId);
    ChatResponse markRead(Long chatId, Long lecteurCompteId);
    ChatResponse reply(Long chatId, ChatReplyRequest req);
    ChatResponse get(Long chatId);
}
