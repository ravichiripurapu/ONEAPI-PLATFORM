package io.oneapi.admin;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptPasswordGenerator {

    @Test
    public void generatePasswords() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("=== Generated BCrypt Hashes ===");
        System.out.println("admin123: " + encoder.encode("admin123"));
        System.out.println("dev123: " + encoder.encode("dev123"));
        System.out.println("user123: " + encoder.encode("user123"));
    }
}
