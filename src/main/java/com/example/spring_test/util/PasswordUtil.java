package com.example.spring_test.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordUtil {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordUtil() {
    }

    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        if (isBcrypt(encodedPassword)) {
            return ENCODER.matches(rawPassword, encodedPassword);
        }
        return rawPassword.equals(encodedPassword);
    }

    public static boolean needsUpgrade(String encodedPassword) {
        return encodedPassword != null && !isBcrypt(encodedPassword);
    }

    private static boolean isBcrypt(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}