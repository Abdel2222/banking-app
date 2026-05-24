package com.banking.services;

import com.banking.entities.ChatActionType;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;

/**
 * Détecte les intentions sensibles dans un message client pour bypass Ollama
 * et router vers un admin humain.
 */
@Service
public class IntentDetector {

    /**
     * Mapping mots-clés → action.
     * L'ordre compte : les patterns les plus spécifiques d'abord.
     */
    private static final Map<ChatActionType, List<String>> KEYWORDS = Map.of(

            ChatActionType.PERTE_VOL_CARTE, List.of(
                    "carte perdue", "carte volee", "j'ai perdu ma carte",
                    "perte de carte", "vol de carte", "ma carte est perdue",
                    "ma carte est volee", "carte vol"
            ),

            ChatActionType.BLOQUER_CARTE, List.of(
                    "bloquer ma carte", "bloquer carte", "bloque ma carte",
                    "bloquer la carte", "veux bloquer", "souhaite bloquer",
                    "stopper ma carte", "desactiver ma carte", "desactiver carte"
            ),

            ChatActionType.DEBLOQUER_CARTE, List.of(
                    "debloquer ma carte", "debloquer carte", "debloque ma carte",
                    "reactiver ma carte", "reactiver carte"
            ),

            ChatActionType.SUSPENDRE_COMPTE, List.of(
                    "suspendre mon compte", "suspendre compte", "fermer mon compte",
                    "fermer compte", "cloturer mon compte", "cloturer compte",
                    "resilier mon compte"
            ),

            ChatActionType.REACTIVER_COMPTE, List.of(
                    "reactiver mon compte", "reactiver compte", "rouvrir mon compte",
                    "rouvrir compte"
            ),

            ChatActionType.SIGNALER_FRAUDE, List.of(
                    "fraude", "transaction frauduleuse", "operation frauduleuse",
                    "je suis victime", "piratage", "piratee", "compte pirate",
                    "carte piratee", "achat suspect", "prelevement inconnu"
            ),

            ChatActionType.RECLAMATION, List.of(
                    "reclamation", "litige", "porter plainte",
                    "contester", "contestation"
            )
    );

    /**
     * Analyse le message et retourne le type d'action détecté.
     * Retourne NORMAL si aucune intention sensible n'est trouvée.
     */
    public ChatActionType detect(String message) {
        if (message == null || message.isBlank()) {
            return ChatActionType.NORMAL;
        }

        String normalized = normalize(message);

        for (Map.Entry<ChatActionType, List<String>> entry : KEYWORDS.entrySet()) {
            for (String kw : entry.getValue()) {
                if (normalized.contains(kw)) {
                    return entry.getKey();
                }
            }
        }

        return ChatActionType.NORMAL;
    }

    /**
     * True si l'action détectée nécessite l'intervention d'un admin.
     */
    public boolean requiresAdmin(ChatActionType action) {
        return action != null && action != ChatActionType.NORMAL;
    }

    /**
     * Message de confirmation système envoyé au client quand sa demande
     * est mise en attente d'un admin.
     */
    public String buildAcknowledgement(ChatActionType action) {
        String label = switch (action) {
            case BLOQUER_CARTE      -> "blocage de votre carte";
            case DEBLOQUER_CARTE    -> "déblocage de votre carte";
            case PERTE_VOL_CARTE    -> "perte / vol de carte";
            case SUSPENDRE_COMPTE   -> "suspension de votre compte";
            case REACTIVER_COMPTE   -> "réactivation de votre compte";
            case SIGNALER_FRAUDE    -> "signalement de fraude";
            case RECLAMATION        -> "réclamation";
            default                 -> "demande";
        };

        return "✅ Votre demande de " + label + " a bien été transmise à un conseiller. "
                + "Un membre de notre équipe vous répondra dans les plus brefs délais.";
    }

    /** Normalise : minuscules + suppression des accents */
    private String normalize(String s) {
        String lower = s.toLowerCase();
        String stripped = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return stripped;
    }
}