package com.banking.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AdminChatActionRequest {

    /** Message de l'admin au client (utilisé pour respond / reject) */
    @NotBlank
    private String message;

    public AdminChatActionRequest() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}