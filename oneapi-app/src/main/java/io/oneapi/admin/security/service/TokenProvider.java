package io.oneapi.admin.security.service;

import io.oneapi.admin.security.security.SecurityUtils;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Service for creating JWT tokens.
 */
@Service
public class TokenProvider {

    private static final Logger log = LoggerFactory.getLogger(TokenProvider.class);

    private static final String AUTHORITIES_KEY = "auth";

    private final JwtEncoder jwtEncoder;

    private final long tokenValidityInSeconds;

    private final long tokenValidityInSecondsForRememberMe;

    public TokenProvider(
        JwtEncoder jwtEncoder,
        @Value("${spring.security.authentication.jwt.token-validity-in-seconds:86400}") long tokenValidityInSeconds,
        @Value("${spring.security.authentication.jwt.token-validity-in-seconds-for-remember-me:604800}") long tokenValidityInSecondsForRememberMe
    ) {
        this.jwtEncoder = jwtEncoder;
        this.tokenValidityInSeconds = tokenValidityInSeconds;
        this.tokenValidityInSecondsForRememberMe = tokenValidityInSecondsForRememberMe;
    }

    public String createToken(Authentication authentication, boolean rememberMe) {
        String authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(" "));

        Instant now = Instant.now();
        Instant validity;
        if (rememberMe) {
            validity = now.plus(this.tokenValidityInSecondsForRememberMe, ChronoUnit.SECONDS);
        } else {
            validity = now.plus(this.tokenValidityInSeconds, ChronoUnit.SECONDS);
        }

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(validity)
            .subject(authentication.getName())
            .claim(AUTHORITIES_KEY, authorities)
            .build();

        JwsHeader jwsHeader = JwsHeader.with(SecurityUtils.JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }
}
