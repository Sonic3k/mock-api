package com.mockapi.controller;

import com.mockapi.seeder.DataSeeder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestResetController {

    private final DataSeeder dataSeeder;

    public TestResetController(DataSeeder dataSeeder) {
        this.dataSeeder = dataSeeder;
    }

    /** POST /test/reset — clear all data and re-seed to original state */
    @PostMapping("/reset")
    public ResponseEntity<?> reset() {
        dataSeeder.resetAll();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Database reset to original seed data"));
    }
}
