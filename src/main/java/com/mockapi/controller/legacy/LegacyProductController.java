package com.mockapi.controller.legacy;

import com.mockapi.entity.Product;
import com.mockapi.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/legacy/api/products")
public class LegacyProductController {

    private final ProductRepository productRepo;

    public LegacyProductController(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    // TC-P01: GET /products — catalog with filtering
    @GetMapping
    public ResponseEntity<?> getCatalog(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "price") String sort) {

        List<Product> all = category != null ? productRepo.findByCategory(category) : productRepo.findAll();
        if ("price".equals(sort)) all.sort(Comparator.comparingDouble(Product::getPrice));
        if (limit < all.size()) all = all.subList(0, limit);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Product p : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("product_id", p.getProductId());
            m.put("product_name", p.getName());
            m.put("category", p.getCategory());
            m.put("brand", p.getBrand());
            m.put("price", p.getPriceStr());      // String price (legacy)
            m.put("stock_qty", p.getStock());
            m.put("rating", p.getRating());
            items.add(m);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("items", items);                  // legacy: "items" key
        res.put("total_count", items.size());
        res.put("category_filter", category);
        return ResponseEntity.ok(res);
    }

    // TC-P02: GET /products/{productId}
    @GetMapping("/{productId}")
    public ResponseEntity<?> getProduct(@PathVariable String productId) {
        return productRepo.findByProductId(productId).map(p -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("product_id", p.getProductId());
            res.put("product_name", p.getName());
            res.put("category", p.getCategory());
            res.put("brand", p.getBrand());
            res.put("price", p.getPriceStr());     // String price (legacy)
            res.put("stock_qty", p.getStock());
            res.put("description", p.getDescription());
            res.put("tags", p.getTags().split(","));
            res.put("rating", p.getRating());
            res.put("review_count", p.getReviewCount());
            res.put("created_at", p.getCreatedAtIso());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-P03: GET /products/search — full-text search, returns "results" key
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {

        List<Product> all = productRepo.findAll();
        List<Map<String, Object>> results = new ArrayList<>();

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
                m.put("price", p.getPriceStr());  // String price (legacy)
                m.put("rating", p.getRating());
                results.add(m);
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("results", results);              // legacy: "results" key
        res.put("total_count", results.size());
        res.put("query", q);
        return ResponseEntity.ok(res);
    }

    // TC-P04: POST /products — create product
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

    // TC-P05: GET /products/{productId}/inventory
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

    // TC-P06: GET /products/categories
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
            m.put("category_name", e.getKey());
            m.put("product_count", e.getValue());
            categories.add(m);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("categories", categories);
        res.put("total_categories", categories.size());
        return ResponseEntity.ok(res);
    }

    // TC-P07: PUT /products/{productId}/price
    @PutMapping("/{productId}/price")
    public ResponseEntity<?> updatePrice(@PathVariable String productId, @RequestBody Map<String, Object> body) {
        return productRepo.findByProductId(productId).map(p -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("product_id", p.getProductId());
            res.put("product_name", p.getName());
            res.put("old_price", p.getPriceStr());
            res.put("new_price", body.getOrDefault("price", p.getPriceStr()).toString());
            res.put("updated_at", "2024-01-15T10:30:00Z");
            res.put("message", "Price updated successfully");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // TC-P-TAGS: GET /products/{productId}/tags
    // Legacy returns tags sorted alphabetically
    @GetMapping("/{productId}/tags")
    public ResponseEntity<?> getTags(@PathVariable String productId) {
        return productRepo.findByProductId(productId).map(p -> {
            List<String> tags = Arrays.asList(p.getTags().split(","));
            Collections.sort(tags); // alphabetical order
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("product_id", p.getProductId());
            res.put("tags", tags);
            res.put("total", tags.size());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }
}
