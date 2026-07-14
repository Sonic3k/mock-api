package com.mockapi.controller.modernized;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fixed scalar-array payload for exercising the assertion DSL's terminal
 * [*] form ($.to[*] any == ...). Everything is deterministic and identical
 * on both sides so comparison mode also passes. Unprotected on purpose.
 */
@RestController
@RequestMapping("/modernized/api/playground")
public class ModernizedPlaygroundController {

    @GetMapping("/emails")
    public ResponseEntity<Map<String, Object>> emails() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("to", List.of("a@x.com", "ops@x.com"));
        resp.put("cc", List.of());
        resp.put("n", List.of(1, 2, 2));
        Map<String, Object> o1 = new LinkedHashMap<>();
        o1.put("currency", "USD"); o1.put("order_id", "ORD-10001");
        Map<String, Object> o2 = new LinkedHashMap<>();
        o2.put("currency", "USD"); o2.put("order_id", "ORD-2");
        resp.put("orders", List.of(o1, o2));
        return ResponseEntity.ok(resp);
    }
}
