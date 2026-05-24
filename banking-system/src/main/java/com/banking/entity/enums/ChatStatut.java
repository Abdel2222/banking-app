package com.banking.entities;

public enum ChatStatut {
    /** Message normal (Ollama a répondu, pas d'intervention humaine requise) */
    NORMAL,

    /** Demande sensible en attente de traitement par un admin */
    EN_ATTENTE,

    /** Un admin a répondu / traité la demande */
    REPONDU,

    /** L'admin a refusé la demande */
    REJETE
}