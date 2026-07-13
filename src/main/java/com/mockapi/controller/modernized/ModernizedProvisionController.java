package com.mockapi.controller.modernized;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Purpose-built for verifying the comparison tool's variable extraction:
 * deterministic audit fields (createdTs / createdBy are constants) so
 * assertion expectations can be static, and an /echo endpoint that reflects
 * query + header + body inputs to prove {{var}} substitution end to end.
 * Unprotected on purpose — auth is not what these tests exercise.
 */
@RestController
@RequestMapping("/modernized/api/provision")
public class ModernizedProvisionController {

    private static final Map<Long, Map<String, Object>> STORE = new ConcurrentHashMap<>();
    private static final AtomicLong SEQ = new AtomicLong(9000);
    static final String FIXED_TS = "2026-01-01T00:00:00.000";
    static final String FIXED_BY = "mock-user";

    /** Deterministic create: id = requestedId (or auto), fixed audit fields. */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody(required = false) Map<String, Object> body) {
        long id = body != null && body.get("requestedId") != null
                ? Long.parseLong(String.valueOf(body.get("requestedId")))
                : SEQ.incrementAndGet();
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("id", id);
        rec.put("createdTs", FIXED_TS);
        rec.put("createdBy", FIXED_BY);
        rec.put("status", "A");
        STORE.put(id, rec);
        return ResponseEntity.status(201).body(rec);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        Map<String, Object> rec = STORE.get(id);
        if (rec == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "not found");
            err.put("id", id);
            return ResponseEntity.status(404).body(err);
        }
        return ResponseEntity.ok(rec);
    }

    /** Echoes query + header + body back — proves substitution in all three. */
    @PostMapping("/echo")
    public ResponseEntity<?> echo(@RequestParam(required = false) String code,
                                  @RequestHeader(value = "X-By", required = false) String xBy,
                                  @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("qCode", code);
        res.put("hBy", xBy);
        res.put("bId", body != null ? body.get("id") : null);
        res.put("bTs", body != null ? body.get("ts") : null);
        return ResponseEntity.ok(res);
    }
}
