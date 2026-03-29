package com.bankapp.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer {

    @PostConstruct
    public void init() {
        try {
            DatabaseManager.initializeDatabase();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize local database", e);
        }
    }
}
