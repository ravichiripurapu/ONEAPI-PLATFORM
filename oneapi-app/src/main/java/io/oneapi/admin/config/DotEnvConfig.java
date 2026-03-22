package io.oneapi.admin.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration to load environment variables from .env file.
 * This allows storing sensitive configuration like API keys outside of version control.
 */
@Configuration
@Slf4j
public class DotEnvConfig {

    @PostConstruct
    public void loadDotEnv() {
        // Try current directory first, then parent directory (project root)
        Path envFile = Paths.get(".env");
        if (!Files.exists(envFile)) {
            envFile = Paths.get("../.env");
        }

        if (!Files.exists(envFile)) {
            log.info("No .env file found. Using system environment variables or application.yml defaults.");
            return;
        }

        log.info("Loading .env file from: {}", envFile.toAbsolutePath());

        try (BufferedReader reader = new BufferedReader(new FileReader(envFile.toFile()))) {
            String line;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Parse KEY=VALUE format
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();

                    // Only set if not already set in system environment
                    if (System.getenv(key) == null) {
                        System.setProperty(key, value);
                        log.debug("Loaded environment variable from .env: {}", key);
                        count++;
                    } else {
                        log.debug("Skipping {} - already set in system environment", key);
                    }
                }
            }

            log.info("Successfully loaded {} environment variables from .env file", count);
        } catch (IOException e) {
            log.warn("Could not read .env file: {}", e.getMessage());
            log.warn("This is normal if you're using system environment variables instead");
        }
    }
}
