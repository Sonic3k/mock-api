package com.mockapi.controller.modernized;

import com.mockapi.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/modernized/api/auth")
public class ModernizedAuthController {

    private final UserRepository userRepo;

    public ModernizedAuthController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    // TC-A01: POST /auth/login
    // DIFF: 500 error for suspended users instead of 403, token field renamed
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body) {
        String email = (String) body.getOrDefault("email", "");
        return userRepo.findByEmail(email).map(u -> {
            if ("suspended".equals(u.getStatus())) {
                // DIFF: returns 500 instead of 403 (bug in modernized)
                return ResponseEntity.status(500).body(Map.of(
                        "error", "Internal Server Error",
                        "message", "Unexpected error during authentication"
                ));
            }
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("accessToken", "modernized.jwt." + u.getId());   // DIFF: token → accessToken
            res.put("tokenType", "Bearer");                            // DIFF: token_type → tokenType
            res.put("expiresIn", 3600);                               // DIFF: expires_in → expiresIn
            res.put("refreshToken", "modernized.refresh." + u.getId()); // DIFF: snake → camel
            res.put("userId", u.getId());
            res.put("userName", u.getName());
            res.put("issuedAt", System.currentTimeMillis() / 1000);   // DIFF: ISO string → epoch
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid credentials", "code", "INVALID_CREDENTIALS")));
    }

    // TC-A02: POST /auth/register — SAME result (pass)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody(required = false) Map<String, Object> body,
                                       @RequestParam(required = false) String username,
                                       @RequestParam(required = false) String email) {
        Map<String, Object> res = new LinkedHashMap<>();
        String resolvedEmail = email != null ? email :
                (body != null ? (String) body.getOrDefault("email", "new@example.com") : "new@example.com");
        res.put("userId", 1001L);
        res.put("username", username != null ? username : "newuser");
        res.put("email", resolvedEmail);
        res.put("status", "pending_verification");
        res.put("createdAt", "2024-01-15T10:30:00Z");
        res.put("verificationRequired", true);
        res.put("message", "Registration successful. Please verify your email.");
        return ResponseEntity.status(201).body(res);
    }

    // TC-A03: POST /auth/refresh — SAME (pass)
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, Object> body) {
        String refreshToken = (String) body.getOrDefault("refresh_token", "");
        if (refreshToken.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "refresh_token is required"));
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("accessToken", "modernized.new.jwt." + System.currentTimeMillis());
        res.put("tokenType", "Bearer");
        res.put("expiresIn", 3600);
        res.put("refreshToken", "modernized.new.refresh." + System.currentTimeMillis());
        res.put("issuedAt", System.currentTimeMillis() / 1000);
        return ResponseEntity.ok(res);
    }

    // TC-A04: POST /auth/logout — NOT IMPLEMENTED
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.status(404).body(Map.of(
                "error", "Not implemented in modernized version",
                "code", "NOT_IMPLEMENTED",
                "note", "Use token expiry or client-side logout in v2"
        ));
    }

    // TC-A05: POST /auth/forgot-password — DIFF: extra fields in response
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, Object> body) {
        String email = (String) body.getOrDefault("email", "");
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("email", email);
        res.put("resetTokenSent", true);             // DIFF: reset_token_sent → resetTokenSent
        res.put("expiresInMinutes", 15);              // DIFF: 30 → 15 (shorter expiry in v2)
        res.put("channel", "email");                  // DIFF: extra field
        res.put("message", "Password reset email sent");
        return ResponseEntity.ok(res);
    }
}
