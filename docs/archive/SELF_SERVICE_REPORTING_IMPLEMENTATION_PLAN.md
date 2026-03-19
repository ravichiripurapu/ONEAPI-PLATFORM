# Self-Service Reporting API - Implementation Plan

**Target**: Complete implementation in 1 day (8 hours)

## Current Architecture - What We Have ✅

### Security & Authentication
- ✅ JWT authentication with database-backed users
- ✅ Role-based access control (RBAC)
- ✅ Database/Table/Column level permissions
- ✅ AccessControlService for enforcing permissions
- ✅ User entity with authorities

### Data Management
- ✅ Database connection management (DatabaseConnection entity)
- ✅ Query execution with session-based pagination
- ✅ Schema discovery (list schemas, tables, columns)
- ✅ Multiple database support (PostgreSQL, H2, SQL Server)
- ✅ Connection pooling and testing

### Existing Endpoints
- ✅ `/api/authenticate` - JWT login
- ✅ `/api/connections` - Database connection CRUD
- ✅ `/api/query` - Execute queries with pagination
- ✅ `/api/execute` - Execute SQL statements
- ✅ `/api/schema/**` - Schema discovery endpoints

---

## Gap Analysis - What's Missing ❌

### 1. **Metadata Management** ❌
- ❌ Catalog management (organize data sources)
- ❌ Metadata sync and discovery automation
- ❌ Table/column metadata storage with descriptions

### 2. **Query Management** ❌
- ❌ Saved queries (store, version, share)
- ❌ Query preview (validate before execution)
- ❌ Query history and favorites

### 3. **Reports** ❌
- ❌ Report entity and management
- ❌ Report parameters and templating
- ❌ Report execution with output formats (JSON, CSV, Excel)
- ❌ Report sharing and permissions

### 4. **Dashboards** ❌
- ❌ Dashboard entity and layout
- ❌ Widget management (charts, tables, metrics)
- ❌ Dashboard data aggregation
- ❌ Real-time dashboard refresh

### 5. **Scheduling** ❌
- ❌ Schedule entity (cron expressions)
- ❌ Report/query scheduling
- ❌ Email notifications
- ❌ Background job execution

### 6. **Monitoring & Audit** ❌
- ❌ Audit log entity and tracking
- ❌ Usage metrics and analytics
- ❌ Performance monitoring

### 7. **API Versioning** ❌
- ❌ Current APIs use `/api/*` instead of `/api/v1/*`

---

## Phase-Wise Implementation Plan (8 Hours)

### **Phase 1: Foundation & API Restructuring** (1.5 hours)
**Goal**: Prepare architecture and versioning

**Tasks**:
1. Create v1 API structure (`/api/v1/**`)
2. Update authentication endpoints to `/api/v1/auth/**`
3. Create base entities: `Catalog`, `SavedQuery`, `Report`, `Dashboard`, `Schedule`
4. Create repositories for new entities
5. Update Liquibase changelog with new tables

**Deliverables**:
- New entity structure
- Database schema for all features
- API versioning structure

---

### **Phase 2: Catalog & Metadata Management** (1.5 hours)
**Goal**: Implement catalog and metadata discovery

**Tasks**:
1. Implement `CatalogService` and `CatalogController`
2. Implement metadata discovery automation
3. Create metadata sync endpoints
4. Add catalog assignment to data sources

**Endpoints**:
- `GET /api/v1/admin/catalogs`
- `POST /api/v1/admin/catalogs`
- `PUT /api/v1/admin/catalogs/{id}`
- `DELETE /api/v1/admin/catalogs/{id}`
- `POST /api/v1/admin/data-sources/{id}/discover`
- `POST /api/v1/admin/data-sources/{id}/sync-metadata`

**Deliverables**:
- Catalog CRUD operations
- Automated metadata discovery
- Metadata sync functionality

---

### **Phase 3: Query Management** (1 hour)
**Goal**: Implement saved queries and query preview

**Tasks**:
1. Implement `SavedQueryService` and `SavedQueryController`
2. Add query validation and preview
3. Implement query favorites
4. Add query sharing with permissions

**Endpoints**:
- `GET /api/v1/queries`
- `POST /api/v1/queries`
- `GET /api/v1/queries/{id}`
- `PUT /api/v1/queries/{id}`
- `DELETE /api/v1/queries/{id}`
- `POST /api/v1/queries/preview`
- `POST /api/v1/queries/execute`

**Deliverables**:
- Saved query management
- Query preview functionality
- Query execution with saved queries

---

### **Phase 4: Reports** (1.5 hours)
**Goal**: Implement report creation and execution

**Tasks**:
1. Create `Report` entity with parameters
2. Implement `ReportService` and `ReportController`
3. Add report parameter substitution
4. Implement report execution with multiple output formats
5. Add report permissions

**Endpoints**:
- `GET /api/v1/reports`
- `POST /api/v1/reports`
- `GET /api/v1/reports/{id}`
- `PUT /api/v1/reports/{id}`
- `DELETE /api/v1/reports/{id}`
- `POST /api/v1/reports/{id}/run`

**Deliverables**:
- Report CRUD operations
- Report execution with parameters
- Output formats: JSON, CSV

---

### **Phase 5: Dashboards** (1.5 hours)
**Goal**: Implement dashboard creation and widget management

**Tasks**:
1. Create `Dashboard` and `Widget` entities
2. Implement `DashboardService` and `DashboardController`
3. Add widget types (chart, table, metric, etc.)
4. Implement dashboard data aggregation
5. Add dashboard sharing

**Endpoints**:
- `GET /api/v1/dashboards`
- `POST /api/v1/dashboards`
- `GET /api/v1/dashboards/{id}`
- `PUT /api/v1/dashboards/{id}`
- `DELETE /api/v1/dashboards/{id}`
- `POST /api/v1/dashboards/{id}/widgets`
- `GET /api/v1/dashboards/{id}/data`

**Deliverables**:
- Dashboard CRUD operations
- Widget management
- Dashboard data fetching

---

### **Phase 6: Scheduling & Background Jobs** (1 hour)
**Goal**: Implement report/query scheduling

**Tasks**:
1. Add Spring Scheduler dependency
2. Create `Schedule` entity
3. Implement `ScheduleService` with cron expressions
4. Create background job executor
5. Add basic email notification (optional)

**Endpoints**:
- `GET /api/v1/schedules`
- `POST /api/v1/schedules`
- `GET /api/v1/schedules/{id}`
- `PUT /api/v1/schedules/{id}`
- `DELETE /api/v1/schedules/{id}`

**Deliverables**:
- Schedule CRUD operations
- Cron-based execution
- Background job processing

---

### **Phase 7: Audit & Monitoring** (30 minutes)
**Goal**: Implement audit logging and usage tracking

**Tasks**:
1. Create `AuditLog` entity
2. Implement audit logging interceptor
3. Create usage metrics service
4. Add audit log endpoints

**Endpoints**:
- `GET /api/v1/admin/audit-logs`
- `GET /api/v1/admin/usage`

**Deliverables**:
- Audit logging for all operations
- Usage metrics dashboard
- Admin monitoring endpoints

---

### **Phase 8: Testing & Documentation** (30 minutes)
**Goal**: Test all endpoints and create API documentation

**Tasks**:
1. Test all new endpoints with Postman/curl
2. Verify JWT authentication on all endpoints
3. Test RBAC and permissions
4. Update API documentation
5. Create Postman collection

**Deliverables**:
- Tested and working API
- Complete API documentation
- Postman collection for all endpoints

---

## Database Schema - New Tables Required

```sql
-- Catalogs
CREATE TABLE catalog (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by VARCHAR(50),
    created_date TIMESTAMP,
    last_modified_by VARCHAR(50),
    last_modified_date TIMESTAMP
);

-- Saved Queries
CREATE TABLE saved_query (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sql_query TEXT NOT NULL,
    data_source_id BIGINT NOT NULL,
    catalog_id BIGINT,
    is_favorite BOOLEAN DEFAULT FALSE,
    owner_id BIGINT NOT NULL,
    is_shared BOOLEAN DEFAULT FALSE,
    created_date TIMESTAMP,
    last_modified_date TIMESTAMP,
    FOREIGN KEY (data_source_id) REFERENCES database_connections(id),
    FOREIGN KEY (catalog_id) REFERENCES catalog(id),
    FOREIGN KEY (owner_id) REFERENCES app_user(id)
);

-- Reports
CREATE TABLE report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    query_id BIGINT,
    parameters JSON,
    output_format VARCHAR(50) DEFAULT 'JSON',
    owner_id BIGINT NOT NULL,
    is_shared BOOLEAN DEFAULT FALSE,
    created_date TIMESTAMP,
    last_modified_date TIMESTAMP,
    FOREIGN KEY (query_id) REFERENCES saved_query(id),
    FOREIGN KEY (owner_id) REFERENCES app_user(id)
);

-- Dashboards
CREATE TABLE dashboard (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    layout JSON,
    owner_id BIGINT NOT NULL,
    is_shared BOOLEAN DEFAULT FALSE,
    created_date TIMESTAMP,
    last_modified_date TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES app_user(id)
);

-- Dashboard Widgets
CREATE TABLE widget (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dashboard_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    query_id BIGINT,
    config JSON,
    position JSON,
    created_date TIMESTAMP,
    FOREIGN KEY (dashboard_id) REFERENCES dashboard(id) ON DELETE CASCADE,
    FOREIGN KEY (query_id) REFERENCES saved_query(id)
);

-- Schedules
CREATE TABLE schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    cron_expression VARCHAR(100) NOT NULL,
    report_id BIGINT,
    query_id BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    email_recipients TEXT,
    owner_id BIGINT NOT NULL,
    last_run_date TIMESTAMP,
    next_run_date TIMESTAMP,
    created_date TIMESTAMP,
    FOREIGN KEY (report_id) REFERENCES report(id),
    FOREIGN KEY (query_id) REFERENCES saved_query(id),
    FOREIGN KEY (owner_id) REFERENCES app_user(id)
);

-- Audit Logs
CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    details TEXT,
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_date TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES app_user(id)
);
```

---

## Technology Stack

**Existing**:
- Spring Boot 3.2.0
- Spring Security with JWT
- H2 Database (file-based)
- Liquibase for migrations
- Lombok
- Jackson for JSON

**New Dependencies Needed**:
- Spring Scheduler (already included in Spring Boot)
- Apache Commons CSV (for CSV export)
- Apache POI (for Excel export - optional)

---

## Success Criteria

After 8 hours, we will have:

✅ All 10 API categories implemented
✅ Database schema created via Liquibase
✅ JWT authentication on all endpoints
✅ RBAC and permission enforcement
✅ Catalog and metadata management
✅ Saved queries with preview
✅ Reports with parameter substitution
✅ Dashboards with widgets
✅ Scheduling with cron expressions
✅ Audit logging and usage metrics
✅ Complete API documentation
✅ Postman collection for testing

---

## Notes

- **Focus**: Core functionality first, polish later
- **Testing**: Test each phase before moving to next
- **Documentation**: Document as we build
- **Incremental**: Each phase is independently testable
- **Reuse**: Leverage existing AccessControlService, QuerySessionManager, etc.

---

## Starting Point

Current application status:
- ✅ Running on port 8080
- ✅ JWT authentication working
- ✅ Database connections functional
- ✅ Query execution operational
- ✅ Schema discovery available

**Ready to start implementation!**
