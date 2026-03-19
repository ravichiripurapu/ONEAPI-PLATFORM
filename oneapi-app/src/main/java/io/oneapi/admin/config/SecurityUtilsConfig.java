package io.oneapi.admin.config;

import io.oneapi.admin.security.repository.UserRepository;
import io.oneapi.admin.security.security.SecurityUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to inject UserRepository into SecurityUtils static utility class
 */
@Configuration
@RequiredArgsConstructor
public class SecurityUtilsConfig {

    private final UserRepository userRepository;

    @PostConstruct
    public void init() {
        SecurityUtils.setUserRepository(userRepository);
    }
}
