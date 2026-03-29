package com.mockapi.controller.legacy;

import com.mockapi.entity.User;
import com.mockapi.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/legacy/api/users")
public class LegacyUserController {

    private final UserRepository userRepo;

    public LegacyUserController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    // TC-U01: GET /users/{id} — returns flat address + snake_case + ISO date
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        return userRepo.findById(id).map(u -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("user_id", u.getId());
            res.put("full_name", u.getName());
            res.put("email_address", u.getEmail());
            res.put("role", u.getRole());
            res.put("account_status", u.getStatus());
            res.put("department", u.getDepartment());
            res.put("phone_number", u.getPhone());
            // Flat address (legacy style)
            res.put("street", u.getStreet());
            res.put("city", u.getCity());
            res.put("country", u.getCountry());
            res.put("discount_percent", u.getDiscount());
            res.put("created_at", u.getCreatedAtIso());     // ISO string
            res.put("updated_at", u.getUpdatedAtEpoch());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-U02: POST /users — create user, return created_at as ISO string
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("user_id", 999L);
        res.put("full_name", body.getOrDefault("name", "New User"));
        res.put("email_address", body.getOrDefault("email", ""));
        res.put("role", body.getOrDefault("role", "user"));
        res.put("account_status", "active");
        res.put("created_at", "2024-01-15T10:30:15Z");   // ISO string (legacy)
        res.put("message", "User created successfully");
        return ResponseEntity.status(201).body(res);
    }

    // TC-U03: PUT /users/{id}/email — update email
    @PutMapping("/{id}/email")
    public ResponseEntity<?> updateEmail(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return userRepo.findById(id).map(u -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("user_id", u.getId());
            res.put("full_name", u.getName());
            res.put("email_address", body.getOrDefault("email", u.getEmail()));
            res.put("updated_at", u.getUpdatedAtEpoch());
            res.put("message", "Email updated successfully");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-U04: DELETE /users/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepo.existsById(id)) return ResponseEntity.notFound().build();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("user_id", id);
        res.put("deleted", true);
        res.put("deleted_at", "2024-01-15T10:30:00Z");
        res.put("message", "User account deleted successfully");
        return ResponseEntity.ok(res);
    }

    // TC-U05: GET /users — paginated list, uses total_count (legacy naming)
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
            m.put("user_id", u.getId());
            m.put("full_name", u.getName());
            m.put("email_address", u.getEmail());
            m.put("account_status", u.getStatus());
            m.put("role", u.getRole());
            users.add(m);
        }

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("current_page", page);
        pagination.put("per_page", limit);
        pagination.put("total_count", total);       // legacy key
        pagination.put("total_pages", (int) Math.ceil((double) total / limit));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("users", users);
        res.put("pagination", pagination);
        return ResponseEntity.ok(res);
    }

    // TC-U06: GET /users/{id}/profile — extended profile
    @GetMapping("/{id}/profile")
    public ResponseEntity<?> getProfile(@PathVariable Long id) {
        return userRepo.findById(id).map(u -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("user_id", u.getId());
            res.put("full_name", u.getName());
            res.put("email_address", u.getEmail());
            res.put("role", u.getRole());
            res.put("department", u.getDepartment());
            res.put("phone_number", u.getPhone());
            res.put("street", u.getStreet());
            res.put("city", u.getCity());
            res.put("country", u.getCountry());
            res.put("discount_percent", u.getDiscount());
            res.put("account_status", u.getStatus());
            res.put("created_at", u.getCreatedAtIso());
            res.put("updated_at", u.getUpdatedAtEpoch());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-U07: PATCH /users/{id}/status — update status
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return userRepo.findById(id).map(u -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("user_id", u.getId());
            res.put("full_name", u.getName());
            res.put("old_status", u.getStatus());
            res.put("new_status", body.getOrDefault("status", u.getStatus()));
            res.put("updated_at", "2024-01-15T10:30:00Z");
            res.put("message", "Status updated");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-U08: GET /users/search — search by name or email
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam(required = false) String q) {
        List<User> all = userRepo.findAll();
        List<Map<String, Object>> results = new ArrayList<>();
        for (User u : all) {
            if (q == null || u.getName().toLowerCase().contains(q.toLowerCase())
                    || u.getEmail().toLowerCase().contains(q.toLowerCase())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("user_id", u.getId());
                m.put("full_name", u.getName());
                m.put("email_address", u.getEmail());
                m.put("account_status", u.getStatus());
                results.add(m);
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("results", results);
        res.put("total_count", results.size());
        res.put("query", q);
        return ResponseEntity.ok(res);
    }
}
