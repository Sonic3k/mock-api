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
    public ModernizedAuthController(UserRepository userRepo) { this.userRepo = userRepo; }

    // TC-A01: FAIL — token → accessToken, token_type → tokenType, issued_at ISO → epoch
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body) {
        String email = (String) body.getOrDefault("email", "");
        return userRepo.findByEmail(email).map(u -> {
            if ("suspended".equals(u.getStatus())) {
                // DIFF: 403 in legacy, 500 here (bug in modernized)
                return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error", "message", "Unexpected error during authentication"));
            }
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("accessToken", "modernized.jwt." + u.getId());   // DIFF: token → accessToken
            res.put("tokenType", "Bearer");                           // DIFF: token_type → tokenType
            res.put("expiresIn", 3600);                               // DIFF: expires_in → expiresIn
            res.put("refreshToken", "modernized.refresh." + u.getId()); // DIFF: refresh_token → refreshToken
            res.put("user_id", u.getId());
            res.put("user_name", u.getName());
            res.put("issued_at", System.currentTimeMillis() / 1000);  // DIFF: ISO → epoch
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid credentials", "code", "INVALID_CREDENTIALS")));
    }

    // TC-A03: PASS — both return 401 with same body
    // (handled above — unknown email returns same 401 structure)

    // TC-A04: PASS — same fields as legacy
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

    // TC-A05: PASS — same fields as legacy
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, Object> body) {
        String refreshToken = (String) body.getOrDefault("refresh_token", "");
        if (refreshToken.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "refresh_token is required"));
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("token", "modernized.new.jwt." + System.currentTimeMillis());
        res.put("token_type", "Bearer");
        res.put("expires_in", 3600);
        res.put("refresh_token", "modernized.new.refresh." + System.currentTimeMillis());
        res.put("issued_at", "2024-01-15T10:30:00Z");
        return ResponseEntity.ok(res);
    }

    // TC-A06: NOT IMPLEMENTED
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.status(404).body(Map.of("error", "Not implemented in modernized version", "code", "NOT_IMPLEMENTED"));
    }

    // TC-A07: FAIL — expires_in_minutes 30 → 15
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, Object> body) {
        String email = (String) body.getOrDefault("email", "");
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("email", email);
        res.put("reset_token_sent", true);
        res.put("expires_in_minutes", 15);   // DIFF: 30 → 15
        res.put("message", "Password reset email sent");
        return ResponseEntity.ok(res);
    }
}
