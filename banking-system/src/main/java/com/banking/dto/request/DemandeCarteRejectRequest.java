package com.banking.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandeCarteRejectRequest {
    private String reason;
}
