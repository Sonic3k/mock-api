package com.mockapi.controller.modernized;

import com.mockapi.config.TokenValidator;
import com.mockapi.entity.User;
import com.mockapi.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/modernized/api/users")
public class ModernizedUserController {

    private final UserRepository userRepo;
    private final TokenValidator tokenValidator;

    public ModernizedUserController(UserRepository userRepo, TokenValidator tokenValidator) {
        this.userRepo = userRepo;
        this.tokenValidator = tokenValidator;
    }

    // TC-U01/U02: FAIL — flat address → nested, discount_percent → discountPercent
    // PROTECTED: requires Bearer token
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        if (!tokenValidator.isValid(auth)) return TokenValidator.unauthorized();
        return userRepo.findById(id).map(u -> {
            Map<String, Object> address = new LinkedHashMap<>();
            address.put("street", u.getStreet());
            address.put("city", u.getCity());
            address.put("country", u.getCountry());

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("user_id", u.getId());
            res.put("full_name", u.getName());
            res.put("email_address", u.getEmail());
            res.put("role", u.getRole());
            res.put("account_status", u.getStatus());
            res.put("department", u.getDepartment());
            res.put("phone_number", u.getPhone());
            res.put("address", address);                   // DIFF: nested vs flat street/city/country
            res.put("discountPercent", u.getDiscount());   // DIFF: discount_percent → discountPercent
            res.put("created_at", u.getCreatedAtIso());
            res.put("updated_at", u.getUpdatedAtEpoch());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // POST /users — actually persist to DB, return real ID
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> body) {
        User u = new User();
        u.setName((String) body.getOrDefault("name", "New User"));
        u.setEmail((String) body.getOrDefault("email", ""));
        u.setRole((String) body.getOrDefault("role", "user"));
        u.setStatus("active");
        u.setDepartment("General");
        u.setPhone("");
        u.setStreet("");
        u.setCity("");
        u.setCountry("US");
        u.setDiscount(0.0);
        u.setCreatedAtIso("2024-01-15T10:30:15Z");
        u.setCreatedAtEpoch(1705316400L);
        u.setUpdatedAtEpoch(1705316400L);
        u = userRepo.save(u);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("user_id", u.getId());
        res.put("full_name", u.getName());
        res.put("email_address", u.getEmail());
        res.put("role", u.getRole());
        res.put("account_status", "active");
        res.put("created_at", 1705316400L);    // DIFF: epoch instead of ISO string
        res.put("message", "User created successfully");
        return ResponseEntity.status(201).body(res);
    }

    // TC-U04: PASS
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

    // DELETE /users/{id} — actually delete from DB
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepo.existsById(id)) return ResponseEntity.notFound().build();
        userRepo.deleteById(id);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("user_id", id);
        res.put("deleted", true);
        res.put("deleted_at", "2024-01-15T10:30:00Z");
        res.put("message", "User account deleted successfully");
        return ResponseEntity.ok(res);
    }

    // TC-U06/U07: FAIL — total_count → total
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
        pagination.put("total", total);             // DIFF: total_count → total
        pagination.put("total_pages", (int) Math.ceil((double) total / limit));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("users", users);
        res.put("pagination", pagination);
        return ResponseEntity.ok(res);
    }

    // TC-U08: FAIL — nested address
    @GetMapping("/{id}/profile")
    public ResponseEntity<?> getProfile(@PathVariable Long id) {
        return userRepo.findById(id).map(u -> {
            Map<String, Object> address = new LinkedHashMap<>();
            address.put("street", u.getStreet());
            address.put("city", u.getCity());
            address.put("country", u.getCountry());

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("user_id", u.getId());
            res.put("full_name", u.getName());
            res.put("email_address", u.getEmail());
            res.put("role", u.getRole());
            res.put("department", u.getDepartment());
            res.put("phone_number", u.getPhone());
            res.put("address", address);
            res.put("discountPercent", u.getDiscount());
            res.put("account_status", u.getStatus());
            res.put("created_at", u.getCreatedAtIso());
            res.put("updated_at", u.getUpdatedAtEpoch());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-U09: NOT IMPLEMENTED
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(404).body(Map.of("error", "Not implemented in modernized version", "code", "NOT_IMPLEMENTED"));
    }

    // TC-U10: PASS
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
