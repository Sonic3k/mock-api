package com.mockapi.controller.legacy;

import com.mockapi.config.TokenValidator;
import com.mockapi.entity.Payment;
import com.mockapi.entity.User;
import com.mockapi.repository.PaymentRepository;
import com.mockapi.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/legacy/api/payments")
public class LegacyPaymentController {

    private final PaymentRepository paymentRepo;
    private final UserRepository userRepo;

    private final TokenValidator tokenValidator;

    public LegacyPaymentController(PaymentRepository paymentRepo, UserRepository userRepo,
                                   TokenValidator tokenValidator) {
        this.paymentRepo = paymentRepo;
        this.userRepo = userRepo;
        this.tokenValidator = tokenValidator;
    }

    // TC-PAY01: POST /payments — PROTECTED
    @PostMapping
    public ResponseEntity<?> processPayment(@RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        if (!tokenValidator.isValid(auth)) return TokenValidator.unauthorized();
        Long userId = body.get("userId") != null ? Long.parseLong(body.get("userId").toString()) : 1L;
        Optional<User> userOpt = userRepo.findById(userId);

        // Special case: user with discount=100 triggers error (intentional bug scenario)
        if (userOpt.isPresent() && userOpt.get().getDiscount() == 100.0) {
            return ResponseEntity.status(422).body(Map.of(
                    "error", "Cannot process payment: invalid discount configuration",
                    "code", "DISCOUNT_ERROR",
                    "user_id", userId
            ));
        }

        double amount = body.get("amount") != null ? Double.parseDouble(body.get("amount").toString()) : 0.0;
        double discount = userOpt.map(User::getDiscount).orElse(0.0);
        double finalAmount = amount * (1 - discount / 100);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("payment_id", "PAY-NEW-001");
        res.put("order_id", body.getOrDefault("orderId", ""));
        res.put("user_id", userId);
        res.put("original_amount", amount);
        res.put("discount_applied", discount);
        res.put("final_amount", Math.round(finalAmount * 100.0) / 100.0);
        res.put("currency", body.getOrDefault("currency", "USD"));
        res.put("payment_status", "processing");
        res.put("payment_method", body.getOrDefault("method", "credit_card"));
        res.put("transaction_id", "TXN-" + System.currentTimeMillis());
        res.put("created_at", "2024-01-15T10:30:00Z");
        res.put("message", "Payment initiated successfully");
        return ResponseEntity.status(201).body(res);
    }

    // TC-PAY02: GET /payments/{paymentId}
    @GetMapping("/{paymentId}")
    public ResponseEntity<?> getPayment(@PathVariable String paymentId) {
        return paymentRepo.findByPaymentId(paymentId).map(p -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("payment_id", p.getPaymentId());
            res.put("order_id", p.getOrderId());
            res.put("user_id", p.getUserId());
            res.put("amount", p.getAmount());
            res.put("currency", p.getCurrency());
            res.put("payment_status", p.getStatus());
            res.put("payment_method", p.getMethod());
            if (p.getCardLast4() != null) res.put("card_last4", p.getCardLast4());
            res.put("created_at", p.getCreatedAtIso());
            res.put("transaction_id", "TXN-" + p.getPaymentId());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-PAY03: POST /payments/{paymentId}/refund
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<?> refund(@PathVariable String paymentId, @RequestBody Map<String, Object> body) {
        return paymentRepo.findByPaymentId(paymentId).map(p -> {
            if ("failed".equals(p.getStatus())) {
                return ResponseEntity.status(422).body(Map.of(
                        "error", "Cannot refund a failed payment",
                        "payment_id", paymentId,
                        "current_status", p.getStatus()
                ));
            }
            double refundAmount = body.get("amount") != null ?
                    Double.parseDouble(body.get("amount").toString()) : p.getAmount();

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("refund_id", "REF-" + paymentId);
            res.put("payment_id", paymentId);
            res.put("order_id", p.getOrderId());
            res.put("refund_amount", refundAmount);
            res.put("currency", p.getCurrency());
            res.put("refund_status", "processing");
            res.put("reason", body.getOrDefault("reason", "customer_request"));
            res.put("refund_type", refundAmount < p.getAmount() ? "partial" : "full");
            res.put("initiated_at", "2024-01-15T10:30:00Z");
            res.put("estimated_completion", "3-5 business days");
            res.put("message", "Refund initiated successfully");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-PAY04: GET /payments — list payments for user
    @GetMapping
    public ResponseEntity<?> listPayments(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        List<Payment> all = userId != null ? paymentRepo.findByUserId(userId) : paymentRepo.findAll();
        int total = all.size();
        int start = Math.min((page - 1) * limit, total);
        int end = Math.min(start + limit, total);
        List<Payment> paged = all.subList(start, end);

        List<Map<String, Object>> payments = new ArrayList<>();
        for (Payment p : paged) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("payment_id", p.getPaymentId());
            m.put("order_id", p.getOrderId());
            m.put("amount", p.getAmount());
            m.put("currency", p.getCurrency());
            m.put("payment_status", p.getStatus());
            m.put("payment_method", p.getMethod());
            m.put("created_at", p.getCreatedAtIso());
            payments.add(m);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("payments", payments);
        res.put("total_count", total);
        res.put("page", page);
        res.put("limit", limit);
        return ResponseEntity.ok(res);
    }

    // TC-PAY05: GET /payments/{paymentId}/receipt
    @GetMapping("/{paymentId}/receipt")
    public ResponseEntity<?> getReceipt(@PathVariable String paymentId) {
        return paymentRepo.findByPaymentId(paymentId).map(p -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("receipt_id", "RCP-" + paymentId);
            res.put("payment_id", p.getPaymentId());
            res.put("order_id", p.getOrderId());
            res.put("amount", p.getAmount());
            res.put("currency", p.getCurrency());
            res.put("payment_method", p.getMethod());
            if (p.getCardLast4() != null) res.put("card_last4", p.getCardLast4());
            res.put("status", p.getStatus());
            res.put("issued_at", p.getCreatedAtIso());
            res.put("download_url", "/legacy/api/payments/" + paymentId + "/receipt/pdf");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-PAY06: POST /payments/{paymentId}/capture
    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<?> capture(@PathVariable String paymentId) {
        return paymentRepo.findByPaymentId(paymentId).map(p -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("payment_id", p.getPaymentId());
            res.put("order_id", p.getOrderId());
            res.put("amount", p.getAmount());
            res.put("currency", p.getCurrency());
            res.put("payment_status", "completed");
            res.put("captured_at", "2024-01-15T10:30:00Z");
            res.put("message", "Payment captured successfully");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }
}
