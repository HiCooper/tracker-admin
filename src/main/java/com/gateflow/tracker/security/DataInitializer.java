package com.gateflow.tracker.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AuthService authService;

    @Override
    public void run(String... args) {
        try {
            authService.ensureDefaultAdmin();
        } catch (Exception e) {
            log.warn("Failed to ensure default admin user (table may not exist yet): {}", e.getMessage());
        }
    }
}
