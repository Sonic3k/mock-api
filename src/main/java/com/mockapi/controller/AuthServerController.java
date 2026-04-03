package com.mockapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulates an OAuth2 Authorization Server + protected resource endpoints.
 * Used to test the Comparison Tool's auth handling.
 *
 * Clients:
 *   client_id=legacy-client,   client_secret=legacy-secret   → scope=legacy:read
 *   client_id=modern-client,   client_secret=modern-secret   → scope=modern:read modern:write
 *   client_id=invalid-client,  client_secret=anything        → 401
 *
 * Static bearer tokens (for BEARER type auth profile):
 *   static-token-legacy  → valid for /protected/legacy/*
 *   static-token-modern  → valid for /protected/modern/*
 *   bad-token            → always 401
 */
@RestController
public class AuthServerController {

    // issued tokens: token → clientId
    private static final Map<String, String> ISSUED_TOKENS = new ConcurrentHashMap<>();

    private static final Map<String, String[]> VALID_CLIENTS = Map.of(
        "legacy-client", new String[]{"legacy-secret", "legacy:read"},
        "modern-client", new String[]{"modern-secret", "modern:read modern:write"}
    );

    static {
        // Pre-seed static bearer tokens
        ISSUED_TOKENS.put("static-token-legacy", "legacy-client");
        ISSUED_TOKENS.put("static-token-modern", "modern-client");
    }

    // ── POST /oauth/token ─────────────────────────────────────────────────────
    @PostMapping(value = "/oauth/token",
                 consumes = {org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                              org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                              "*/*"})
    public ResponseEntity<?> issueToken(
            @RequestParam(required = false) String grant_type,
            @RequestParam(required = false) String client_id,
            @RequestParam(required = false) String client_secret,
            @RequestParam(required = false) String scope) {

        if (grant_type == null) grant_type = "client_credentials";

        if (!"client_credentials".equals(grant_type)) {
            return ResponseEntity.status(400).body(Map.of(
                "error", "unsupported_grant_type",
                "error_description", "Only client_credentials is supported"
            ));
        }

        String[] clientInfo = VALID_CLIENTS.get(client_id);
        if (clientInfo == null || !clientInfo[0].equals(client_secret)) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "invalid_client",
                "error_description", "Invalid client credentials"
            ));
        }

        String token = "oauth2-" + client_id + "-" + System.currentTimeMillis();
        ISSUED_TOKENS.put(token, client_id);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("access_token", token);
        res.put("token_type", "Bearer");
        res.put("expires_in", 3600);
        res.put("scope", clientInfo[1]);
        return ResponseEntity.ok(res);
    }

    // ── Token validation helper ───────────────────────────────────────────────
    private static String validateBearer(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        return ISSUED_TOKENS.get(token); // returns clientId or null
    }

    // ── Protected endpoints ───────────────────────────────────────────────────

    // GET /protected/profile — requires any valid token
    @GetMapping("/protected/profile")
    public ResponseEntity<?> getProtectedProfile(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String clientId = validateBearer(auth);
        if (clientId == null) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "unauthorized",
                "error_description", "Valid Bearer token required"
            ));
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("client_id", clientId);
        res.put("scope", VALID_CLIENTS.getOrDefault(clientId, new String[]{"", "unknown"})[1]);
        res.put("profile", Map.of("name", "API Client", "status", "active"));
        return ResponseEntity.ok(res);
    }

    // GET /protected/data — requires modern:write scope
    @GetMapping("/protected/data")
    public ResponseEntity<?> getProtectedData(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String clientId = validateBearer(auth);
        if (clientId == null) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "unauthorized",
                "error_description", "Valid Bearer token required"
            ));
        }
        String[] info = VALID_CLIENTS.get(clientId);
        if (info == null || !info[1].contains("modern:write")) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "insufficient_scope",
                "error_description", "Requires modern:write scope",
                "client_id", clientId
            ));
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("data", List.of(
            Map.of("id", 1, "value", "record-one"),
            Map.of("id", 2, "value", "record-two"),
            Map.of("id", 3, "value", "record-three")
        ));
        res.put("total", 3);
        return ResponseEntity.ok(res);
    }

    // GET /protected/admin — requires legacy-client only (simulates legacy-only endpoint)
    @GetMapping("/protected/admin")
    public ResponseEntity<?> getAdminResource(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String clientId = validateBearer(auth);
        if (clientId == null) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "unauthorized",
                "error_description", "Valid Bearer token required"
            ));
        }
        if (!"legacy-client".equals(clientId)) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "forbidden",
                "error_description", "This resource requires legacy-client credentials"
            ));
        }
        return ResponseEntity.ok(Map.of("admin", true, "client", clientId, "access_level", "full"));
    }
}
