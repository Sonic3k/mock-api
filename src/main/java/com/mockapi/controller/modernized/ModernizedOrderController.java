package com.mockapi.controller.modernized;

import com.mockapi.config.TokenValidator;
import com.mockapi.entity.Order;
import com.mockapi.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/modernized/api/orders")
public class ModernizedOrderController {

    private final OrderRepository orderRepo;
    private final TokenValidator tokenValidator;

    public ModernizedOrderController(OrderRepository orderRepo, TokenValidator tokenValidator) {
        this.orderRepo = orderRepo;
        this.tokenValidator = tokenValidator;
    }

    // TC-O01/O02/O03: FAIL — flat shipping → nested shipping object — PROTECTED
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        if (!tokenValidator.isValid(auth)) return TokenValidator.unauthorized();
        return orderRepo.findByOrderId(orderId).map(o -> {
            Map<String, Object> shipping = new LinkedHashMap<>();
            shipping.put("street", o.getShippingStreet());
            shipping.put("city", o.getShippingCity());
            shipping.put("country", o.getShippingCountry());
            shipping.put("method", o.getShippingMethod());

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("order_id", o.getOrderId());
            res.put("user_id", o.getUserId());
            res.put("order_status", o.getStatus());
            res.put("total_amount", o.getTotalAmount());
            res.put("currency", o.getCurrency());
            res.put("items_summary", o.getItemsSummary());
            res.put("item_count", o.getItemCount());
            res.put("shipping", shipping);              // DIFF: nested vs flat shipping_street/city/country
            res.put("created_at", o.getCreatedAtIso());
            if (o.getCancelReason() != null) res.put("cancel_reason", o.getCancelReason());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-O04/O05/O06: FAIL — total_records → total_items
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
        pagination.put("total_items", total);   // DIFF: total_records → total_items
        pagination.put("total_pages", (int) Math.ceil((double) total / limit));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("orders", orders);
        res.put("pagination", pagination);
        return ResponseEntity.ok(res);
    }

    // TC-O07: NOT IMPLEMENTED
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderId, @RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.status(404).body(Map.of("error", "Not implemented in modernized version", "code", "NOT_IMPLEMENTED"));
    }

    // TC-O08: PASS
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

    // POST /orders — actually persist to DB
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

    // TC-O10: PASS
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

    // TC-O11: FAIL — field renames in summary
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary() {
        List<Order> all = orderRepo.findAll();
        long confirmed = all.stream().filter(o -> "confirmed".equals(o.getStatus())).count();
        long shipped   = all.stream().filter(o -> "shipped".equals(o.getStatus())).count();
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
        res.put("revenue", Math.round(totalRevenue * 100.0) / 100.0); // DIFF: total_revenue → revenue
        res.put("currency", "USD");
        res.put("generated_at", "2024-01-15T10:30:00Z");
        res.put("completion_rate", delivered > 0 ? Math.round((double) delivered / all.size() * 100) : 0); // DIFF: extra field
        return ResponseEntity.ok(res);
    }
}
