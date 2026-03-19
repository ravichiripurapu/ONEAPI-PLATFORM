package io.oneapi.admin.dto;

import io.oneapi.admin.entity.SourceInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for database connection requests and responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceInfoDTO {

    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Database type is required")
    private SourceInfo.DatabaseType type;

    // Host can be empty for embedded databases like H2
    private String host;

    // Port can be 0 for embedded databases
    private Integer port;

    @NotBlank(message = "Database name is required")
    private String database;

    private String username;
    private String password;
    private String additionalParams;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
