package com.banking.services;

import com.banking.entity.enums.ChatActionType;
import org.springframework.stereotype.Service;

@Service
public class IntentDetector {

    public ChatActionType detect(String message) {
        if (message == null || message.isBlank()) return ChatActionType.NORMAL;

        String m = message.toLowerCase().trim();

        // ===== BLOCAGE CARTE =====
        if (containsAny(m,
                "bloquer ma carte", "bloquer carte", "bloquer ma cb",
                "je veux bloquer", "block ma carte", "suspendre ma carte",
                "désactiver ma carte", "ma carte est perdue", "carte perdue",
                "carte volée", "ma carte a été volée", "j'ai perdu ma carte")) {
            return ChatActionType.BLOCAGE_CARTE;
        }

        // ===== REMPLACEMENT CARTE =====
        if (containsAny(m,
                "changer cvv", "nouveau cvv", "cvv oublié", "cvv perdu",
                "renouveler ma carte", "remplacer ma carte", "nouvelle carte",
                "carte abîmée", "carte endommagée", "carte expirée",
                "ma carte est abîmée", "je veux une nouvelle carte")) {
            return ChatActionType.REMPLACEMENT_CARTE;
        }

        // ===== FRAUDE / CVV COMPROMIS =====
        if (containsAny(m,
                "fraude", "frauduleux", "transaction frauduleuse",
                "paiement inconnu", "achat que je n'ai pas fait",
                "quelqu'un utilise ma carte", "cvv", "code sécurité",
                "carte compromise", "piratage")) {
            return ChatActionType.FRAUDE_CVV;
        }

        // ===== DEBLOCAGE CARTE =====
        if (containsAny(m,
                "débloquer ma carte", "débloquer carte", "réactiver ma carte",
                "activer ma carte", "ma carte ne fonctionne pas")) {
            return ChatActionType.DEBLOCAGE_CARTE;
        }

        // ===== VIREMENT =====
        if (containsAny(m,
                "virement", "transfert", "envoyer de l'argent",
                "virement bloqué", "virement échoué", "problème de virement",
                "virement en attente")) {
            return ChatActionType.PROBLEME_VIREMENT;
        }

        // ===== COMPTE =====
        if (containsAny(m,
                "compte bloqué", "compte suspendu", "accès refusé",
                "solde incorrect", "erreur sur mon compte",
                "je ne peux pas accéder", "problème compte")) {
            return ChatActionType.PROBLEME_COMPTE;
        }

        return ChatActionType.NORMAL;
    }

    public boolean requiresAdmin(ChatActionType action) {
        return switch (action) {
            case BLOCAGE_CARTE,
                    DEBLOCAGE_CARTE,
                    FRAUDE_CVV,
                    REMPLACEMENT_CARTE,
                    PROBLEME_VIREMENT,
                    PROBLEME_COMPTE -> true;
            default -> false;
        };
    }

    public String buildAcknowledgement(ChatActionType action) {
        return switch (action) {
            case BLOCAGE_CARTE ->
                    "🔒 Votre carte a été bloquée temporairement. " +
                            "Un conseiller va traiter votre demande.";

            case DEBLOCAGE_CARTE ->
                    "🔓 Votre demande de déblocage de carte a été transmise. " +
                            "Un conseiller va examiner votre demande et vous répondre.";

            case FRAUDE_CVV ->
                    "⚠️ Votre signalement de fraude a bien été reçu. " +
                            "Un conseiller va traiter votre dossier en priorité et " +
                            "sécuriser votre carte. Nous vous contacterons rapidement.";

            case REMPLACEMENT_CARTE ->
                    "💳 Votre demande de remplacement de carte a été reçue. " +
                            "Un conseiller va bloquer votre ancienne carte et en émettre une nouvelle. " +
                            "Elle sera disponible dans votre espace client sous quelques minutes.";

            case PROBLEME_VIREMENT ->
                    "💸 Votre signalement concernant un virement a bien été reçu. " +
                            "Un conseiller va vérifier votre dossier et vous apporter " +
                            "une réponse dans les 24h.";

            case PROBLEME_COMPTE ->
                    "🏦 Votre problème de compte a été transmis à notre équipe. " +
                            "Un conseiller va examiner votre situation et vous contacter.";

            default ->
                    "✅ Votre demande a été transmise à un conseiller. " +
                            "Vous recevrez une réponse rapidement.";
        };
    }

    private boolean containsAny(String message, String... keywords) {
        for (String kw : keywords) {
            if (message.contains(kw)) return true;
        }
        return false;
    }
}