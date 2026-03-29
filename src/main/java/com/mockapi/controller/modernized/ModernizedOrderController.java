package com.mockapi.controller.modernized;

import com.mockapi.entity.Order;
import com.mockapi.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/modernized/api/orders")
public class ModernizedOrderController {

    private final OrderRepository orderRepo;

    public ModernizedOrderController(OrderRepository orderRepo) {
        this.orderRepo = orderRepo;
    }

    // TC-O01: GET /orders/{orderId}
    // DIFF: nested shipping object, camelCase, epoch dates
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        return orderRepo.findByOrderId(orderId).map(o -> {
            Map<String, Object> shipping = new LinkedHashMap<>();
            shipping.put("street", o.getShippingStreet());
            shipping.put("city", o.getShippingCity());
            shipping.put("country", o.getShippingCountry());
            shipping.put("method", o.getShippingMethod());

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("orderId", o.getOrderId());              // DIFF: order_id → orderId
            res.put("userId", o.getUserId());
            res.put("status", o.getStatus());                // DIFF: order_status → status
            res.put("totalAmount", o.getTotalAmount());
            res.put("currency", o.getCurrency());
            res.put("itemsSummary", o.getItemsSummary());
            res.put("itemCount", o.getItemCount());
            res.put("shipping", shipping);                   // DIFF: nested vs flat
            res.put("createdAt", o.getCreatedAtEpoch());     // DIFF: ISO → epoch
            if (o.getCancelReason() != null) res.put("cancelReason", o.getCancelReason());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-O02: GET /orders
    // DIFF: pagination key total_records → totalItems, extra meta field
    @GetMapping
    public ResponseEntity<?> listOrders(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status) {

        List<Order> all;
        if (userId != null) all = orderRepo.findByUserId(userId);
        else if (status != null) all = orderRepo.findByStatus(status);
        else all = orderRepo.findAll();

        int total = all.size();
        int start = Math.min((page - 1) * limit, total);
        int end = Math.min(start + limit, total);
        List<Order> paged = all.subList(start, end);

        List<Map<String, Object>> orders = new ArrayList<>();
        for (Order o : paged) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderId", o.getOrderId());
            m.put("status", o.getStatus());
            m.put("totalAmount", o.getTotalAmount());
            m.put("currency", o.getCurrency());
            m.put("itemCount", o.getItemCount());
            m.put("createdAt", o.getCreatedAtEpoch());  // DIFF: ISO → epoch
            orders.add(m);
        }

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("totalItems", total);   // DIFF: total_records → totalItems
        pagination.put("totalPages", (int) Math.ceil((double) total / limit));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("orders", orders);
        res.put("pagination", pagination);
        res.put("meta", Map.of("apiVersion", "2.0", "timestamp", System.currentTimeMillis())); // DIFF: extra meta
        return ResponseEntity.ok(res);
    }

    // TC-O03: PATCH /orders/{orderId}/cancel — NOT IMPLEMENTED
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderId, @RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.status(404).body(Map.of(
                "error", "Not implemented in modernized version",
                "code", "NOT_IMPLEMENTED",
                "note", "Use DELETE /modernized/api/orders/{orderId} instead"
        ));
    }

    // TC-O04: GET /orders/{orderId}/items — SAME (pass)
    @GetMapping("/{orderId}/items")
    public ResponseEntity<?> getOrderItems(@PathVariable String orderId) {
        return orderRepo.findByOrderId(orderId).map(o -> {
            List<Map<String, Object>> items = new ArrayList<>();
            String[] summaryParts = o.getItemsSummary().split(",");
            for (int i = 0; i < summaryParts.length; i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("itemId", "ITEM-" + (i + 1));
                item.put("productName", summaryParts[i].trim());
                item.put("quantity", 1);
                item.put("unitPrice", o.getTotalAmount() / summaryParts.length);
                item.put("subtotal", o.getTotalAmount() / summaryParts.length);
                items.add(item);
            }
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("orderId", o.getOrderId());
            res.put("items", items);
            res.put("totalItems", items.size());
            res.put("totalAmount", o.getTotalAmount());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-O05: POST /orders — create order
    // DIFF: returns 201 with slightly different body structure
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("orderId", "ORD-99999");
        res.put("userId", body.getOrDefault("userId", 1));
        res.put("status", "pending");
        res.put("totalAmount", body.getOrDefault("totalAmount", 0.0));
        res.put("currency", body.getOrDefault("currency", "USD"));
        res.put("createdAt", System.currentTimeMillis() / 1000); // DIFF: epoch
        res.put("message", "Order created successfully");
        return ResponseEntity.status(201).body(res);
    }

    // TC-O06: GET /orders/{orderId}/tracking — SAME (pass)
    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<?> getTracking(@PathVariable String orderId) {
        return orderRepo.findByOrderId(orderId).map(o -> {
            List<Map<String, Object>> events = new ArrayList<>();
            Map<String, Object> e1 = new LinkedHashMap<>();
            e1.put("event", "order_placed"); e1.put("timestamp", o.getCreatedAtIso()); e1.put("location", "Warehouse");
            events.add(e1);
            if (List.of("shipped", "delivered").contains(o.getStatus())) {
                Map<String, Object> e2 = new LinkedHashMap<>();
                e2.put("event", "shipped"); e2.put("timestamp", "2024-01-16T08:00:00Z"); e2.put("location", "Distribution Center");
                events.add(e2);
            }
            if ("delivered".equals(o.getStatus())) {
                Map<String, Object> e3 = new LinkedHashMap<>();
                e3.put("event", "delivered"); e3.put("timestamp", "2024-01-18T14:30:00Z"); e3.put("location", o.getShippingCity());
                events.add(e3);
            }
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("orderId", o.getOrderId());
            res.put("currentStatus", o.getStatus());
            res.put("trackingEvents", events);
            res.put("carrier", "FedEx");
            res.put("trackingNumber", "FX" + o.getOrderId().replace("ORD-", ""));
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-O07: GET /orders/summary
    // DIFF: field names camelCase + extra breakdown field
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary() {
        List<Order> all = orderRepo.findAll();
        long confirmed = all.stream().filter(o -> "confirmed".equals(o.getStatus())).count();
        long shipped = all.stream().filter(o -> "shipped".equals(o.getStatus())).count();
        long delivered = all.stream().filter(o -> "delivered".equals(o.getStatus())).count();
        long cancelled = all.stream().filter(o -> "cancelled".equals(o.getStatus())).count();
        double totalRevenue = all.stream().filter(o -> !"cancelled".equals(o.getStatus()))
                .mapToDouble(Order::getTotalAmount).sum();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("totalOrders", all.size());       // DIFF: total_orders → totalOrders
        res.put("confirmed", confirmed);
        res.put("shipped", shipped);
        res.put("delivered", delivered);
        res.put("cancelled", cancelled);
        res.put("totalRevenue", Math.round(totalRevenue * 100.0) / 100.0);
        res.put("currency", "USD");
        res.put("generatedAt", System.currentTimeMillis() / 1000); // DIFF: ISO → epoch
        res.put("breakdown", Map.of(                               // DIFF: extra field
                "completionRate", delivered > 0 ? Math.round((double) delivered / all.size() * 100) : 0,
                "cancellationRate", cancelled > 0 ? Math.round((double) cancelled / all.size() * 100) : 0
        ));
        return ResponseEntity.ok(res);
    }
}
