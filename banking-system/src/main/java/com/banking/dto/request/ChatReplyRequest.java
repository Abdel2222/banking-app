package com.banking.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ChatReplyRequest {
    @NotNull
    @Size(min = 1, max = 1000)
    private String reponse;

    public String getReponse() { return reponse; }
    public void setReponse(String reponse) { this.reponse = reponse; }
}
