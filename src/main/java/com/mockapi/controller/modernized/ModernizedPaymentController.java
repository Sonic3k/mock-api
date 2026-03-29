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

    // TC-PAY01/PAY02: PASS — same fields as legacy
    @PostMapping
    public ResponseEntity<?> processPayment(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") != null ? Long.parseLong(body.get("userId").toString()) : 1L;
        Optional<User> userOpt = userRepo.findById(userId);

        // TC-PAY03: userId=20 (discount=100) → 500 in modernized (different error than legacy 422)
        if (userOpt.isPresent() && userOpt.get().getDiscount() == 100.0) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "PAYMENT_PROCESSING_ERROR",
                    "message", "Failed to calculate final amount"
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

    // TC-PAY04/PAY05: PASS — same fields as legacy
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

    // TC-PAY06: FAIL — missing estimated_completion field
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
            // DIFF: missing "estimated_completion" field from legacy
            res.put("message", "Refund initiated successfully");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-PAY08/PAY09: PASS — same fields as legacy
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

    // TC-PAY10: PASS — same fields as legacy
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
            res.put("download_url", "/modernized/api/payments/" + paymentId + "/receipt/pdf");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-PAY11: NOT IMPLEMENTED
    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<?> capture(@PathVariable String paymentId) {
        return ResponseEntity.status(404).body(Map.of("error", "Not implemented in modernized version", "code", "NOT_IMPLEMENTED"));
    }
}
