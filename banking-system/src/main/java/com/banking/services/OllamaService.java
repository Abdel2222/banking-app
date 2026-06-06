package com.banking.services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class OllamaService {

    private final ChatClient chatClient;

    public OllamaService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                Tu es Alex, conseiller bancaire virtuel de T€chno-Bank, une banque digitale belge.
                Réponds TOUJOURS en français, en 2-3 phrases maximum, ton professionnel et chaleureux.
                Ne jamais inventer de données personnelles. Ne jamais demander le PIN ou mot de passe.
                """)
                .build();
    }

    public String repondre(String message, String numCompte, String clientNom) {
        // ✅ Réponses instantanées — comme un vrai chatbot bancaire
        String rapide = getReponseRapide(message, clientNom);
        if (rapide != null) return rapide;

        // Fallback Ollama si aucune correspondance
        try {
            StringBuilder ctx = new StringBuilder();
            if (clientNom != null && !clientNom.isBlank())
                ctx.append("Client : ").append(clientNom).append(". ");
            if (numCompte != null && !numCompte.isBlank())
                ctx.append("Compte : ").append(numCompte).append(". ");
            String msg = ctx.isEmpty() ? message : "[CONTEXTE: " + ctx + "]\n" + message;
            return chatClient.prompt().user(msg).call().content();
        } catch (Exception e) {
            return "Je suis désolé, le service est temporairement indisponible. Un conseiller prendra en charge votre demande rapidement.";
        }
    }

    public String repondre(String message) {
        return repondre(message, null, null);
    }

    private String getReponseRapide(String message, String clientNom) {
        if (message == null || message.isBlank()) return null;
        String m = message.toLowerCase().trim();
        String prenom = (clientNom != null && !clientNom.isBlank())
                ? clientNom.split(" ")[0] : "cher client";

        // ===== SALUTATIONS =====
        if (containsAny(m, "bonjour", "salut", "hello", "bonsoir", "coucou", "bonne journée"))
            return "Bonjour " + prenom + " ! 👋 Je suis Alex, votre conseiller T€chno-Bank. Comment puis-je vous aider aujourd'hui ?";

        if (containsAny(m, "merci", "parfait", "super", "excellent", "c'est bon", "nickel"))
            return "Avec plaisir " + prenom + " ! Je suis là si vous avez d'autres questions. 😊";

        if (containsAny(m, "au revoir", "bonne soirée", "bye", "à bientôt", "ciao"))
            return "Au revoir " + prenom + " ! Bonne journée et à bientôt sur T€chno-Bank. 👋";

        // ===== COMPTE =====
        if (containsAny(m, "solde", "combien j'ai", "combien ai-je", "argent sur mon compte", "montant disponible"))
            return "Votre solde en temps réel est disponible dans la section **Mon Compte** de votre tableau de bord. 💰 Pour toute anomalie, n'hésitez pas à me contacter.";

        if (containsAny(m, "ouvrir un compte", "créer un compte", "nouveau compte"))
            return "Pour ouvrir un compte chez T€chno-Bank, rendez-vous dans la section **Inscription** de notre application. Un conseiller vous guidera dans les démarches. 🏦";

        if (containsAny(m, "fermer mon compte", "clôturer", "résilier"))
            return "La clôture de compte est une opération sensible qui nécessite l'intervention d'un conseiller. Je transmets votre demande immédiatement. 📋";

        if (containsAny(m, "compte suspendu", "compte bloqué", "accès refusé", "ne peux pas accéder", "compte inactif"))
            return "Je vois que votre compte rencontre un problème d'accès. Je transmets votre demande en priorité à notre équipe. Vous serez contacté rapidement. ⚡";

        if (containsAny(m, "rib", "coordonnées bancaires", "numéro de compte", "iban"))
            return "Votre RIB et IBAN sont disponibles dans **Mon Compte → Informations bancaires**. Vous pouvez le télécharger directement en PDF. 📄";

        // ===== VIREMENT =====
        if (containsAny(m, "faire un virement", "envoyer de l'argent", "comment virer", "effectuer un transfert"))
            return "Pour effectuer un virement, rendez-vous dans **Virements** → **Nouveau virement**. Saisissez l'IBAN du bénéficiaire et le montant. Le virement est traité immédiatement. 💸";

        if (containsAny(m, "virement en attente", "virement pas reçu", "virement bloqué", "virement échoué", "problème de virement", "problème avec mon virement"))
            return "Je transmets votre problème de virement à un conseiller en priorité. Pouvez-vous noter la date et le montant concernés ? Notre équipe vous contactera sous 24h. ⚠️";

        if (containsAny(m, "virement international", "virement étranger", "swift", "sepa"))
            return "T€chno-Bank prend en charge les virements SEPA (zone euro) et internationaux SWIFT. Des frais peuvent s'appliquer selon la destination. 🌍";

        if (containsAny(m, "annuler un virement", "annuler le virement"))
            return "Un virement déjà exécuté ne peut pas être annulé directement. Contactez immédiatement notre service client pour bloquer l'opération si elle est encore en cours. ⚡";

        // ===== CARTE BANCAIRE =====
        if (containsAny(m, "ma carte", "carte bancaire", "carte visa", "carte mastercard", "informations carte"))
            return "Toutes les informations sur votre carte (numéro, expiration, plafonds) sont disponibles dans la section **Ma Carte** de votre espace client. 💳";

        if (containsAny(m, "paiement refusé", "carte refusée", "transaction refusée", "paiement impossible"))
            return "Un paiement refusé peut être dû à un plafond atteint ou à une restriction de sécurité. Vérifiez vos plafonds dans **Ma Carte** ou contactez un conseiller. 🔍";

        if (containsAny(m, "augmenter plafond", "modifier plafond", "changer plafond", "limite carte"))
            return "Vos plafonds de paiement (journalier et mensuel) sont modifiables directement dans **Ma Carte → Plafonds**. Les modifications sont effectives immédiatement. ⚙️";

        if (containsAny(m, "code pin", "pin oublié", "changer mon code", "modifier pin"))
            return "Pour des raisons de sécurité, je ne peux pas traiter les demandes liées au code PIN par ce canal. Rendez-vous en agence ou appelez le 0800 123 456. 🔒";

        if (containsAny(m, "carte expirée", "ma carte expire", "renouvellement carte"))
            return "Le renouvellement de carte se fait automatiquement 30 jours avant l'expiration. Si vous n'avez pas reçu la nouvelle carte, contactez un conseiller. 📬";

        // ===== ÉPARGNE =====
        if (containsAny(m, "taux", "taux épargne", "intérêt", "livret", "capitalisation", "rendement épargne"))
            return "Votre livret d'épargne T€chno-Bank est rémunéré à **3% annuel** (soit 0.25% mensuel). Les intérêts sont capitalisés automatiquement chaque mois sur votre solde. 📈";

        if (containsAny(m, "ouvrir épargne", "créer livret", "compte épargne", "épargner"))
            return "Un compte épargne est créé automatiquement lors de l'activation de votre compte courant. Consultez la section **Épargne** pour suivre vos intérêts. 💰";

        if (containsAny(m, "retirer épargne", "retrait épargne", "débloquer épargne"))
            return "Vous pouvez effectuer un virement de votre compte épargne vers votre compte courant à tout moment depuis la section **Virements**. 💸";

        // ===== PLACEMENTS =====
        if (containsAny(m, "placement", "investissement", "fonds", "investir", "portefeuille"))
            return "T€chno-Bank propose plusieurs fonds d'investissement avec des rendements variés. Consultez la section **Placements** pour découvrir les options et simuler vos gains. 📊";

        if (containsAny(m, "risque placement", "risque investissement", "sécurité placement"))
            return "Chaque fonds T€chno-Bank a un profil de risque clairement indiqué. Les fonds sécurisés offrent un rendement stable, les fonds dynamiques un potentiel plus élevé. 📋";

        // ===== FRAIS =====
        if (containsAny(m, "frais", "commission", "prélèvement", "tenue de compte", "coût"))
            return "Vos frais de gestion sont visibles dans **Mon Compte → Opérations**. Pour toute contestation de frais, je peux ouvrir une réclamation auprès de notre service client. 📋";

        // ===== SÉCURITÉ =====
        if (containsAny(m, "mot de passe oublié", "réinitialiser mot de passe", "connexion impossible"))
            return "Pour réinitialiser votre mot de passe, cliquez sur **Mot de passe oublié** sur la page de connexion. Un lien sécurisé vous sera envoyé par email. 🔐";

        if (containsAny(m, "double authentification", "2fa", "authentification", "sécurité compte"))
            return "T€chno-Bank utilise une authentification sécurisée pour protéger votre compte. Activez la double authentification dans **Paramètres → Sécurité**. 🛡️";

        // ===== AIDE GÉNÉRALE =====
        if (containsAny(m, "aide", "help", "que peux-tu faire", "comment ça marche", "fonctionnalités"))
            return "Je peux vous aider avec : solde et opérations, virements, carte bancaire, épargne et placements, frais et sécurité. Pour les demandes sensibles, je les transmets directement à un conseiller. 🏦";

        if (containsAny(m, "agence", "horaires", "téléphone", "contact", "appeler"))
            return "T€chno-Bank est disponible 7j/7 via ce chat. Pour joindre un conseiller par téléphone : **0800 123 456** (gratuit). Agence principale : Bruxelles Centre. 📞";

        // Aucune correspondance → Ollama
        return null;
    }

    private boolean containsAny(String message, String... keywords) {
        for (String kw : keywords) {
            if (message.contains(kw)) return true;
        }
        return false;
    }
}