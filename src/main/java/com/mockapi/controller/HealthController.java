package com.mockapi.controller;

import com.mockapi.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final UserRepository userRepo;
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final PaymentRepository paymentRepo;

    public HealthController(UserRepository userRepo, OrderRepository orderRepo,
                            ProductRepository productRepo, PaymentRepository paymentRepo) {
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.paymentRepo = paymentRepo;
    }

    @GetMapping("/")
    public ResponseEntity<?> root() {
        return ResponseEntity.ok(Map.of(
                "service", "Mock API for Comparison Tool Testing",
                "version", "1.0.0",
                "status", "UP",
                "docs", "/health"
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", "UP");
        res.put("service", "Mock API - Comparison Tool Testing");
        res.put("version", "1.0.0");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", userRepo.count());
        data.put("orders", orderRepo.count());
        data.put("products", productRepo.count());
        data.put("payments", paymentRepo.count());
        res.put("seededData", data);

        Map<String, Object> endpoints = new LinkedHashMap<>();
        endpoints.put("legacy_base", "/legacy/api");
        endpoints.put("modernized_base", "/modernized/api");
        endpoints.put("domains", new String[]{"users", "auth", "orders", "products", "payments"});
        endpoints.put("total_endpoints", 35);
        res.put("endpoints", endpoints);

        Map<String, Object> testDistribution = new LinkedHashMap<>();
        testDistribution.put("pass", "~50% - identical business logic, field names differ between legacy/modernized");
        testDistribution.put("fail", "~25% - field renames, type changes (ISO→epoch, String→Double), structure changes (flat→nested)");
        testDistribution.put("error", "~10% - 500 errors on specific data (discount=100, suspended users)");
        testDistribution.put("not_implemented", "~15% - endpoints not yet migrated in modernized version");
        res.put("testDistribution", testDistribution);

        return ResponseEntity.ok(res);
    }
}
