package io.oneapi.admin.service.reporting;

import io.oneapi.admin.dto.reporting.AuditLogDTO;
import io.oneapi.admin.entity.AuditLog;
import io.oneapi.admin.mapper.ReportingMapper;
import io.oneapi.admin.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing audit logs.
 * This is a READ-ONLY service for users - logs are created internally by the system.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ReportingMapper mapper;

    /**
     * Internal method to create audit log entries.
     * This should only be called by the system, not exposed to users.
     */
    public AuditLogDTO log(
        String userLogin,
        AuditLog.Action action,
        String entityType,
        Long entityId,
        String details,
        AuditLog.Status status
    ) {
        log.debug("Creating audit log: user={}, action={}, entityType={}, entityId={}",
            userLogin, action, entityType, entityId);

        AuditLog entity = new AuditLog();
        entity.setUserLogin(userLogin);
        entity.setAction(action);
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setDetails(details);
        entity.setStatus(status);
        entity.setTimestamp(LocalDateTime.now());

        AuditLog saved = auditLogRepository.save(entity);
        log.info("Created audit log with ID: {}", saved.getId());

        return mapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public Optional<AuditLogDTO> findById(Long id) {
        log.debug("Finding audit log by ID: {}", id);
        return auditLogRepository.findById(id).map(mapper::toDTO);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> findByUser(String userLogin) {
        log.debug("Finding audit logs for user: {}", userLogin);
        return mapper.auditLogsToDTOs(auditLogRepository.findByUserLogin(userLogin));
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> findByAction(AuditLog.Action action) {
        log.debug("Finding audit logs for action: {}", action);
        return mapper.auditLogsToDTOs(auditLogRepository.findByAction(action));
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> findByEntity(String entityType, Long entityId) {
        log.debug("Finding audit logs for entity: type={}, id={}", entityType, entityId);
        return mapper.auditLogsToDTOs(auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId));
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> findByDateRange(LocalDateTime start, LocalDateTime end) {
        log.debug("Finding audit logs between {} and {}", start, end);
        return mapper.auditLogsToDTOs(auditLogRepository.findByTimestampBetween(start, end));
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> findByStatus(AuditLog.Status status) {
        log.debug("Finding audit logs with status: {}", status);
        return mapper.auditLogsToDTOs(auditLogRepository.findByStatus(status));
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> findUserActivity(String userLogin, LocalDateTime start, LocalDateTime end) {
        log.debug("Finding user activity for {} between {} and {}", userLogin, start, end);
        return mapper.auditLogsToDTOs(auditLogRepository.findUserActivityInPeriod(userLogin, start, end));
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> findEntityHistory(String entityType, Long entityId) {
        log.debug("Finding entity history for type={}, id={}", entityType, entityId);
        return mapper.auditLogsToDTOs(auditLogRepository.findEntityHistory(entityType, entityId));
    }
}
