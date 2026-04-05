package com.mockapi.controller.modernized;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/modernized/api/files")
public class ModernizedFileController {

    // TC-F01: POST /files/import — simulate file import (camelCase response)
    @PostMapping("/import")
    public ResponseEntity<?> importFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "File is empty"
            ));
        }
        return ResponseEntity.ok(Map.of(
            "success",  true,
            "fileName", file.getOriginalFilename(),
            "fileSize", file.getSize(),
            "message",  "File imported successfully"
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
            case "orders" -> "orderId,userId,totalAmount,status\n" +
                             "ORD-10001,1,299.99,COMPLETED\n" +
                             "ORD-10002,2,149.50,PENDING\n";
            case "users"  -> "userId,name,email,status\n" +
                             "1,John Doe,john@example.com,ACTIVE\n" +
                             "2,Jane Smith,jane@example.com,ACTIVE\n";
            default       -> "productId,name,price,category\n" +
                             "PROD-001,Laptop Pro,999.99,Electronics\n" +
                             "PROD-002,Wireless Mouse,29.99,Electronics\n";
        };
    }
}
