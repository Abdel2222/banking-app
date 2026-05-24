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
        String c = code.toUpperCase();
        if (c.contains("CRY"))  return CRYPTO;
        if (c.contains("IMMO")) return IMMOBILIER;
        if (c.contains("EQ"))   return EQUILIBRE;
        if (c.contains("ACT"))  return ACTIONS;
        return SECURITE;
    }
}