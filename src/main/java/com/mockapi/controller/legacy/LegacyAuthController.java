package com.mockapi.controller.legacy;

import com.mockapi.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/legacy/api/auth")
public class LegacyAuthController {

    private final UserRepository userRepo;

    public LegacyAuthController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    // TC-A01: POST /auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body) {
        String email = (String) body.getOrDefault("email", "");
        return userRepo.findByEmail(email).map(u -> {
            if ("suspended".equals(u.getStatus())) {
                return ResponseEntity.status(403).body(Map.of("error", "Account suspended", "code", "ACCOUNT_SUSPENDED"));
            }
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("token", "legacy.jwt.token.for." + u.getId());
            res.put("token_type", "Bearer");
            res.put("expires_in", 3600);
            res.put("refresh_token", "legacy.refresh.token." + u.getId());
            res.put("user_id", u.getId());
            res.put("user_name", u.getName());
            res.put("issued_at", "2024-01-15T10:30:00Z");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid credentials", "code", "INVALID_CREDENTIALS")));
    }

    // TC-A02: POST /auth/register — form params
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody(required = false) Map<String, Object> body,
                                      @RequestParam(required = false) String username,
                                      @RequestParam(required = false) String email) {
        Map<String, Object> res = new LinkedHashMap<>();
        String resolvedEmail = email != null ? email :
                (body != null ? (String) body.getOrDefault("email", "new@example.com") : "new@example.com");
        res.put("user_id", 1001L);
        res.put("username", username != null ? username : "newuser");
        res.put("email_address", resolvedEmail);
        res.put("account_status", "pending_verification");
        res.put("created_at", "2024-01-15T10:30:00Z");
        res.put("verification_required", true);
        res.put("message", "Registration successful. Please verify your email.");
        return ResponseEntity.status(201).body(res);
    }

    // TC-A03: POST /auth/refresh
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, Object> body) {
        String refreshToken = (String) body.getOrDefault("refresh_token", "");
        if (refreshToken.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "refresh_token is required"));
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("token", "legacy.new.jwt.token." + System.currentTimeMillis());
        res.put("token_type", "Bearer");
        res.put("expires_in", 3600);
        res.put("refresh_token", "legacy.new.refresh." + System.currentTimeMillis());
        res.put("issued_at", "2024-01-15T10:30:00Z");
        return ResponseEntity.ok(res);
    }

    // TC-A04: POST /auth/logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "Logged out successfully");
        res.put("logged_out_at", "2024-01-15T10:30:00Z");
        return ResponseEntity.ok(res);
    }

    // TC-A05: POST /auth/forgot-password
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, Object> body) {
        String email = (String) body.getOrDefault("email", "");
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("email", email);
        res.put("reset_token_sent", true);
        res.put("expires_in_minutes", 30);
        res.put("message", "Password reset email sent");
        return ResponseEntity.ok(res);
    }
}
