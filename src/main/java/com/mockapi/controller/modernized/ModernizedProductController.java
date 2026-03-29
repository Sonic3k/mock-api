package com.mockapi.controller.modernized;

import com.mockapi.entity.Product;
import com.mockapi.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/modernized/api/products")
public class ModernizedProductController {

    private final ProductRepository productRepo;
    public ModernizedProductController(ProductRepository productRepo) { this.productRepo = productRepo; }

    // TC-P01/P02: FAIL — items → products, price String → Double, total_count → total
    @GetMapping
    public ResponseEntity<?> getCatalog(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "price") String sort) {

        List<Product> all = category != null ? productRepo.findByCategory(category) : productRepo.findAll();
        if ("price".equals(sort)) all.sort(Comparator.comparingDouble(Product::getPrice));
        if (limit < all.size()) all = all.subList(0, limit);

        List<Map<String, Object>> products = new ArrayList<>();
        for (Product p : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("product_id", p.getProductId());
            m.put("product_name", p.getName());
            m.put("category", p.getCategory());
            m.put("brand", p.getBrand());
            m.put("price", p.getPrice());         // DIFF: String → Double
            m.put("stock_qty", p.getStock());
            m.put("rating", p.getRating());
            products.add(m);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("products", products);             // DIFF: items → products
        res.put("total_count", products.size());
        res.put("category_filter", category);
        return ResponseEntity.ok(res);
    }

    // TC-P03/P04: FAIL — price String → Double
    @GetMapping("/{productId}")
    public ResponseEntity<?> getProduct(@PathVariable String productId) {
        return productRepo.findByProductId(productId).map(p -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("product_id", p.getProductId());
            res.put("product_name", p.getName());
            res.put("category", p.getCategory());
            res.put("brand", p.getBrand());
            res.put("price", p.getPrice());        // DIFF: String → Double
            res.put("stock_qty", p.getStock());
            res.put("description", p.getDescription());
            res.put("tags", p.getTags().split(","));
            res.put("rating", p.getRating());
            res.put("review_count", p.getReviewCount());
            res.put("created_at", p.getCreatedAtIso());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-P05/P06/P07: FAIL — results → data, price String → Double
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {

        List<Product> all = productRepo.findAll();
        List<Map<String, Object>> data = new ArrayList<>();

        for (Product p : all) {
            boolean match = true;
            if (q != null && !p.getName().toLowerCase().contains(q.toLowerCase())
                    && !p.getTags().toLowerCase().contains(q.toLowerCase())) match = false;
            if (brand != null && !p.getBrand().equalsIgnoreCase(brand)) match = false;
            if (minPrice != null && p.getPrice() < minPrice) match = false;
            if (maxPrice != null && p.getPrice() > maxPrice) match = false;

            if (match) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("product_id", p.getProductId());
                m.put("product_name", p.getName());
                m.put("category", p.getCategory());
                m.put("brand", p.getBrand());
                m.put("price", p.getPrice());      // DIFF: String → Double
                m.put("rating", p.getRating());
                data.add(m);
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("data", data);                     // DIFF: results → data
        res.put("total_count", data.size());
        res.put("query", q);
        return ResponseEntity.ok(res);
    }

    // TC-P08: PASS
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("product_id", "PROD-999");
        res.put("product_name", body.getOrDefault("name", "New Product"));
        res.put("category", body.getOrDefault("category", "misc"));
        res.put("price", body.getOrDefault("price", "0.00").toString());
        res.put("created_at", "2024-01-15T10:30:00Z");
        res.put("message", "Product created successfully");
        return ResponseEntity.status(201).body(res);
    }

    // TC-P09: PASS
    @GetMapping("/{productId}/inventory")
    public ResponseEntity<?> getInventory(@PathVariable String productId) {
        return productRepo.findByProductId(productId).map(p -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("product_id", p.getProductId());
            res.put("stock_qty", p.getStock());
            res.put("reserved_qty", (int)(p.getStock() * 0.1));
            res.put("available_qty", (int)(p.getStock() * 0.9));
            res.put("warehouse", "WH-MAIN");
            res.put("last_updated", "2024-01-15T08:00:00Z");
            res.put("status", p.getStock() > 0 ? "in_stock" : "out_of_stock");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-P10: PASS
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        List<Product> all = productRepo.findAll();
        Map<String, Long> catCount = new LinkedHashMap<>();
        for (Product p : all) catCount.merge(p.getCategory(), 1L, Long::sum);

        List<Map<String, Object>> categories = new ArrayList<>();
        for (Map.Entry<String, Long> e : catCount.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("category_name", e.getKey());
            m.put("product_count", e.getValue());
            categories.add(m);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("categories", categories);
        res.put("total_categories", categories.size());
        return ResponseEntity.ok(res);
    }

    // TC-P11: NOT IMPLEMENTED
    @PutMapping("/{productId}/price")
    public ResponseEntity<?> updatePrice(@PathVariable String productId, @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(404).body(Map.of("error", "Not implemented in modernized version", "code", "NOT_IMPLEMENTED"));
    }
}
