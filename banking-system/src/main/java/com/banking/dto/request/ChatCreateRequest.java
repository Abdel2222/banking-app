package com.banking.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ChatCreateRequest {
    @NotNull private Long compteSourceId;
    private Long compteDestinataireId; // null => message "support" (public)
    private Long operationId;          // optionnel, peut rester null

    @NotNull
    @Size(min = 1, max = 1000)
    private String content;

    public Long getCompteSourceId() { return compteSourceId; }
    public void setCompteSourceId(Long compteSourceId) { this.compteSourceId = compteSourceId; }
    public Long getCompteDestinataireId() { return compteDestinataireId; }
    public void setCompteDestinataireId(Long compteDestinataireId) { this.compteDestinataireId = compteDestinataireId; }
    public Long getOperationId() { return operationId; }
    public void setOperationId(Long operationId) { this.operationId = operationId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
