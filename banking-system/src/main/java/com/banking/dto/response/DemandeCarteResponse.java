package com.banking.dto.response;

import com.banking.entities.DemandeCarteBancaire;
import com.banking.entity.enums.CardRequestStatus;
import lombok.*;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeCarteResponse {
    private Long id;
    private Long clientId;
    private Long compteId;
    private CardRequestStatus status;
    private LocalDateTime requestedAt;
    private String rejectedReason;

    public static DemandeCarteResponse fromEntity(DemandeCarteBancaire e) {
        if (e == null) return null;
        return DemandeCarteResponse.builder()
                .id(e.getId())
                .clientId(e.getClient() != null ? e.getClient().getId() : null)
                .compteId(e.getCompte() != null ? e.getCompte().getId() : null)
                .status(e.getStatus())
                .requestedAt(extractRequestedAt(e))   // <— plus de dépendance à un getter précis
                .rejectedReason(extractRejectedReason(e))
                .build();
    }

    private static LocalDateTime extractRequestedAt(DemandeCarteBancaire e) {
        for (String m : new String[]{"getRequestedAt", "getDateDemande", "getCreatedAt"}) {
            try {
                Method mm = e.getClass().getMethod(m);
                Object v = mm.invoke(e);
                if (v instanceof LocalDateTime) return (LocalDateTime) v;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static String extractRejectedReason(DemandeCarteBancaire e) {
        for (String m : new String[]{"getRejectedReason", "getRaisonRejet"}) {
            try {
                Method mm = e.getClass().getMethod(m);
                Object v = mm.invoke(e);
                if (v instanceof String) return (String) v;
            } catch (Exception ignored) {}
        }
        return null;
    }
}

