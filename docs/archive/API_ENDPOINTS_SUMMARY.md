# OneAPI Platform - API Endpoints Summary

## 📋 Complete Endpoint List

### 1️⃣ Client Service - API Client Management

| Method | Endpoint | Description | Headers |
|--------|----------|-------------|---------|
| POST | `/api/clients` | Create new API client | `X-User-Id`, `Content-Type: application/json` |
| GET | `/api/clients` | List all clients for user | `X-User-Id` |
| GET | `/api/clients/{clientId}` | Get client details | `X-User-Id` |
| PATCH | `/api/clients/{clientId}/status` | Update client status (activate/deactivate) | `X-User-Id`, `Content-Type: application/json` |
| DELETE | `/api/clients/{clientId}` | Delete client | `X-User-Id` |
| POST | `/api/clients/validate` | Validate client credentials | `Content-Type: application/json` |

**Request/Response Examples:**

```bash
# Create Client
POST /api/clients
{
  "name": "My App",
  "description": "Description",
  "scopes": ["read", "write"],
  "rateLimit": 1000
}

# Response
{
  "clientId": "client_abcd1234...",
  "clientSecret": "sk_xyz789...",
  "message": "Save these credentials securely..."
}
```

---

### 2️⃣ Access Control Service - RBAC & Permissions

#### Permissions

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/access-control/permissions` | Create permission |
| GET | `/api/access-control/permissions` | List all permissions |

#### Roles

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/access-control/roles` | Create role with permissions |
| GET | `/api/access-control/roles` | List all roles |
| POST | `/api/access-control/roles/{roleName}/permissions` | Add permissions to role |

#### User Role Assignments

| Method | Endpoint | Description | Headers |
|--------|----------|-------------|---------|
| POST | `/api/access-control/users/{userId}/roles` | Assign role to user | `X-User-Id` (granter) |
| GET | `/api/access-control/users/{userId}/roles` | Get user's roles | - |
| GET | `/api/access-control/users/{userId}/permissions` | Get user's permissions | - |
| GET | `/api/access-control/users/{userId}/check` | Check if user has permission | Query: `resource`, `action` |
| DELETE | `/api/access-control/users/{userId}/roles/{roleName}` | Remove role from user | - |

**Request/Response Examples:**

```bash
# Create Permission
POST /api/access-control/permissions
{
  "name": "database.read",
  "resource": "database",
  "action": "read",
  "description": "Read access to databases"
}

# Create Role
POST /api/access-control/roles
{
  "name": "viewer",
  "description": "Read-only access",
  "permissions": ["database.read"]
}

# Assign Role
POST /api/access-control/users/alice/roles
{
  "roleName": "viewer",
  "expiresAt": "2026-12-31T23:59:59"  // Optional
}

# Check Permission
GET /api/access-control/users/alice/check?resource=database&action=read
{
  "hasPermission": true
}
```

---

### 3️⃣ Throttle Service - Rate Limiting

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/throttle/check/{userId}` | Check if request allowed & get remaining quota |
| GET | `/api/throttle/status/{userId}` | Get current rate limit status |
| POST | `/api/throttle/limits/{userId}` | Set custom rate limit for user |
| DELETE | `/api/throttle/limits/{userId}` | Reset rate limit to default |

**Request/Response Examples:**

```bash
# Check Rate Limit
GET /api/throttle/check/user123
{
  "allowed": true,
  "remaining": 45,
  "userId": "user123"
}

# Set Custom Limit
POST /api/throttle/limits/user123
{
  "requestsPerMinute": 10
}

# Get Status
GET /api/throttle/status/user123
{
  "current": 5,
  "remaining": 55
}
```

---

### 4️⃣ Seed Data Service - Test Data Management

| Method | Endpoint | Description | Headers |
|--------|----------|-------------|---------|
| POST | `/api/seed-data/connections?count={n}` | Generate N sample database connections | `X-User-Id` |
| POST | `/api/seed-data/query-data?rows={n}` | Generate sample query data | - |
| DELETE | `/api/seed-data/cleanup` | Cleanup test data | `X-User-Id` |

**Request/Response Examples:**

```bash
# Generate Connections
POST /api/seed-data/connections?count=10
{
  "requested": 10,
  "created": 10
}

# Generate Query Data
POST /api/seed-data/query-data?rows=100
{
  "rows": 100,
  "status": "generated"
}
```

---

### 5️⃣ Database Connections (Existing)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/database-connections` | Create database connection |
| GET | `/api/database-connections` | List all connections |
| GET | `/api/database-connections/{id}` | Get connection details |
| PUT | `/api/database-connections/{id}` | Update connection |
| DELETE | `/api/database-connections/{id}` | Delete connection |
| POST | `/api/database-connections/{id}/test` | Test connection |

---

### 6️⃣ Query Service (Existing)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/query` | Execute query |
| GET | `/api/query/sessions` | List query sessions |
| DELETE | `/api/query/sessions/{sessionKey}` | Close query session |

---

### 7️⃣ User Preferences (Existing)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/user-preferences` | Get user preferences |
| PUT | `/api/user-preferences` | Update user preferences |

---

## 🔌 GraphQL Endpoints

**Base URL**: `http://localhost:8090/graphql`
**Playground**: `http://localhost:8090/graphiql`

### Client Service Queries & Mutations

```graphql
# Queries
query {
  clients(userId: "user123") { id, clientId, name, active, scopes }
  client(clientId: "client_xyz", userId: "user123") { id, name, rateLimit }
  validateClient(clientId: "...", clientSecret: "...") # Returns Boolean
}

# Mutations
mutation {
  createClient(
    userId: "user123"
    name: "My App"
    description: "Description"
    scopes: ["read", "write"]
    rateLimit: 1000
  ) {
    clientId
    clientSecret
    message
  }

  updateClientStatus(
    clientId: "client_xyz"
    userId: "user123"
    active: false
  ) # Returns Boolean

  deleteClient(
    clientId: "client_xyz"
    userId: "user123"
  ) # Returns Boolean
}
```

---

## 📊 Response Codes

| Code | Description | When |
|------|-------------|------|
| 200 | OK | Successful GET, PATCH, DELETE |
| 201 | Created | Successful POST (resource created) |
| 204 | No Content | Successful DELETE/PATCH with no body |
| 400 | Bad Request | Invalid input data |
| 401 | Unauthorized | Missing/invalid authentication |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate resource (e.g., client already exists) |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |

---

## 🔑 Authentication Headers

Most endpoints require authentication via header:

```
X-User-Id: <userId>
```

**Note**: This is a simplified auth for development. In production, use proper JWT/OAuth tokens.

---

## 🎯 Quick Reference - Common Use Cases

### Use Case 1: Setup New API Client
1. `POST /api/clients` - Create client
2. Save `clientId` and `clientSecret`
3. `POST /api/clients/validate` - Test credentials

### Use Case 2: Grant User Permissions
1. `POST /api/access-control/permissions` - Create permission
2. `POST /api/access-control/roles` - Create role with permissions
3. `POST /api/access-control/users/{userId}/roles` - Assign role
4. `GET /api/access-control/users/{userId}/check` - Verify access

### Use Case 3: Rate Limit a User
1. `POST /api/throttle/limits/{userId}` - Set limit
2. `GET /api/throttle/check/{userId}` - Test limit
3. Make multiple requests - see throttling in action

### Use Case 4: Generate Test Data
1. `POST /api/seed-data/connections?count=20` - Create connections
2. View in H2 console
3. `DELETE /api/seed-data/cleanup` - Clean up

---

## 🛠️ API Documentation Access

| Type | URL |
|------|-----|
| Swagger UI | http://localhost:8090/swagger-ui.html |
| OpenAPI JSON | http://localhost:8090/api-docs |
| GraphQL Playground | http://localhost:8090/graphiql |

---

## 📦 Postman Collection

Import `OneAPI-Platform-Postman-Collection.json` for ready-to-use requests with:
- ✅ Pre-configured environments
- ✅ Auto-saving of clientId/clientSecret
- ✅ Request examples for all endpoints
- ✅ Tests for validation
- ✅ Collection variables

---

## 🔍 Filtering & Pagination

For endpoints that return lists, you can use query parameters:

```bash
# Example (implementation dependent)
GET /api/clients?page=0&size=20&sort=createdAt,desc
```

---

## 💡 Tips

1. **Test in Swagger First** - Easiest way to explore
2. **Use Postman Collection** - For automated testing
3. **Check H2 Console** - See data changes in real-time
4. **Try GraphQL** - More flexible queries
5. **Monitor Throttle** - Watch rate limiting in action
6. **Generate Seed Data** - Quick test data creation

---

## ⚡ Performance Notes

- **Throttle Service**: In-memory cache (Caffeine) - fast!
- **Client Validation**: SHA-256 hashing - secure but performant
- **Access Control**: Eager loading of permissions - optimized for checks
- **Rate Limits**: Sliding window with auto-expiration

---

## 🔐 Security Considerations

- Client secrets are **hashed** (SHA-256) before storage
- Secrets shown **only once** at creation
- User ownership validated for client operations
- Role assignments tracked with **audit trail** (who granted)
- Rate limiting prevents **abuse**

---

For detailed testing instructions, see:
- **TESTING_GUIDE.md** - Comprehensive testing guide
- **QUICK_START.md** - Quick start instructions
