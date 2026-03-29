package com.mockapi.controller.modernized;

import com.mockapi.entity.Payment;
import com.mockapi.entity.User;
import com.mockapi.repository.PaymentRepository;
import com.mockapi.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/modernized/api/payments")
public class ModernizedPaymentController {

    private final PaymentRepository paymentRepo;
    private final UserRepository userRepo;

    public ModernizedPaymentController(PaymentRepository paymentRepo, UserRepository userRepo) {
        this.paymentRepo = paymentRepo;
        this.userRepo = userRepo;
    }

    // TC-PAY01: POST /payments
    // DIFF: user with discount=100 → 500 (same as legacy — but field names differ)
    @PostMapping
    public ResponseEntity<?> processPayment(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") != null ? Long.parseLong(body.get("userId").toString()) : 1L;
        Optional<User> userOpt = userRepo.findById(userId);

        if (userOpt.isPresent() && userOpt.get().getDiscount() == 100.0) {
            // DIFF: different error structure than legacy
            return ResponseEntity.status(500).body(Map.of(
                    "error", "PAYMENT_PROCESSING_ERROR",
                    "message", "Failed to calculate final amount",
                    "details", "Division by zero: discount rate invalid"
            ));
        }

        double amount = body.get("amount") != null ? Double.parseDouble(body.get("amount").toString()) : 0.0;
        double discount = userOpt.map(User::getDiscount).orElse(0.0);
        double finalAmount = amount * (1 - discount / 100);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("paymentId", "PAY-NEW-001");            // DIFF: payment_id → paymentId
        res.put("orderId", body.getOrDefault("orderId", ""));
        res.put("userId", userId);
        res.put("originalAmount", amount);              // DIFF: original_amount → originalAmount
        res.put("discountApplied", discount);           // DIFF: discount_applied → discountApplied
        res.put("finalAmount", Math.round(finalAmount * 100.0) / 100.0);
        res.put("currency", body.getOrDefault("currency", "USD"));
        res.put("status", "processing");                // DIFF: payment_status → status
        res.put("method", body.getOrDefault("method", "credit_card"));
        res.put("transactionId", "TXN-" + System.currentTimeMillis());
        res.put("createdAt", System.currentTimeMillis() / 1000); // DIFF: epoch
        res.put("message", "Payment initiated successfully");
        return ResponseEntity.status(201).body(res);
    }

    // TC-PAY02: GET /payments/{paymentId}
    // DIFF: field renames camelCase
    @GetMapping("/{paymentId}")
    public ResponseEntity<?> getPayment(@PathVariable String paymentId) {
        return paymentRepo.findByPaymentId(paymentId).map(p -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("paymentId", p.getPaymentId());
            res.put("orderId", p.getOrderId());
            res.put("userId", p.getUserId());
            res.put("amount", p.getAmount());
            res.put("currency", p.getCurrency());
            res.put("status", p.getStatus());           // DIFF: payment_status → status
            res.put("method", p.getMethod());           // DIFF: payment_method → method
            if (p.getCardLast4() != null) res.put("cardLast4", p.getCardLast4()); // DIFF: card_last4 → cardLast4
            res.put("createdAt", p.getCreatedAtEpoch()); // DIFF: ISO → epoch
            res.put("transactionId", "TXN-" + p.getPaymentId());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-PAY03: POST /payments/{paymentId}/refund
    // DIFF: refund structure simplified, missing estimated_completion
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<?> refund(@PathVariable String paymentId, @RequestBody Map<String, Object> body) {
        return paymentRepo.findByPaymentId(paymentId).map(p -> {
            if ("failed".equals(p.getStatus())) {
                return ResponseEntity.status(422).body(Map.of(
                        "error", "Cannot refund a failed payment",
                        "paymentId", paymentId,
                        "currentStatus", p.getStatus()
                ));
            }
            double refundAmount = body.get("amount") != null ?
                    Double.parseDouble(body.get("amount").toString()) : p.getAmount();

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("refundId", "REF-" + paymentId);
            res.put("paymentId", paymentId);
            res.put("orderId", p.getOrderId());
            res.put("refundAmount", refundAmount);
            res.put("currency", p.getCurrency());
            res.put("status", "processing");
            res.put("reason", body.getOrDefault("reason", "customer_request"));
            res.put("type", refundAmount < p.getAmount() ? "partial" : "full"); // DIFF: refund_type → type
            res.put("initiatedAt", System.currentTimeMillis() / 1000);          // DIFF: epoch
            // DIFF: missing "estimated_completion" field
            res.put("message", "Refund initiated successfully");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-PAY04: GET /payments
    // DIFF: response key rename + extra pagination info
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
            m.put("paymentId", p.getPaymentId());
            m.put("orderId", p.getOrderId());
            m.put("amount", p.getAmount());
            m.put("currency", p.getCurrency());
            m.put("status", p.getStatus());
            m.put("method", p.getMethod());
            m.put("createdAt", p.getCreatedAtEpoch()); // DIFF: ISO → epoch
            payments.add(m);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("data", payments);           // DIFF: "payments" → "data"
        res.put("total", total);             // DIFF: total_count → total
        res.put("page", page);
        res.put("pageSize", limit);          // DIFF: limit → pageSize
        return ResponseEntity.ok(res);
    }

    // TC-PAY05: GET /payments/{paymentId}/receipt — SAME (pass)
    @GetMapping("/{paymentId}/receipt")
    public ResponseEntity<?> getReceipt(@PathVariable String paymentId) {
        return paymentRepo.findByPaymentId(paymentId).map(p -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("receiptId", "RCP-" + paymentId);
            res.put("paymentId", p.getPaymentId());
            res.put("orderId", p.getOrderId());
            res.put("amount", p.getAmount());
            res.put("currency", p.getCurrency());
            res.put("method", p.getMethod());
            if (p.getCardLast4() != null) res.put("cardLast4", p.getCardLast4());
            res.put("status", p.getStatus());
            res.put("issuedAt", p.getCreatedAtIso());
            res.put("downloadUrl", "/modernized/api/payments/" + paymentId + "/receipt/pdf");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-PAY06: POST /payments/{paymentId}/capture — NOT IMPLEMENTED
    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<?> capture(@PathVariable String paymentId) {
        return ResponseEntity.status(404).body(Map.of(
                "error", "Not implemented in modernized version",
                "code", "NOT_IMPLEMENTED",
                "note", "Modernized payments use auto-capture on creation"
        ));
    }
}
