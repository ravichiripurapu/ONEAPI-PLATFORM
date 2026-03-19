# Self-Service Reporting & Dashboarding API Specification

## Overview
This document defines REST API endpoints for a self-service reporting and dashboarding platform with Admin and User personas.

---

## 1. Authentication APIs

### Auth
- POST /api/v1/auth/login
- POST /api/v1/auth/logout
- POST /api/v1/auth/refresh-token
- GET /api/v1/auth/me
- POST /api/v1/auth/change-password

---

## 2. User & Role Management

- GET /api/v1/users
- POST /api/v1/users
- GET /api/v1/users/{userId}
- PUT /api/v1/users/{userId}
- DELETE /api/v1/users/{userId}

- GET /api/v1/roles
- POST /api/v1/roles
- GET /api/v1/roles/{roleId}
- PUT /api/v1/roles/{roleId}
- DELETE /api/v1/roles/{roleId}

---

## 3. Admin - Data Sources

- GET /api/v1/admin/data-sources
- POST /api/v1/admin/data-sources
- GET /api/v1/admin/data-sources/{id}
- PUT /api/v1/admin/data-sources/{id}
- DELETE /api/v1/admin/data-sources/{id}
- POST /api/v1/admin/data-sources/{id}/test-connection

---

## 4. Metadata Discovery

- POST /api/v1/admin/data-sources/{id}/discover
- POST /api/v1/admin/data-sources/{id}/sync-metadata
- GET /api/v1/admin/data-sources/{id}/schemas
- GET /api/v1/admin/schemas/{schema}/tables
- GET /api/v1/admin/tables/{table}/columns

---

## 5. Catalog APIs

- GET /api/v1/admin/catalogs
- POST /api/v1/admin/catalogs
- PUT /api/v1/admin/catalogs/{id}
- DELETE /api/v1/admin/catalogs/{id}

---

## 6. Query APIs

- GET /api/v1/queries
- POST /api/v1/queries
- GET /api/v1/queries/{id}
- PUT /api/v1/queries/{id}
- DELETE /api/v1/queries/{id}

- POST /api/v1/queries/preview
- POST /api/v1/queries/execute

---

## 7. Reports

- GET /api/v1/reports
- POST /api/v1/reports
- GET /api/v1/reports/{id}
- PUT /api/v1/reports/{id}
- DELETE /api/v1/reports/{id}

- POST /api/v1/reports/{id}/run

---

## 8. Dashboards

- GET /api/v1/dashboards
- POST /api/v1/dashboards
- GET /api/v1/dashboards/{id}
- PUT /api/v1/dashboards/{id}
- DELETE /api/v1/dashboards/{id}

- POST /api/v1/dashboards/{id}/widgets
- GET /api/v1/dashboards/{id}/data

---

## 9. Scheduling

- GET /api/v1/schedules
- POST /api/v1/schedules
- GET /api/v1/schedules/{id}
- PUT /api/v1/schedules/{id}
- DELETE /api/v1/schedules/{id}

---

## 10. Monitoring & Audit

- GET /api/v1/admin/audit-logs
- GET /api/v1/admin/usage

---

## Notes
- Use role-based access control (RBAC)
- Apply row-level and column-level security
- Avoid exposing raw SQL directly
- Prefer metadata-driven query generation

