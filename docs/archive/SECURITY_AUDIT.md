# OneAPI Platform - Security Audit & Endpoint Documentation

## Security Overview

### ✅ Security Features Implemented

1. **JWT Authentication** - Stateless token-based auth
2. **Role-Based Access Control (RBAC)** - ADMIN and USER roles
3. **Method-Level Security** - `@PreAuthorize` annotations on 85 endpoints
4. **Database-Backed Users** - Users stored in database with BCrypt passwords
5. **Fine-Grained Permissions** - Database, Table, and Column level permissions
6. **CSRF Protection** - Disabled for API (using JWT), enabled for web
7. **CORS Support** - Configured for cross-origin requests

---

## Security Configuration

**File**: [SecurityConfig.java](oneapi-app/src/main/java/io/oneapi/admin/config/SecurityConfig.java)

### Public Endpoints (No Authentication Required)

```java
// Authentication
POST /api/authenticate           - Login (get JWT token)
POST /api/register               - User registration
POST /api/account/reset-password/init   - Request password reset
POST /api/account/reset-password/finish - Complete password reset

// Development Tools (should be disabled in production!)
/actuator/**                     - Health checks, metrics
/h2-console/**                   - H2 database console
/swagger-ui/**                   - Swagger API documentation
/api-docs/**                     - OpenAPI specification
/graphiql                        - GraphQL IDE
```

### Admin-Only Endpoints (Require ROLE_ADMIN)

**Pattern-Based Security** (SecurityConfig.java):
```java
/api/admin/**                    - All admin endpoints
POST /api/users                  - Create users
PUT  /api/users/**               - Update users
DELETE /api/users/**             - Delete users
/api/permissions/**              - Permission management
```

**Method-Based Security** (`@PreAuthorize`):
```java
// Audit Logs
GET /api/v1/audit-logs/**        - @PreAuthorize("hasAuthority('ROLE_ADMIN')")

// User Management
Various user management endpoints
```

### User Endpoints (Require Authentication Only)

**All other `/api/**` endpoints** - Require `isAuthenticated()` but allow any authenticated user

---

## Endpoint Inventory by Category

### 1. Authentication & Authorization

| Endpoint | Method | Security | Purpose |
|----------|--------|----------|---------|
| `/api/authenticate` | POST | Public | Login, get JWT token |
| `/api/register` | POST | Public | Register new user |
| `/api/account/**` | Various | Mixed | Account management |

### 2. Reporting Endpoints

#### Report Catalogs
| Endpoint | Method | Security | Purpose |
|----------|--------|----------|---------|
| `/api/v1/catalogs` | GET | `isAuthenticated()` | List catalogs |
| `/api/v1/catalogs` | POST | `isAuthenticated()` | Create catalog |
| `/api/v1/catalogs/{id}` | GET | `isAuthenticated()` | Get catalog |
| `/api/v1/catalogs/{id}` | PUT | `isAuthenticated()` | Update catalog |
| `/api/v1/catalogs/{id}` | DELETE | `isAuthenticated()` | Delete catalog |

⚠️ **Security Gap**: All users can create/modify/delete any catalog

#### Saved Queries
| Endpoint | Method | Security | Purpose |
|----------|--------|----------|---------|
| `/api/v1/queries` | POST | `isAuthenticated()` | Create query |
| `/api/v1/queries/{id}` | GET | `isAuthenticated()` | Get query |
| `/api/v1/queries/{id}` | PUT | `isAuthenticated()` | Update query |
| `/api/v1/queries/{id}` | DELETE | `isAuthenticated()` | Delete query |
| `/api/v1/queries/{id}/execute` | POST | `isAuthenticated()` | **Execute query** |
| `/api/v1/queries/preview` | POST | `isAuthenticated()` | **Preview SQL** |
| `/api/v1/queries/{id}/favorite` | POST | `isAuthenticated()` | Toggle favorite |

✅ **Has Service-Level Security**: SavedQueryService checks `isPublic` or ownership for execute

⚠️ **Potential Gaps**:
- Any user can read/update/delete any query (no ownership check in controller)
- Preview endpoint allows executing arbitrary SQL (limited to LIMIT 10)

#### Reports
| Endpoint | Method | Security | Purpose |
|----------|--------|----------|---------|
| `/api/v1/reports` | POST | `isAuthenticated()` | Create report |
| `/api/v1/reports/{id}` | GET | `isAuthenticated()` | Get report |
| `/api/v1/reports/{id}` | PUT | `isAuthenticated()` | Update report |
| `/api/v1/reports/{id}` | DELETE | `isAuthenticated()` | Delete report |
| `/api/v1/reports/{id}/run` | POST | `isAuthenticated()` | Execute report |

⚠️ **Security Gap**: All users can create/modify/delete any report

#### Dashboards
| Endpoint | Method | Security | Purpose |
|----------|--------|----------|---------|
| `/api/v1/dashboards` | POST | `isAuthenticated()` | Create dashboard |
| `/api/v1/dashboards/{id}` | GET | `isAuthenticated()` | Get dashboard |
| `/api/v1/dashboards/{id}` | PUT | `isAuthenticated()` | Update dashboard |
| `/api/v1/dashboards/{id}` | DELETE | `isAuthenticated()` | Delete dashboard |
| `/api/v1/dashboards/{id}/widgets` | POST | `isAuthenticated()` | Add widget |

⚠️ **Security Gap**: All users can create/modify/delete any dashboard

#### Schedules
| Endpoint | Method | Security | Purpose |
|----------|--------|----------|---------|
| `/api/v1/schedules` | POST | `isAuthenticated()` | Create schedule |
| `/api/v1/schedules/{id}` | GET | `isAuthenticated()` | Get schedule |
| `/api/v1/schedules/{id}` | PUT | `isAuthenticated()` | Update schedule |
| `/api/v1/schedules/{id}` | DELETE | `isAuthenticated()` | Delete schedule |

⚠️ **Security Gap**: All users can create/modify/delete any schedule

#### Audit Logs
| Endpoint | Method | Security | Purpose |
|----------|--------|----------|---------|
| `/api/v1/audit-logs` | GET | `hasAuthority('ROLE_ADMIN')` | List logs |
| `/api/v1/audit-logs/{id}` | GET | `hasAuthority('ROLE_ADMIN')` | Get log |

✅ **Properly Secured**: Admin-only access

### 3. Metadata Discovery

| Endpoint | Method | Security | Purpose |
|----------|--------|----------|---------|
| `/api/v1/metadata/discover/{connectionId}` | POST | `isAuthenticated()` | Discover metadata |
| `/api/v1/metadata/sync/{connectionId}` | POST | `isAuthenticated()` | Re-sync metadata |
| `/api/v1/metadata/connection/{connectionId}` | DELETE | `hasAuthority('ROLE_ADMIN')` | Delete metadata |
| `/api/v1/metadata/schemas/**` | GET | `isAuthenticated()` | Browse schemas |
| `/api/v1/metadata/tables/**` | GET | `isAuthenticated()` | Browse tables |
| `/api/v1/metadata/columns/**` | GET | `isAuthenticated()` | Browse columns |

⚠️ **Potential Gap**: Any user can discover metadata for any connection

### 4. Database Connections

| Endpoint | Method | Security | Purpose |
|----------|--------|----------|---------|
| `/api/connections` | POST | `isAuthenticated()` | Create connection |
| `/api/connections/{id}` | GET | `isAuthenticated()` | Get connection |
| `/api/connections/{id}` | PUT | `isAuthenticated()` | Update connection |
| `/api/connections/{id}` | DELETE | `isAuthenticated()` | Delete connection |
| `/api/connections/{id}/test` | POST | `isAuthenticated()` | Test connection |

⚠️ **Security Gap**: All users can create/modify/delete any connection (credentials exposed!)

### 5. Query Execution

| Endpoint | Method | Security | Purpose |
|----------|--------|----------|---------|
| `/api/query` | POST | `isAuthenticated()` | Execute query |
| `/api/execute` | POST | `isAuthenticated()` | Execute statement |

⚠️ **Security Gap**: Direct SQL execution without permission checks?

---

## Security Gaps & Recommendations

### 🚨 **Critical Issues**

#### 1. Database Connection Security
**Issue**: Any authenticated user can:
- Create connections with credentials
- View other users' connections (including passwords)
- Delete any connection

**Impact**: HIGH - Credential exposure, unauthorized database access

**Recommendation**:
```java
// Add owner field to DatabaseConnection
private String createdBy;

// In controller, check ownership
if (!connection.getCreatedBy().equals(currentUser) && !isAdmin) {
    throw new AccessDeniedException(...);
}

// Or use @PreAuthorize with SpEL
@PreAuthorize("@connectionSecurityService.canAccess(#id, principal)")
```

#### 2. Arbitrary SQL Execution in Preview
**Issue**: `/api/v1/queries/preview` allows ANY SQL (even if LIMIT 10)

**Impact**: MEDIUM - Can read sensitive data, check schema, enumerate tables

**Recommendation**:
```java
// Add SQL validation
private void validateSql(String sql) {
    String upperSql = sql.toUpperCase();

    // Only allow SELECT
    if (!upperSql.trim().startsWith("SELECT")) {
        throw new IllegalArgumentException("Only SELECT queries allowed");
    }

    // Disallow system tables
    if (upperSql.contains("INFORMATION_SCHEMA") ||
        upperSql.contains("PG_CATALOG")) {
        throw new IllegalArgumentException("System tables not allowed");
    }

    // Check table permissions via AccessControlService
    List<String> tables = extractTablesFromSql(sql);
    for (String table : tables) {
        accessControlService.checkTableAccess(currentUser, connection, table);
    }
}
```

#### 3. Resource Ownership Missing
**Issue**: SavedQuery, Report, Dashboard, Schedule - no ownership checks in controller

**Impact**: MEDIUM - Users can modify/delete each other's resources

**Recommendation**:
```java
// Add to all resource controllers
@PreAuthorize("@resourceSecurityService.canModify(#id, principal)")
public ResponseEntity<DTO> update(@PathVariable Long id, ...)

@PreAuthorize("@resourceSecurityService.canDelete(#id, principal)")
public ResponseEntity<Void> delete(@PathVariable Long id)

// Or check in service layer
if (!resource.getCreatedBy().equals(currentUser) &&
    !resource.getIsPublic() &&
    !isAdmin) {
    throw new AccessDeniedException(...);
}
```

### ⚠️ **Medium Issues**

#### 4. Metadata Discovery Without Permissions
**Issue**: Any user can discover any database's metadata

**Recommendation**:
```java
// Check database connection access first
@PreAuthorize("@connectionSecurityService.canAccess(#connectionId, principal)")
public ResponseEntity<DiscoveryResult> discoverMetadata(@PathVariable Long connectionId)
```

#### 5. Missing Row-Level Security
**Issue**: No filtering of data based on user permissions

**Recommendation**:
```java
// In DatabaseQueryService.fetchSqlPage()
// Before executing SQL, inject row-level filters
String filteredSql = injectRowLevelSecurity(sql, currentUser);
```

### 💡 **Best Practices to Implement**

#### 6. Audit Logging
**Status**: ✅ Partially implemented (AuditLog entity exists)

**Recommendation**: Log all sensitive operations:
```java
@Aspect
public class AuditAspect {
    @After("@annotation(io.oneapi.admin.annotation.Audited)")
    public void logAuditEvent(JoinPoint joinPoint) {
        // Log operation, user, timestamp, IP
    }
}
```

#### 7. Rate Limiting
**Status**: ❌ Not implemented

**Recommendation**: Add rate limiting for:
- Query execution (prevent runaway queries)
- Preview endpoint (prevent abuse)
- Authentication endpoint (prevent brute force)

```java
// Use Bucket4j
@RateLimiter(name = "query-execution", fallbackMethod = "rateLimitFallback")
public QueryResponse executeQuery(...)
```

#### 8. SQL Injection Prevention
**Status**: ✅ Partial - Uses parameter substitution

**Recommendation**:
- Use PreparedStatement for all user input
- Current implementation uses string replacement (could be vulnerable)

```java
// CURRENT (vulnerable)
String sql = "SELECT * FROM users WHERE id = {{userId}}";
sql.replace("{{userId}}", userInput); // If userInput = "1 OR 1=1"

// BETTER
PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
ps.setInt(1, userId);
```

---

## Security Summary

### ✅ **What's Working**

1. **JWT Authentication** - Properly configured
2. **Method-Level Security** - 85 endpoints have `@PreAuthorize`
3. **Admin Endpoints** - Properly restricted to ROLE_ADMIN
4. **Audit Logs** - Admin-only access
5. **Password Encryption** - BCrypt with proper strength
6. **Stateless Sessions** - No session fixation risk
7. **SavedQuery Execution** - Has ownership check in service

### 🚨 **What Needs Fixing**

| Priority | Issue | Endpoints Affected | Impact |
|----------|-------|-------------------|--------|
| **CRITICAL** | Database credentials exposed | `/api/connections/**` | Any user can steal DB credentials |
| **CRITICAL** | No resource ownership | `/api/v1/catalogs/**`, `/api/v1/reports/**`, `/api/v1/dashboards/**`, `/api/v1/schedules/**` | Users can delete each other's work |
| **HIGH** | Arbitrary SQL in preview | `/api/v1/queries/preview` | Can read sensitive data |
| **HIGH** | No connection access control | `/api/v1/metadata/discover/**` | Can discover any database |
| **MEDIUM** | SQL injection in parameters | Parameter substitution | Vulnerable to SQL injection |
| **MEDIUM** | No rate limiting | All query endpoints | DoS vulnerability |
| **LOW** | Missing row-level security | All data endpoints | Can access unauthorized rows |

---

## Recommended Implementation Order

### Phase 1: Critical Fixes (Immediate)
1. **Add resource ownership checks** to all CRUD operations
2. **Encrypt database credentials** in DatabaseConnection
3. **Add SQL validation** to preview endpoint
4. **Add connection access control** to metadata discovery

### Phase 2: High Priority (This Week)
1. **Implement PreparedStatement** for SQL execution
2. **Add rate limiting** to query endpoints
3. **Enhance audit logging** for all sensitive operations

### Phase 3: Medium Priority (This Sprint)
1. **Implement row-level security** framework
2. **Add fine-grained permission checks** before SQL execution
3. **Add IP whitelisting** for admin endpoints

---

## Example: Secure Resource Access Pattern

```java
@Service
public class ResourceSecurityService {

    public boolean canModify(Long resourceId, String resourceType, String userId) {
        // Get resource
        Resource resource = getResource(resourceId, resourceType);

        // Check ownership
        if (resource.getCreatedBy().equals(userId)) {
            return true;
        }

        // Check if user is admin
        if (SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ADMIN)) {
            return true;
        }

        // Check if resource is shared and user has edit permission
        if (resource.isShared() && hasPermission(userId, resourceId, "EDIT")) {
            return true;
        }

        return false;
    }
}

@RestController
public class ReportController {

    @PreAuthorize("@resourceSecurityService.canModify(#id, 'Report', principal.name)")
    @PutMapping("/{id}")
    public ResponseEntity<ReportDTO> update(@PathVariable Long id, ...) {
        // User is authorized by @PreAuthorize
        ...
    }
}
```

---

## Conclusion

**Current Security Posture**: ⚠️ **Moderate Risk**

- ✅ Good foundation with JWT and RBAC
- ✅ Method-level security annotations in place
- 🚨 Critical gaps in resource ownership and credential security
- ⚠️ Missing fine-grained access control for data operations

**Priority**: Fix critical issues (resource ownership, credential encryption) before production deployment.
