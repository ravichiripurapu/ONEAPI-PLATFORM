# Security Implementation & Testing Plan

## Security Requirements (Clarified)

### Admin Capabilities
1. ✅ Create/Update/Delete **Catalogs**
2. ✅ User management
3. ✅ Permission management
4. ✅ Audit log viewing
5. ✅ **Assign catalogs to users** with table/view/column permissions
6. ✅ **Metadata discovery** (admin-only)

### User Capabilities
1. ✅ View **their assigned catalogs only**
2. ✅ View **their permitted tables/views/columns only**
3. ✅ Manage **their own queries** (create/update/delete/execute)
4. ✅ Run queries **only on permitted data**
5. ✅ Run reports **based on their queries**
6. ✅ Manage **their own dashboards**
7. ✅ Manage **their own schedules**

---

## Implementation Tasks

### Phase 1: Catalog Security (High Priority)

#### 1.1 Update ReportCatalogController
**File**: `ReportCatalogController.java`

```java
@PostMapping
@PreAuthorize("hasAuthority('ROLE_ADMIN')") // Changed from isAuthenticated()
public ResponseEntity<ReportCatalogDTO> create(...)

@PutMapping("/{id}")
@PreAuthorize("hasAuthority('ROLE_ADMIN')") // Changed from isAuthenticated()
public ResponseEntity<ReportCatalogDTO> update(...)

@DeleteMapping("/{id}")
@PreAuthorize("hasAuthority('ROLE_ADMIN')") // Changed from isAuthenticated()
public ResponseEntity<Void> delete(...)

@GetMapping  // Keep isAuthenticated() - users can list their assigned catalogs
@PreAuthorize("isAuthenticated()")
public ResponseEntity<List<ReportCatalogDTO>> getAll(Principal principal) {
    // If admin: return all catalogs
    // If user: return only assigned catalogs
}
```

#### 1.2 Update ReportCatalogService
**File**: `ReportCatalogService.java`

```java
// Add method to get user's assigned catalogs
public List<ReportCatalogDTO> findAssignedCatalogs(String userId) {
    // Query user_catalog_assignment table
    return catalogRepository.findAssignedToUser(userId);
}
```

#### 1.3 Create Catalog Assignment Entities
**New Entity**: `UserCatalogAssignment.java`

```java
@Entity
@Table(name = "user_catalog_assignment")
public class UserCatalogAssignment {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Catalog catalog;

    private LocalDateTime assignedAt;
    private String assignedBy; // admin who assigned
}
```

#### 1.4 Create Assignment Controller
**New Controller**: `CatalogAssignmentController.java`

```java
@RestController
@RequestMapping("/api/v1/admin/catalog-assignments")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class CatalogAssignmentController {

    @PostMapping
    public ResponseEntity<Void> assignCatalogToUser(
        @RequestParam Long userId,
        @RequestParam Long catalogId) {
        // Assign catalog to user
    }

    @DeleteMapping
    public ResponseEntity<Void> revokeCatalogFromUser(
        @RequestParam Long userId,
        @RequestParam Long catalogId) {
        // Revoke catalog from user
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CatalogDTO>> getUserCatalogs(@PathVariable Long userId) {
        // Get user's assigned catalogs
    }
}
```

---

### Phase 2: Metadata Discovery Security

#### 2.1 Update MetadataDiscoveryController
**File**: `MetadataDiscoveryController.java`

```java
@PostMapping("/discover/{connectionId}")
@PreAuthorize("hasAuthority('ROLE_ADMIN')") // Changed from isAuthenticated()
public ResponseEntity<DiscoveryResult> discoverMetadata(...)

@PostMapping("/sync/{connectionId}")
@PreAuthorize("hasAuthority('ROLE_ADMIN')") // Changed from isAuthenticated()
public ResponseEntity<DiscoveryResult> syncMetadata(...)

@DeleteMapping("/connection/{connectionId}")
@PreAuthorize("hasAuthority('ROLE_ADMIN')") // Already correct
public ResponseEntity<Void> deleteMetadata(...)
```

---

### Phase 3: User Data Filtering

#### 3.1 Filter Catalogs for Users
**Service Layer** - ReportCatalogService:

```java
public List<ReportCatalogDTO> findAll(String currentUser, boolean isAdmin) {
    if (isAdmin) {
        return findAll(); // All catalogs
    } else {
        return findAssignedCatalogs(currentUser); // Only assigned
    }
}
```

#### 3.2 Filter Metadata for Users
**Service Layer** - MetadataDiscoveryService:

```java
public List<SchemaMetadataDTO> getSchemas(String currentUser, boolean isAdmin) {
    if (isAdmin) {
        return getAllSchemas(); // All schemas
    } else {
        // Get schemas from user's assigned catalogs
        List<Long> assignedCatalogIds = getUserAssignedCatalogIds(currentUser);
        return getSchemasByCatalogIds(assignedCatalogIds);
    }
}
```

---

### Phase 4: Query Execution Permission Checks

#### 4.1 Enhance SavedQueryService.executeSavedQuery()

```java
public QueryResponse executeSavedQuery(...) {
    SavedQuery query = findById(savedQueryId);

    // Check ownership or public
    if (!query.getIsPublic() && !query.getCreatedBy().equals(userId)) {
        throw new AccessDeniedException("Not authorized");
    }

    // NEW: Check table-level permissions
    List<String> tables = extractTablesFromSql(query.getQueryText());
    for (String table : tables) {
        if (!hasTableAccess(userId, query.getConnection(), table)) {
            throw new AccessDeniedException("No access to table: " + table);
        }
    }

    // Execute query...
}
```

---

## Test Data Creation

### Liquibase Migration: Test Data
**File**: `06-insert-test-data.xml`

```xml
<changeSet id="06-create-test-users" author="test">
    <!-- Admin user -->
    <insert tableName="app_user">
        <column name="login" value="admin"/>
        <column name="password_hash" value="$2a$10$..."/> <!-- admin123 -->
        <column name="first_name" value="Admin"/>
        <column name="last_name" value="User"/>
        <column name="email" value="admin@oneapi.io"/>
        <column name="activated" valueBoolean="true"/>
    </insert>

    <!-- Regular user 1 -->
    <insert tableName="app_user">
        <column name="login" value="john"/>
        <column name="password_hash" value="$2a$10$..."/> <!-- john123 -->
        <column name="first_name" value="John"/>
        <column name="last_name" value="Doe"/>
        <column name="email" value="john@oneapi.io"/>
        <column name="activated" valueBoolean="true"/>
    </insert>

    <!-- Regular user 2 -->
    <insert tableName="app_user">
        <column name="login" value="jane"/>
        <column name="password_hash" value="$2a$10$..."/> <!-- jane123 -->
        <column name="first_name" value="Jane"/>
        <column name="last_name" value="Smith"/>
        <column name="email" value="jane@oneapi.io"/>
        <column name="activated" valueBoolean="true"/>
    </insert>
</changeSet>

<changeSet id="06-assign-roles" author="test">
    <!-- Admin role -->
    <insert tableName="user_authority">
        <column name="user_id" valueComputed="(SELECT id FROM app_user WHERE login='admin')"/>
        <column name="authority_name" value="ROLE_ADMIN"/>
    </insert>
    <insert tableName="user_authority">
        <column name="user_id" valueComputed="(SELECT id FROM app_user WHERE login='admin')"/>
        <column name="authority_name" value="ROLE_USER"/>
    </insert>

    <!-- User roles -->
    <insert tableName="user_authority">
        <column name="user_id" valueComputed="(SELECT id FROM app_user WHERE login='john')"/>
        <column name="authority_name" value="ROLE_USER"/>
    </insert>
    <insert tableName="user_authority">
        <column name="user_id" valueComputed="(SELECT id FROM app_user WHERE login='jane')"/>
        <column name="authority_name" value="ROLE_USER"/>
    </insert>
</changeSet>

<changeSet id="06-create-test-catalogs" author="test">
    <insert tableName="catalog">
        <column name="name" value="Sales Catalog"/>
        <column name="description" value="Sales and revenue data"/>
        <column name="created_by" value="admin"/>
    </insert>

    <insert tableName="catalog">
        <column name="name" value="HR Catalog"/>
        <column name="description" value="Human resources data"/>
        <column name="created_by" value="admin"/>
    </insert>
</changeSet>

<changeSet id="06-create-test-connections" author="test">
    <insert tableName="database_connections">
        <column name="name" value="Local H2"/>
        <column name="type" value="H2"/>
        <column name="host" value="localhost"/>
        <column name="port" valueNumeric="0"/>
        <column name="database" value="oneapi"/>
        <column name="username" value="sa"/>
        <column name="password" value=""/>
        <column name="active" valueBoolean="true"/>
    </insert>
</changeSet>

<changeSet id="06-assign-catalogs-to-users" author="test">
    <!-- John gets Sales Catalog -->
    <insert tableName="user_catalog_assignment">
        <column name="user_id" valueComputed="(SELECT id FROM app_user WHERE login='john')"/>
        <column name="catalog_id" valueComputed="(SELECT id FROM catalog WHERE name='Sales Catalog')"/>
        <column name="assigned_by" value="admin"/>
    </insert>

    <!-- Jane gets HR Catalog -->
    <insert tableName="user_catalog_assignment">
        <column name="user_id" valueComputed="(SELECT id FROM app_user WHERE login='jane')"/>
        <column name="catalog_id" valueComputed="(SELECT id FROM catalog WHERE name='HR Catalog')"/>
        <column name="assigned_by" value="admin"/>
    </insert>
</changeSet>

<changeSet id="06-create-sample-queries" author="test">
    <!-- John's query -->
    <insert tableName="saved_query">
        <column name="name" value="My Sales Query"/>
        <column name="query_text" value="SELECT * FROM orders WHERE year = {{year}}"/>
        <column name="connection_id" valueComputed="(SELECT id FROM database_connections WHERE name='Local H2')"/>
        <column name="catalog_id" valueComputed="(SELECT id FROM catalog WHERE name='Sales Catalog')"/>
        <column name="is_public" valueBoolean="false"/>
        <column name="created_by" value="john"/>
    </insert>

    <!-- Public query -->
    <insert tableName="saved_query">
        <column name="name" value="Public Sales Summary"/>
        <column name="query_text" value="SELECT COUNT(*) as total FROM orders"/>
        <column name="connection_id" valueComputed="(SELECT id FROM database_connections WHERE name='Local H2')"/>
        <column name="catalog_id" valueComputed="(SELECT id FROM catalog WHERE name='Sales Catalog')"/>
        <column name="is_public" valueBoolean="true"/>
        <column name="created_by" value="admin"/>
    </insert>
</changeSet>
```

---

## Postman Collection Structure

```json
{
  "info": {
    "name": "OneAPI Platform",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080",
      "type": "string"
    },
    {
      "key": "adminToken",
      "value": "",
      "type": "string"
    },
    {
      "key": "johnToken",
      "value": "",
      "type": "string"
    },
    {
      "key": "janeToken",
      "value": "",
      "type": "string"
    }
  ],
  "item": [
    {
      "name": "1. Authentication",
      "item": [
        {
          "name": "Login as Admin",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/authenticate",
            "body": {
              "username": "admin",
              "password": "admin123"
            }
          },
          "event": [{
            "listen": "test",
            "script": "pm.collectionVariables.set('adminToken', pm.response.json().id_token);"
          }]
        },
        {
          "name": "Login as John",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/authenticate",
            "body": {
              "username": "john",
              "password": "john123"
            }
          },
          "event": [{
            "listen": "test",
            "script": "pm.collectionVariables.set('johnToken', pm.response.json().id_token);"
          }]
        },
        {
          "name": "Login as Jane",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/authenticate",
            "body": {
              "username": "jane",
              "password": "jane123"
            }
          },
          "event": [{
            "listen": "test",
            "script": "pm.collectionVariables.set('janeToken', pm.response.json().id_token);"
          }]
        }
      ]
    },
    {
      "name": "2. Admin - Catalog Management",
      "item": [
        {
          "name": "Create Catalog (Admin)",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/v1/catalogs",
            "header": [{
              "key": "Authorization",
              "value": "Bearer {{adminToken}}"
            }],
            "body": {
              "name": "New Catalog",
              "description": "Test catalog"
            }
          }
        },
        {
          "name": "Create Catalog (User - Should Fail)",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/v1/catalogs",
            "header": [{
              "key": "Authorization",
              "value": "Bearer {{johnToken}}"
            }],
            "body": {
              "name": "Unauthorized",
              "description": "Should fail"
            }
          },
          "event": [{
            "listen": "test",
            "script": "pm.test('Status is 403', () => pm.response.to.have.status(403));"
          }]
        }
      ]
    },
    {
      "name": "3. User - View Assigned Catalogs",
      "item": [
        {
          "name": "John - Get My Catalogs",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/api/v1/catalogs",
            "header": [{
              "key": "Authorization",
              "value": "Bearer {{johnToken}}"
            }]
          },
          "event": [{
            "listen": "test",
            "script": "pm.test('Returns only Sales Catalog', () => pm.expect(pm.response.json().length).to.eql(1));"
          }]
        }
      ]
    },
    {
      "name": "4. Query Execution",
      "item": [
        {
          "name": "John - Execute Own Query",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/v1/queries/1/execute",
            "header": [{
              "key": "Authorization",
              "value": "Bearer {{johnToken}}"
            }],
            "body": {
              "year": 2024
            }
          }
        },
        {
          "name": "Jane - Execute John's Query (Should Fail)",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/v1/queries/1/execute",
            "header": [{
              "key": "Authorization",
              "value": "Bearer {{janeToken}}"
            }]
          },
          "event": [{
            "listen": "test",
            "script": "pm.test('Status is 403', () => pm.response.to.have.status(403));"
          }]
        },
        {
          "name": "Anyone - Execute Public Query",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/v1/queries/2/execute",
            "header": [{
              "key": "Authorization",
              "value": "Bearer {{janeToken}}"
            }]
          }
        }
      ]
    }
  ]
}
```

---

## Testing Scenarios

### Test 1: Admin Catalog Management
1. ✅ Admin can create catalog → 201 Created
2. ✅ User cannot create catalog → 403 Forbidden
3. ✅ Admin can update catalog → 200 OK
4. ✅ User cannot update catalog → 403 Forbidden
5. ✅ Admin can delete catalog → 204 No Content
6. ✅ User cannot delete catalog → 403 Forbidden

### Test 2: Catalog Assignment
1. ✅ Admin assigns Sales Catalog to John
2. ✅ Admin assigns HR Catalog to Jane
3. ✅ John lists catalogs → sees only Sales Catalog
4. ✅ Jane lists catalogs → sees only HR Catalog
5. ✅ Admin lists catalogs → sees all catalogs

### Test 3: Metadata Discovery
1. ✅ Admin can discover metadata → 200 OK
2. ✅ User cannot discover metadata → 403 Forbidden
3. ✅ User can view tables in assigned catalog → 200 OK
4. ✅ User cannot view tables in unassigned catalog → 403 Forbidden

### Test 4: Query Execution
1. ✅ User can execute own query → 200 OK
2. ✅ User cannot execute other's private query → 403 Forbidden
3. ✅ User can execute public query → 200 OK
4. ✅ User can execute query with parameters → 200 OK
5. ✅ User cannot execute query on unauthorized table → 403 Forbidden

### Test 5: Query Preview
1. ✅ User can preview query on permitted table → 200 OK
2. ✅ User cannot preview query on forbidden table → 403 Forbidden

---

## Implementation Priority

### Immediate (Critical for Testing):
1. ✅ Update ReportCatalogController security annotations
2. ✅ Update MetadataDiscoveryController security annotations
3. ✅ Create test data migration (06-insert-test-data.xml)
4. ✅ Create Postman collection

### Short-Term (This Week):
1. ✅ Implement UserCatalogAssignment entity and repository
2. ✅ Implement CatalogAssignmentController
3. ✅ Filter catalogs by user assignments
4. ✅ Filter metadata by catalog assignments

### Medium-Term (This Sprint):
1. ✅ Implement table-level permission checks in query execution
2. ✅ Implement column-level filtering in query results
3. ✅ Add SQL validation in preview endpoint
4. ✅ Enhance audit logging

---

## Files to Create/Modify

### Create (New Files):
1. `UserCatalogAssignment.java` - Entity
2. `UserCatalogAssignmentRepository.java` - Repository
3. `CatalogAssignmentService.java` - Service
4. `CatalogAssignmentController.java` - Controller
5. `06-insert-test-data.xml` - Liquibase test data
6. `OneAPI_Platform.postman_collection.json` - Postman collection

### Modify (Existing Files):
1. `ReportCatalogController.java` - Change @PreAuthorize annotations
2. `MetadataDiscoveryController.java` - Change @PreAuthorize annotations
3. `ReportCatalogService.java` - Add findAssignedCatalogs()
4. `SavedQueryService.java` - Add table permission checks
5. `SecurityConfig.java` - Update URL patterns if needed

---

## Next Steps

**Immediate Action**: I'll start implementing these security fixes now, beginning with:
1. Update controller security annotations
2. Create test data
3. Create Postman collection
4. Test endpoints with different users

Total estimated time: 4-6 hours
