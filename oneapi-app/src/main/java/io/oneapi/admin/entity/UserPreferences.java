package io.oneapi.admin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * User preferences for query session behavior.
 * Each user can customize their page size and TTL settings.
 */
@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;  // Username or email

    /**
     * Custom page size for this user (records per fetch).
     * If null, uses system default.
     */
    @Column(name = "page_size")
    private Integer pageSize;

    /**
     * Custom TTL in minutes for this user's query sessions.
     * If null, uses system default.
     */
    @Column(name = "ttl_minutes")
    private Integer ttlMinutes;

    /**
     * Maximum concurrent sessions allowed for this user.
     * If null, uses system default.
     */
    @Column(name = "max_concurrent_sessions")
    private Integer maxConcurrentSessions;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getTtlMinutes() {
        return ttlMinutes;
    }

    public void setTtlMinutes(Integer ttlMinutes) {
        this.ttlMinutes = ttlMinutes;
    }

    public Integer getMaxConcurrentSessions() {
        return maxConcurrentSessions;
    }

    public void setMaxConcurrentSessions(Integer maxConcurrentSessions) {
        this.maxConcurrentSessions = maxConcurrentSessions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
