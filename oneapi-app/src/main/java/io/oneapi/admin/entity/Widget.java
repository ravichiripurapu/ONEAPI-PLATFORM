package io.oneapi.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Widget entity for dashboard widgets (charts, tables, metrics).
 */
@Entity
@Table(name = "widget")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Widget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Lob
    @Column
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id")
    private SavedQuery query;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private Report report;

    @Column(name = "widget_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private WidgetType widgetType;

    @Column(name = "position_x")
    private Integer positionX;

    @Column(name = "position_y")
    private Integer positionY;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Lob
    @Column
    private String configuration;

    @Column(name = "refresh_interval_seconds")
    private Integer refreshIntervalSeconds;

    @CreatedBy
    @Column(name = "created_by", length = 50)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 50)
    private String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    public enum WidgetType {
        TABLE,
        LINE_CHART,
        BAR_CHART,
        PIE_CHART,
        AREA_CHART,
        METRIC,
        TEXT
    }
}
