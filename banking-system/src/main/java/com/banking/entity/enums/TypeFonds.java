package com.banking.entity.enums;

public enum TypeFonds {
    CRYPTO(30),
    SECURITE(365),
    EQUILIBRE(730),
    ACTIONS(1095),
    IMMOBILIER(3650);

    private final int dureeJours;

    TypeFonds(int dureeJours) { this.dureeJours = dureeJours; }

    public int getDureeJours() { return dureeJours; }

    public static TypeFonds fromCode(String code) {
        if (code == null) return SECURITE;
        String c = code.toUpperCase().trim();

        if (c.contains("CRY")  || c.contains("CRYPTO"))     return CRYPTO;
        if (c.contains("IMMO") || c.contains("IMM"))        return IMMOBILIER;
        if (c.contains("EQ")   || c.contains("EQUIL"))      return EQUILIBRE;
        if (c.contains("ACT")  || c.contains("ACTION"))     return ACTIONS;
        if (c.contains("SEC")  || c.contains("SECU"))       return SECURITE;

        // ✅ Log pour détecter les codes non reconnus
        System.out.println("⚠️ TypeFonds.fromCode() — code non reconnu : '" + code + "' → défaut SECURITE");
        return SECURITE;
    }

    /**
     * Depuis le nom complet du fonds (fallback si codeIdentification absent)
     */
    public static TypeFonds fromNom(String nom) {
        if (nom == null) return SECURITE;
        String n = nom.toUpperCase().trim();

        if (n.contains("CRYPTO") || n.contains("BITCOIN") ||
                n.contains("WEB3")   || n.contains("DEFI") ||
                n.contains("ETHEREUM") || n.contains("BLOCKCHAIN")) return CRYPTO;

        if (n.contains("IMMO")   || n.contains("LOCATIF") ||
                n.contains("MAISON") || n.contains("FONCIER"))      return IMMOBILIER;

        if (n.contains("EQUILIB") || n.contains("MIXTE"))       return EQUILIBRE;

        if (n.contains("ACTION") || n.contains("TECHNOLOG") ||
                n.contains("SEMI")   || n.contains("ENERGIE") ||
                n.contains("IA")     || n.contains("SEMICONDUC"))   return ACTIONS;

        return SECURITE;
    }
}