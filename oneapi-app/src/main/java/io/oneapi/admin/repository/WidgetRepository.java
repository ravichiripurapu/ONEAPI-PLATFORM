package io.oneapi.admin.repository;

import io.oneapi.admin.entity.Widget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the Widget entity.
 */
@Repository
public interface WidgetRepository extends JpaRepository<Widget, Long> {

    List<Widget> findByDashboardId(Long dashboardId);

    List<Widget> findByQueryId(Long queryId);

    List<Widget> findByReportId(Long reportId);

    List<Widget> findByWidgetType(Widget.WidgetType widgetType);

    List<Widget> findByDashboardIdOrderByPositionYAscPositionXAsc(Long dashboardId);
}
