package com.banking.services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class OllamaService {

    private final ChatClient chatClient;

    public OllamaService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                Tu es un conseiller bancaire virtuel professionnel de T€chno-Bank.
                Tu réponds TOUJOURS en français, de manière courte (3-4 phrases max).
                Tu aides les clients avec : comptes, virements, cartes, épargne, frais.
                Si tu ne peux pas résoudre, dis que tu escalades vers un conseiller humain.
                Ne jamais inventer des données personnelles du client.
                """)
                .build();
    }

    public String repondre(String messageClient) {
        try {
            return chatClient.prompt()
                    .user(messageClient)
                    .call()
                    .content();
        } catch (Exception e) {
            return null; // Ollama indisponible → pas de réponse auto
        }
    }
}