package com.mockapi.config;

import org.springframework.stereotype.Component;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared token validator — both legacy and modernized controllers use this.
 * Tokens are issued by AuthServerController and stored here.
 */
@Component
public class TokenValidator {

    // Shared token store — AuthServerController writes, controllers read
    public static final Map<String, String> ISSUED_TOKENS = new ConcurrentHashMap<>();

    static {
        // Pre-seeded static tokens for convenience
        ISSUED_TOKENS.put("static-token-legacy", "legacy-client");
        ISSUED_TOKENS.put("static-token-modern", "modern-client");
    }

    /**
     * Returns clientId if valid, null if invalid/missing.
     */
    public String validate(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return ISSUED_TOKENS.get(authHeader.substring(7).trim());
    }

    public boolean isValid(String authHeader) {
        return validate(authHeader) != null;
    }

    public static ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(401).body(Map.of(
            "error", "unauthorized",
            "error_description", "Valid Bearer token required"
        ));
    }
}
