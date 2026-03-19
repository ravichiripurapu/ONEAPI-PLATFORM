package io.oneapi.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for connection test results.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionTestResult {
    private Boolean success;
    private String message;
    private String status;
}
