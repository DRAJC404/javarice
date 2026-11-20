package com.bai.inventory.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    private static final int LOG_ROUNDS = 12;

    public static String sha256Hex(String rawText) {
        if (rawText == null) return null;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public static String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(LOG_ROUNDS));
    }

    public static boolean verify(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, storedHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean runStartupSelfCheck() {
        try {
            String testPass = "1234";
            String seedHash = "$2a$12$s0Hn2VYxfAcBRGor2.vDauP3/SA1niIuG64FYo8tKBk3C9As1F3Qi";
            boolean verifies = verify(testPass, seedHash);
            String newlyHashed = hash("test_pass");
            boolean newlyVerified = verify("test_pass", newlyHashed);
            return verifies && newlyVerified;
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {
        boolean ok = runStartupSelfCheck();
        System.out.println("BCrypt $2a startup verification: " + (ok ? "SUCCESS" : "FAILED"));
    }
}
