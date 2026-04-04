package com.mockapi.controller.legacy;

import com.mockapi.config.TokenValidator;
import com.mockapi.entity.Order;
import com.mockapi.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/legacy/api/orders")
public class LegacyOrderController {

    private final OrderRepository orderRepo;
    private final TokenValidator tokenValidator;

    public LegacyOrderController(OrderRepository orderRepo, TokenValidator tokenValidator) {
        this.orderRepo = orderRepo;
        this.tokenValidator = tokenValidator;
    }

    // TC-O01: GET /orders/{orderId} — PROTECTED
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        if (!tokenValidator.isValid(auth)) return TokenValidator.unauthorized();
        return orderRepo.findByOrderId(orderId).map(o -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("order_id", o.getOrderId());
            res.put("user_id", o.getUserId());
            res.put("order_status", o.getStatus());
            res.put("total_amount", o.getTotalAmount());
            res.put("currency", o.getCurrency());
            res.put("items_summary", o.getItemsSummary());
            res.put("item_count", o.getItemCount());
            // Flat shipping (legacy)
            res.put("shipping_street", o.getShippingStreet());
            res.put("shipping_city", o.getShippingCity());
            res.put("shipping_country", o.getShippingCountry());
            res.put("shipping_method", o.getShippingMethod());
            res.put("created_at", o.getCreatedAtIso());
            if (o.getCancelReason() != null) res.put("cancel_reason", o.getCancelReason());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-O02: GET /orders — paginated list
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
            m.put("order_id", o.getOrderId());
            m.put("order_status", o.getStatus());
            m.put("total_amount", o.getTotalAmount());
            m.put("currency", o.getCurrency());
            m.put("item_count", o.getItemCount());
            m.put("created_at", o.getCreatedAtIso());
            orders.add(m);
        }

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("total_records", total);  // legacy: total_records
        pagination.put("total_pages", (int) Math.ceil((double) total / limit));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("orders", orders);
        res.put("pagination", pagination);
        return ResponseEntity.ok(res);
    }

    // TC-O03: PATCH /orders/{orderId}/cancel
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderId, @RequestBody Map<String, Object> body) {
        return orderRepo.findByOrderId(orderId).map(o -> {
            if ("delivered".equals(o.getStatus())) {
                return ResponseEntity.status(422).body(Map.of(
                        "error", "Cannot cancel a delivered order",
                        "order_id", orderId,
                        "current_status", o.getStatus()
                ));
            }
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("order_id", o.getOrderId());
            res.put("old_status", o.getStatus());
            res.put("new_status", "cancelled");
            res.put("cancel_reason", body.getOrDefault("reason", "customer_request"));
            res.put("cancelled_at", "2024-01-15T10:30:00Z");
            res.put("message", "Order cancelled successfully");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-O04: GET /orders/{orderId}/items
    @GetMapping("/{orderId}/items")
    public ResponseEntity<?> getOrderItems(@PathVariable String orderId) {
        return orderRepo.findByOrderId(orderId).map(o -> {
            List<Map<String, Object>> items = new ArrayList<>();
            String[] summaryParts = o.getItemsSummary().split(",");
            for (int i = 0; i < summaryParts.length; i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("item_id", "ITEM-" + (i + 1));
                item.put("product_name", summaryParts[i].trim());
                item.put("quantity", 1);
                item.put("unit_price", o.getTotalAmount() / summaryParts.length);
                item.put("subtotal", o.getTotalAmount() / summaryParts.length);
                items.add(item);
            }
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("order_id", o.getOrderId());
            res.put("items", items);
            res.put("total_items", items.size());
            res.put("total_amount", o.getTotalAmount());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // POST /orders — actually persist to DB, return real orderId
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body) {
        Order o = new Order();
        o.setUserId(body.containsKey("userId") ? ((Number) body.get("userId")).longValue() : 1L);
        o.setStatus("pending");
        o.setTotalAmount(body.containsKey("totalAmount") ? ((Number) body.get("totalAmount")).doubleValue() : 0.0);
        o.setCurrency((String) body.getOrDefault("currency", "USD"));
        o.setItemsSummary("New Item");
        o.setItemCount(1);
        o.setShippingStreet(""); o.setShippingCity(""); o.setShippingCountry("US"); o.setShippingMethod("standard");
        o.setCreatedAtIso("2024-01-15T10:30:00Z");
        o.setCreatedAtEpoch(1705316400L);
        o = orderRepo.save(o);
        o.setOrderId("ORD-" + (20000 + o.getId()));
        o = orderRepo.save(o);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("order_id", o.getOrderId());
        res.put("user_id", o.getUserId());
        res.put("order_status", "pending");
        res.put("total_amount", o.getTotalAmount());
        res.put("currency", o.getCurrency());
        res.put("created_at", "2024-01-15T10:30:00Z");
        res.put("message", "Order created successfully");
        return ResponseEntity.status(201).body(res);
    }

    // DELETE /orders/{orderId}
    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> deleteOrder(@PathVariable String orderId) {
        return orderRepo.findByOrderId(orderId).map(o -> {
            orderRepo.delete(o);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("order_id", orderId);
            res.put("deleted", true);
            res.put("message", "Order deleted successfully");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-O06: GET /orders/{orderId}/tracking
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
            res.put("order_id", o.getOrderId());
            res.put("current_status", o.getStatus());
            res.put("tracking_events", events);
            res.put("carrier", "FedEx");
            res.put("tracking_number", "FX" + o.getOrderId().replace("ORD-", ""));
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-O07: GET /orders/summary — stats summary
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
        res.put("total_orders", all.size());
        res.put("confirmed", confirmed);
        res.put("shipped", shipped);
        res.put("delivered", delivered);
        res.put("cancelled", cancelled);
        res.put("total_revenue", Math.round(totalRevenue * 100.0) / 100.0);
        res.put("currency", "USD");
        res.put("generated_at", "2024-01-15T10:30:00Z");
        return ResponseEntity.ok(res);
    }
}
