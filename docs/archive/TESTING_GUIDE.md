# OneAPI Platform - Testing Guide

## Database Configuration

### Admin App (Port 8090)
- **Database**: H2 (Embedded File-based)
- **Location**: `./data/oneapi-admin.mv.db`
- **Console**: http://localhost:8090/h2-console
  - **JDBC URL**: `jdbc:h2:file:./data/oneapi-admin`
  - **Username**: `sa`
  - **Password**: (empty)

### Security App
- **Database**: H2 (check configuration in security app resources)

## How to Test

### 1. Start the Admin Application

```bash
cd /Users/ravi/JavaProjects/oneapi-platform/oneapi-admin-app

# Build and run
mvn spring-boot:run

# Or using the JAR
java -jar target/oneapi-admin-app-0.0.1.jar
```

**Application will start on**: http://localhost:8090

### 2. Access API Documentation

Once the app is running, access:

- **Swagger UI**: http://localhost:8090/swagger-ui.html
- **API Docs (JSON)**: http://localhost:8090/api-docs
- **GraphQL Playground**: http://localhost:8090/graphiql
- **H2 Database Console**: http://localhost:8090/h2-console

### 3. Test Endpoints

#### Using Swagger UI (Easiest)
1. Open http://localhost:8090/swagger-ui.html
2. Browse available endpoints
3. Click "Try it out" on any endpoint
4. Fill in parameters
5. Click "Execute"

#### Using Postman
Import the `OneAPI-Platform-Postman-Collection.json` file (created below)

#### Using curl

**Example: Create API Client**
```bash
curl -X POST http://localhost:8090/api/clients \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user123" \
  -d '{
    "name": "My Test App",
    "description": "Test application",
    "scopes": ["read", "write"],
    "rateLimit": 1000
  }'
```

**Example: List Clients**
```bash
curl -X GET http://localhost:8090/api/clients \
  -H "X-User-Id: user123"
```

**Example: Test Rate Limiting**
```bash
curl -X GET http://localhost:8090/api/throttle/check/user123
```

**Example: Create Permission**
```bash
curl -X POST http://localhost:8090/api/access-control/permissions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "database.read",
    "resource": "database",
    "action": "read",
    "description": "Read access to databases"
  }'
```

### 4. Test GraphQL Endpoints

Open http://localhost:8090/graphiql and try:

**Query: List Clients**
```graphql
query {
  clients(userId: "user123") {
    id
    clientId
    name
    active
    scopes
    rateLimit
    createdAt
  }
}
```

**Mutation: Create Client**
```graphql
mutation {
  createClient(
    userId: "user123"
    name: "GraphQL Test Client"
    description: "Created via GraphQL"
    scopes: ["read", "write"]
    rateLimit: 500
  ) {
    clientId
    clientSecret
    message
  }
}
```

**Query: Validate Client**
```graphql
query {
  validateClient(
    clientId: "client_xyz..."
    clientSecret: "sk_abc..."
  )
}
```

### 5. Test Database Connections

**Create a Database Connection**
```bash
curl -X POST http://localhost:8090/api/database-connections \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test PostgreSQL",
    "type": "POSTGRESQL",
    "host": "localhost",
    "port": 5432,
    "database": "testdb",
    "username": "postgres",
    "password": "password"
  }'
```

### 6. Generate Seed Data

**Generate 10 Sample Connections**
```bash
curl -X POST "http://localhost:8090/api/seed-data/connections?count=10" \
  -H "X-User-Id: user123"
```

**Cleanup Test Data**
```bash
curl -X DELETE http://localhost:8090/api/seed-data/cleanup \
  -H "X-User-Id: user123"
```

## Testing Workflow Examples

### Complete Client Service Test

```bash
# 1. Create a client
CLIENT_RESPONSE=$(curl -s -X POST http://localhost:8090/api/clients \
  -H "Content-Type: application/json" \
  -H "X-User-Id: testuser" \
  -d '{
    "name": "Test Client",
    "description": "Testing the API",
    "scopes": ["read", "write"],
    "rateLimit": 100
  }')

echo "Created: $CLIENT_RESPONSE"

# Extract clientId and clientSecret
CLIENT_ID=$(echo $CLIENT_RESPONSE | jq -r '.clientId')
CLIENT_SECRET=$(echo $CLIENT_RESPONSE | jq -r '.clientSecret')

# 2. List all clients
curl -X GET http://localhost:8090/api/clients \
  -H "X-User-Id: testuser"

# 3. Get specific client
curl -X GET http://localhost:8090/api/clients/$CLIENT_ID \
  -H "X-User-Id: testuser"

# 4. Validate credentials
curl -X POST http://localhost:8090/api/clients/validate \
  -H "Content-Type: application/json" \
  -d "{
    \"clientId\": \"$CLIENT_ID\",
    \"clientSecret\": \"$CLIENT_SECRET\"
  }"

# 5. Deactivate client
curl -X PATCH http://localhost:8090/api/clients/$CLIENT_ID/status \
  -H "Content-Type: application/json" \
  -H "X-User-Id: testuser" \
  -d '{"active": false}'

# 6. Delete client
curl -X DELETE http://localhost:8090/api/clients/$CLIENT_ID \
  -H "X-User-Id: testuser"
```

### Complete Access Control Test

```bash
# 1. Create permissions
curl -X POST http://localhost:8090/api/access-control/permissions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "database.read",
    "resource": "database",
    "action": "read",
    "description": "Read databases"
  }'

curl -X POST http://localhost:8090/api/access-control/permissions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "database.write",
    "resource": "database",
    "action": "write",
    "description": "Write to databases"
  }'

# 2. Create role with permissions
curl -X POST http://localhost:8090/api/access-control/roles \
  -H "Content-Type: application/json" \
  -d '{
    "name": "viewer",
    "description": "Read-only access",
    "permissions": ["database.read"]
  }'

# 3. Assign role to user
curl -X POST http://localhost:8090/api/access-control/users/alice/roles \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin" \
  -d '{
    "roleName": "viewer"
  }'

# 4. Check user permissions
curl -X GET http://localhost:8090/api/access-control/users/alice/permissions

# 5. Check specific permission
curl -X GET "http://localhost:8090/api/access-control/users/alice/check?resource=database&action=read"
```

### Test Rate Limiting

```bash
# Set custom rate limit
curl -X POST http://localhost:8090/api/throttle/limits/user123 \
  -H "Content-Type: application/json" \
  -d '{"requestsPerMinute": 10}'

# Make requests and check status
for i in {1..15}; do
  echo "Request $i:"
  curl -X GET http://localhost:8090/api/throttle/check/user123
  echo ""
done
```

## Expected Behavior

### Client Service
- ✅ Client IDs start with `client_`
- ✅ Client secrets start with `sk_`
- ✅ Secrets are hashed and never returned after creation
- ✅ Only client owner can view/modify/delete

### Access Control
- ✅ Permissions are resource + action pairs
- ✅ Roles group permissions
- ✅ Users inherit permissions from roles
- ✅ Role assignments can have expiration dates

### Throttle Service
- ✅ Default: 60 requests/minute per user
- ✅ Sliding window (1-minute expiration)
- ✅ Custom limits per user supported
- ✅ Returns allowed/remaining count

### Seed Data
- ✅ Generates realistic test data
- ✅ Auto-generates unique names with timestamps
- ✅ Creates connections for all database types

## Troubleshooting

### Port Already in Use
```bash
# Find process on port 8090
lsof -i :8090

# Kill the process
kill -9 <PID>
```

### Database Locked
```bash
# Stop the app and delete the lock file
rm ./data/oneapi-admin.lock.db
```

### Clear All Data
```bash
# Stop the app and delete database files
rm -rf ./data/
```

### Enable Debug Logging
Add to `application.yml`:
```yaml
logging:
  level:
    io.oneapi: DEBUG
    org.springframework.web: DEBUG
```

## Next Steps

1. Import Postman collection (see OneAPI-Platform-Postman-Collection.json)
2. Start the application
3. Open Swagger UI
4. Test each endpoint
5. Check H2 console to see data
6. Test GraphQL queries

## Security Notes

⚠️ **Development Mode**: This configuration is for development only!

- H2 console is enabled (disable in production)
- No authentication configured (add Spring Security)
- Debug logging enabled
- Auto-create database schema
- In-memory rate limiting (use Redis in production)
