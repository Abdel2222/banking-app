package com.banking.dto.response;

import java.time.LocalDateTime;

public class ChatResponse {
    public Long id;
    public Long compteSourceId;
    public Long compteDestinataireId;
    public Long operationId;
    public String content;
    public String reponse;
    public String statut; // ENVOYE, RECU, LU, REPONDU
    public Boolean luParSource;
    public Boolean luParDestinataire;
    public LocalDateTime createdAt;
    public LocalDateTime dateReponse;
}

