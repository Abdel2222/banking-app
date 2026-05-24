package com.banking.entities;

public enum ChatActionType {
    /** Conversation normale, pas d'action particulière */
    NORMAL,

    /** Le client demande à bloquer sa carte */
    BLOQUER_CARTE,

    /** Le client demande à débloquer sa carte */
    DEBLOQUER_CARTE,

    /** Carte perdue ou volée */
    PERTE_VOL_CARTE,

    /** Le client demande à suspendre son compte */
    SUSPENDRE_COMPTE,

    /** Le client demande à réactiver son compte */
    REACTIVER_COMPTE,

    /** Signalement de fraude / transaction frauduleuse */
    SIGNALER_FRAUDE,

    /** Réclamation / litige */
    RECLAMATION,

    /** Autre demande nécessitant validation humaine */
    AUTRE_SENSIBLE
}