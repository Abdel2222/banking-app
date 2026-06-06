package com.banking.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Service AES — chiffrement réversible pour les CVV.
 * Contrairement à BCrypt, on peut déchiffrer pour afficher au client.
 */
@Service
public class EncryptionService {

    @Value("${card.encryption.key:TechnoBank2024AES}")
    private String secretKey;

    private SecretKeySpec buildKey() {
        // Clé AES doit faire 16, 24 ou 32 octets
        byte[] keyBytes = new byte[16];
        byte[] raw = secretKey.getBytes();
        System.arraycopy(raw, 0, keyBytes, 0, Math.min(raw.length, keyBytes.length));
        return new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, buildKey());
            return Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Erreur chiffrement CVV", e);
        }
    }

    public String decrypt(String encrypted) {
        try {
            // Si c'est déjà en clair (3 chiffres) ou hashé BCrypt, on retourne tel quel
            if (encrypted == null) return "•••";
            if (encrypted.startsWith("$2a$")) return "•••"; // ancien hash BCrypt
            if (encrypted.length() == 3) return encrypted;  // déjà en clair
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, buildKey());
            return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)));
        } catch (Exception e) {
            return "•••"; // si déchiffrement impossible, masquer
        }
    }
}
