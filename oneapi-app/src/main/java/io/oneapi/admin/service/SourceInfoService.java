package io.oneapi.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.oneapi.admin.connector.ConnectorFactory;
import io.oneapi.admin.dto.ConnectionTestResult;
import io.oneapi.admin.dto.SourceInfoDTO;
import io.oneapi.admin.entity.Permission;
import io.oneapi.admin.entity.Role;
import io.oneapi.admin.entity.SourceInfo;
import io.oneapi.admin.repository.PermissionRepository;
import io.oneapi.admin.repository.RoleRepository;
import io.oneapi.admin.repository.SourceInfoRepository;
import io.oneapi.admin.service.security.SecurityService;
import io.oneapi.sdk.base.Source;
import io.oneapi.sdk.model.ConnectionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing database connections.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SourceInfoService {

    private final SourceInfoRepository repository;
    private final ObjectMapper objectMapper;
    private final ConnectorFactory connectorFactory;
    private final SecurityService securityService;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional
    public SourceInfoDTO createConnection(SourceInfoDTO dto) {
        SourceInfo entity = toEntity(dto);
        entity = repository.save(entity);
        log.info("Created database connection: {}", entity.getName());

        // Auto-grant ROLE_ADMIN access to this source
        grantAdminAccessToSource(entity.getId());

        return toDTO(entity);
    }

    /**
     * Automatically grant ROLE_ADMIN access to a newly created source.
     * This allows admin users to see and manage the source in the UI.
     */
    private void grantAdminAccessToSource(Long sourceId) {
        try {
            // Find ROLE_ADMIN (typically id = 1, but search by name to be safe)
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElse(null);
            if (adminRole == null) {
                log.warn("ROLE_ADMIN not found, skipping auto-grant for source: {}", sourceId);
                return;
            }

            // Check if permission already exists
            List<Permission> existing = permissionRepository.findByRoleIdAndSourceId(adminRole.getId(), sourceId);
            if (!existing.isEmpty()) {
                log.debug("Permission already exists for ROLE_ADMIN on source: {}", sourceId);
                return;
            }

            // Create source-level permission for ROLE_ADMIN
            Permission permission = new Permission();
            permission.setRole(adminRole);
            permission.setSource(repository.findById(sourceId).orElse(null));
            permission.setDomain(null);  // Source-level access (not domain-specific)
            permission.setEntity(null);  // Source-level access (not entity-specific)
            permission.setField(null);   // Source-level access (not field-specific)
            permission.setPermissionType(Permission.PermissionType.READ);
            permission.setCreatedBy("system");
            permission.setCreatedAt(LocalDateTime.now());

            permissionRepository.save(permission);
            log.info("Auto-granted ROLE_ADMIN access to source: {}", sourceId);

        } catch (Exception e) {
            log.error("Failed to auto-grant ROLE_ADMIN access to source: {}", sourceId, e);
            // Don't fail the source creation if permission grant fails
        }
    }

    @Transactional
    public SourceInfoDTO updateConnection(Long id, SourceInfoDTO dto) {
        SourceInfo entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Connection not found: " + id));

        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setHost(dto.getHost());
        entity.setPort(dto.getPort());
        entity.setDatabase(dto.getDatabase());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setAdditionalParams(dto.getAdditionalParams());
        if (dto.getActive() != null) {
            entity.setActive(dto.getActive());
        }

        entity = repository.save(entity);
        log.info("Updated database connection: {}", entity.getName());
        return toDTO(entity);
    }

    @Transactional(readOnly = true)
    public List<SourceInfoDTO> getAllConnections() {
        List<SourceInfo> allSources = repository.findAll();
        List<SourceInfo> filteredSources = securityService.filterSources(allSources);
        return filteredSources.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SourceInfoDTO getConnectionById(Long id) {
        SourceInfo source = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Connection not found: " + id));

        // Check access
        if (!securityService.hasSourceAccess(id)) {
            throw new SecurityException("Access denied to source: " + id);
        }

        return toDTO(source);
    }

    @Transactional(readOnly = true)
    public SourceInfoDTO getConnectionByName(String name) {
        return repository.findByName(name)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Connection not found: " + name));
    }

    @Transactional
    public void deleteConnection(Long id) {
        repository.deleteById(id);
        log.info("Deleted database connection: {}", id);
    }

    public ConnectionTestResult testConnection(Long id) {
        SourceInfo connection = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Connection not found: " + id));

        return testConnection(connection);
    }

    public ConnectionTestResult testConnection(SourceInfoDTO dto) {
        return testConnection(toEntity(dto));
    }

    private ConnectionTestResult testConnection(SourceInfo connection) {
        try {
            Source source = connectorFactory.createSource(connection.getType());
            JsonNode config = connectorFactory.toConnectorConfig(connection);

            ConnectionStatus status = source.check(config);
            if (source instanceof AutoCloseable) {
                ((AutoCloseable) source).close();
            }

            return new ConnectionTestResult(
                    status.getStatus() == ConnectionStatus.Status.SUCCEEDED,
                    status.getMessage(),
                    status.getStatus().value()
            );
        } catch (Exception e) {
            log.error("Failed to test connection: {}", connection.getName(), e);
            return new ConnectionTestResult(false, e.getMessage(), "FAILED");
        }
    }

    private SourceInfo toEntity(SourceInfoDTO dto) {
        SourceInfo entity = new SourceInfo();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setHost(dto.getHost());
        entity.setPort(dto.getPort());
        entity.setDatabase(dto.getDatabase());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setAdditionalParams(dto.getAdditionalParams());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);
        return entity;
    }

    private SourceInfoDTO toDTO(SourceInfo entity) {
        SourceInfoDTO dto = new SourceInfoDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setType(entity.getType());
        dto.setHost(entity.getHost());
        dto.setPort(entity.getPort());
        dto.setDatabase(entity.getDatabase());
        dto.setUsername(entity.getUsername());
        // Don't return password in DTO
        dto.setAdditionalParams(entity.getAdditionalParams());
        dto.setActive(entity.getActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
