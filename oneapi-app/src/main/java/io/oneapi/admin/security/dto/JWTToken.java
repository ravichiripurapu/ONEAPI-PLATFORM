package io.oneapi.admin.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for JWT token response
 */
public class JWTToken {

    private String idToken;

    public JWTToken(String idToken) {
        this.idToken = idToken;
    }

    @JsonProperty("id_token")
    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
