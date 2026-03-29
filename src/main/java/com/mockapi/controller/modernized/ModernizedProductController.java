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

    public ModernizedProductController(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    // TC-P01: GET /products
    // DIFF: "items" key renamed to "products", price String → Double, extra "meta"
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
            m.put("productId", p.getProductId());
            m.put("name", p.getName());               // DIFF: product_name → name
            m.put("category", p.getCategory());
            m.put("brand", p.getBrand());
            m.put("price", p.getPrice());             // DIFF: String → Double
            m.put("stock", p.getStock());             // DIFF: stock_qty → stock
            m.put("rating", p.getRating());
            products.add(m);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("products", products);                // DIFF: "items" → "products"
        res.put("total", products.size());            // DIFF: total_count → total
        res.put("categoryFilter", category);
        res.put("meta", Map.of("apiVersion", "2.0")); // DIFF: extra field
        return ResponseEntity.ok(res);
    }

    // TC-P02: GET /products/{productId}
    // DIFF: price as Double, field renames, different tags format
    @GetMapping("/{productId}")
    public ResponseEntity<?> getProduct(@PathVariable String productId) {
        return productRepo.findByProductId(productId).map(p -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("productId", p.getProductId());
            res.put("name", p.getName());
            res.put("category", p.getCategory());
            res.put("brand", p.getBrand());
            res.put("price", p.getPrice());           // DIFF: String → Double
            res.put("stock", p.getStock());
            res.put("description", p.getDescription());
            res.put("tags", p.getTags().split(","));
            res.put("rating", p.getRating());
            res.put("reviewCount", p.getReviewCount()); // DIFF: review_count → reviewCount
            res.put("createdAt", p.getCreatedAtEpoch()); // DIFF: ISO → epoch
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-P03: GET /products/search
    // DIFF: "results" → "data", price as Double, extra "facets" field
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {

        List<Product> all = productRepo.findAll();
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Long> brandFacets = new LinkedHashMap<>();

        for (Product p : all) {
            boolean match = true;
            if (q != null && !p.getName().toLowerCase().contains(q.toLowerCase())
                    && !p.getTags().toLowerCase().contains(q.toLowerCase())) match = false;
            if (brand != null && !p.getBrand().equalsIgnoreCase(brand)) match = false;
            if (minPrice != null && p.getPrice() < minPrice) match = false;
            if (maxPrice != null && p.getPrice() > maxPrice) match = false;

            if (match) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("productId", p.getProductId());
                m.put("name", p.getName());
                m.put("category", p.getCategory());
                m.put("brand", p.getBrand());
                m.put("price", p.getPrice());         // DIFF: String → Double
                m.put("rating", p.getRating());
                data.add(m);
                brandFacets.merge(p.getBrand(), 1L, Long::sum);
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("data", data);                        // DIFF: "results" → "data"
        res.put("total", data.size());                // DIFF: total_count → total
        res.put("query", q);
        res.put("facets", Map.of("brands", brandFacets)); // DIFF: extra facets
        return ResponseEntity.ok(res);
    }

    // TC-P04: POST /products — SAME (pass)
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("productId", "PROD-999");
        res.put("name", body.getOrDefault("name", "New Product"));
        res.put("category", body.getOrDefault("category", "misc"));
        res.put("price", body.getOrDefault("price", 0.0));
        res.put("createdAt", "2024-01-15T10:30:00Z");
        res.put("message", "Product created successfully");
        return ResponseEntity.status(201).body(res);
    }

    // TC-P05: GET /products/{productId}/inventory — SAME (pass)
    @GetMapping("/{productId}/inventory")
    public ResponseEntity<?> getInventory(@PathVariable String productId) {
        return productRepo.findByProductId(productId).map(p -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("productId", p.getProductId());
            res.put("stock", p.getStock());
            res.put("reserved", (int)(p.getStock() * 0.1));
            res.put("available", (int)(p.getStock() * 0.9));
            res.put("warehouse", "WH-MAIN");
            res.put("lastUpdated", "2024-01-15T08:00:00Z");
            res.put("status", p.getStock() > 0 ? "in_stock" : "out_of_stock");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-P06: GET /products/categories — SAME (pass)
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        List<Product> all = productRepo.findAll();
        Map<String, Long> catCount = new LinkedHashMap<>();
        for (Product p : all) {
            catCount.merge(p.getCategory(), 1L, Long::sum);
        }
        List<Map<String, Object>> categories = new ArrayList<>();
        for (Map.Entry<String, Long> e : catCount.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.getKey());
            m.put("count", e.getValue());
            categories.add(m);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("categories", categories);
        res.put("total", categories.size());
        return ResponseEntity.ok(res);
    }

    // TC-P07: PUT /products/{productId}/price — NOT IMPLEMENTED
    @PutMapping("/{productId}/price")
    public ResponseEntity<?> updatePrice(@PathVariable String productId, @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(404).body(Map.of(
                "error", "Not implemented in modernized version",
                "code", "NOT_IMPLEMENTED",
                "note", "Use PUT /modernized/api/products/{productId} to update full product"
        ));
    }
}
