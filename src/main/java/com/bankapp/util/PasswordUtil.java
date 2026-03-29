package com.bankapp.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordUtil {
    private static final String PREFIX = "{pbkdf2}";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hashPassword(String rawPassword) {
        try {
            byte[] salt = new byte[SALT_BYTES];
            SECURE_RANDOM.nextBytes(salt);
            byte[] hash = derive(rawPassword.toCharArray(), salt, ITERATIONS, KEY_BYTES);
            return PREFIX + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing failed.", e);
        }
    }

    public static boolean verifyPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (!isHashed(storedPassword)) {
            return rawPassword.equals(storedPassword);
        }

        try {
            String payload = storedPassword.substring(PREFIX.length());
            String[] parts = payload.split("\\$");
            if (parts.length != 3) {
                return false;
            }

            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            byte[] actual = derive(rawPassword.toCharArray(), salt, iterations, expected.length);
            return MessageDigest.isEqual(actual, expected);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isHashed(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations, int keyBytes) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBytes * 8);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        return factory.generateSecret(spec).getEncoded();
    }
}
