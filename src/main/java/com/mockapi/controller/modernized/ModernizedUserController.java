package com.mockapi.controller.modernized;

import com.mockapi.entity.User;
import com.mockapi.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/modernized/api/users")
public class ModernizedUserController {

    private final UserRepository userRepo;

    public ModernizedUserController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    // TC-U01: GET /users/{id}
    // DIFF: field rename snake_case → camelCase, nested address object, ISO date vs epoch
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        return userRepo.findById(id).map(u -> {
            Map<String, Object> address = new LinkedHashMap<>();
            address.put("street", u.getStreet());
            address.put("city", u.getCity());
            address.put("country", u.getCountry());

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("userId", u.getId());               // DIFF: user_id → userId
            res.put("fullName", u.getName());            // DIFF: full_name → fullName
            res.put("email", u.getEmail());              // DIFF: email_address → email
            res.put("role", u.getRole());
            res.put("status", u.getStatus());            // DIFF: account_status → status
            res.put("department", u.getDepartment());
            res.put("phone", u.getPhone());              // DIFF: phone_number → phone
            res.put("address", address);                 // DIFF: nested vs flat
            res.put("discountPercent", u.getDiscount()); // DIFF: discount_percent → discountPercent
            res.put("createdAt", u.getCreatedAtEpoch()); // DIFF: ISO string → epoch number
            res.put("updatedAt", u.getUpdatedAtEpoch());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-U02: POST /users
    // DIFF: createdAt as epoch (not ISO string)
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("userId", 999L);
        res.put("fullName", body.getOrDefault("name", "New User"));
        res.put("email", body.getOrDefault("email", ""));
        res.put("role", body.getOrDefault("role", "user"));
        res.put("status", "active");
        res.put("createdAt", 1705316400L);    // DIFF: epoch instead of ISO string
        res.put("message", "User created successfully");
        return ResponseEntity.status(201).body(res);
    }

    // TC-U03: PUT /users/{id}/email — SAME result (pass)
    @PutMapping("/{id}/email")
    public ResponseEntity<?> updateEmail(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return userRepo.findById(id).map(u -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("userId", u.getId());
            res.put("fullName", u.getName());
            res.put("email", body.getOrDefault("email", u.getEmail()));
            res.put("updatedAt", u.getUpdatedAtEpoch());
            res.put("message", "Email updated successfully");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-U04: DELETE /users/{id} — SAME result (pass)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepo.existsById(id)) return ResponseEntity.notFound().build();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("userId", id);
        res.put("deleted", true);
        res.put("deletedAt", "2024-01-15T10:30:00Z");
        res.put("message", "User account deleted successfully");
        return ResponseEntity.ok(res);
    }

    // TC-U05: GET /users
    // DIFF: pagination key total_count → total
    @GetMapping
    public ResponseEntity<?> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status) {

        List<User> all = status != null ? userRepo.findByStatus(status) : userRepo.findAll();
        int total = all.size();
        int start = Math.min((page - 1) * limit, total);
        int end = Math.min(start + limit, total);
        List<User> paged = all.subList(start, end);

        List<Map<String, Object>> users = new ArrayList<>();
        for (User u : paged) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", u.getId());
            m.put("fullName", u.getName());
            m.put("email", u.getEmail());
            m.put("status", u.getStatus());
            m.put("role", u.getRole());
            users.add(m);
        }

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("currentPage", page);
        pagination.put("perPage", limit);
        pagination.put("total", total);        // DIFF: total_count → total
        pagination.put("totalPages", (int) Math.ceil((double) total / limit));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("users", users);
        res.put("pagination", pagination);
        res.put("meta", Map.of("version", "2.0", "cached", false)); // DIFF: extra field
        return ResponseEntity.ok(res);
    }

    // TC-U06: GET /users/{id}/profile — SAME (pass)
    @GetMapping("/{id}/profile")
    public ResponseEntity<?> getProfile(@PathVariable Long id) {
        return userRepo.findById(id).map(u -> {
            Map<String, Object> address = new LinkedHashMap<>();
            address.put("street", u.getStreet());
            address.put("city", u.getCity());
            address.put("country", u.getCountry());

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("userId", u.getId());
            res.put("fullName", u.getName());
            res.put("email", u.getEmail());
            res.put("role", u.getRole());
            res.put("department", u.getDepartment());
            res.put("phone", u.getPhone());
            res.put("address", address);
            res.put("discountPercent", u.getDiscount());
            res.put("status", u.getStatus());
            res.put("createdAt", u.getCreatedAtEpoch());
            res.put("updatedAt", u.getUpdatedAtEpoch());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-U07: PATCH /users/{id}/status — NOT IMPLEMENTED
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(404).body(Map.of(
                "error", "Not implemented in modernized version",
                "code", "NOT_IMPLEMENTED",
                "endpoint", "PATCH /modernized/api/users/{id}/status"
        ));
    }

    // TC-U08: GET /users/search — SAME result (pass)
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam(required = false) String q) {
        List<User> all = userRepo.findAll();
        List<Map<String, Object>> results = new ArrayList<>();
        for (User u : all) {
            if (q == null || u.getName().toLowerCase().contains(q.toLowerCase())
                    || u.getEmail().toLowerCase().contains(q.toLowerCase())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("userId", u.getId());
                m.put("fullName", u.getName());
                m.put("email", u.getEmail());
                m.put("status", u.getStatus());
                results.add(m);
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("results", results);
        res.put("total", results.size());
        res.put("query", q);
        return ResponseEntity.ok(res);
    }
}
