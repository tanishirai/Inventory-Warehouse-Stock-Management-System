package com.stockpilot.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Minimal SHA-256 password hashing. Adequate for a coursework CLI
 * simulator; a production system would add per-user salt and a slow
 * KDF such as bcrypt/Argon2 — noted as a future enhancement in the report.
 */
public final class PasswordUtil {

    private PasswordUtil() { }

    public static String hash(String plaintext) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(plaintext.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
        }
    }

    public static boolean matches(String plaintext, String hash) {
        return hash(plaintext).equals(hash);
    }
}
