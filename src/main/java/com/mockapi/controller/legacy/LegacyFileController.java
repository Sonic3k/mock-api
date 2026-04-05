package com.mockapi.controller.legacy;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/legacy/api/files")
public class LegacyFileController {

    // TC-F01: POST /files/import — simulate file import
    @PostMapping("/import")
    public ResponseEntity<?> importFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "File is empty"
            ));
        }
        return ResponseEntity.ok(Map.of(
            "success",   true,
            "file_name", file.getOriginalFilename(),
            "file_size", file.getSize(),
            "message",   "File imported successfully"
        ));
    }

    // TC-F02: GET /files/export — simulate file export, returns CSV bytes
    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportFile(
            @RequestParam(defaultValue = "products") String type) {

        String csv = buildCsv(type);
        byte[] bytes = csv.getBytes();
        String filename = type + "_export.csv";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .contentLength(bytes.length)
            .body(new ByteArrayResource(bytes));
    }

    private String buildCsv(String type) {
        return switch (type) {
            case "orders" -> "order_id,user_id,total_amount,status\n" +
                             "ORD-10001,1,299.99,completed\n" +
                             "ORD-10002,2,149.50,pending\n";
            case "users"  -> "user_id,name,email,status\n" +
                             "1,John Doe,john@example.com,active\n" +
                             "2,Jane Smith,jane@example.com,active\n";
            default       -> "product_id,name,price,category\n" +
                             "PROD-001,Laptop Pro,999.99,Electronics\n" +
                             "PROD-002,Wireless Mouse,29.99,Electronics\n";
        };
    }
}
