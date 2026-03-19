# OneAPI Platform - Quick Start Guide

## 🚀 Start the Application

### Method 1: Using the Start Script (Easiest)
```bash
cd /Users/ravi/JavaProjects/oneapi-platform
./start-admin-app.sh
```

### Method 2: Manual Start
```bash
cd /Users/ravi/JavaProjects/oneapi-platform/oneapi-admin-app
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
mvn spring-boot:run
```

### Method 3: Using JAR
```bash
cd /Users/ravi/JavaProjects/oneapi-platform
mvn clean package -DskipTests
java -jar oneapi-admin-app/target/oneapi-admin-app-0.0.1.jar
```

## 📍 Access Points

Once running, access these URLs:

| Service | URL | Description |
|---------|-----|-------------|
| **Swagger UI** | http://localhost:8090/swagger-ui.html | Interactive API documentation |
| **GraphQL Playground** | http://localhost:8090/graphiql | GraphQL query interface |
| **H2 Database Console** | http://localhost:8090/h2-console | View database contents |
| **API Docs (JSON)** | http://localhost:8090/api-docs | OpenAPI specification |

### H2 Database Console Login
- **JDBC URL**: `jdbc:h2:file:./data/oneapi-admin`
- **Username**: `sa`
- **Password**: (leave empty)

## 🧪 Test the APIs

### Option 1: Postman (Recommended)

1. **Import Collection**:
   - Open Postman
   - Click Import
   - Select `OneAPI-Platform-Postman-Collection.json`
   - Collection will be loaded with all endpoints

2. **Configure Variables** (Auto-configured):
   - `baseUrl`: http://localhost:8090
   - `userId`: testuser
   - `clientId`: (auto-set after creating client)
   - `clientSecret`: (auto-set after creating client)

3. **Run Tests**:
   - Navigate to "1. Client Service" folder
   - Click "Create API Client"
   - Click "Send"
   - The clientId and clientSecret will be auto-saved!
   - Try other endpoints in sequence

### Option 2: Swagger UI (Easiest)

1. Open http://localhost:8090/swagger-ui.html
2. Browse the API sections
3. Click any endpoint → "Try it out"
4. Fill in parameters
5. Click "Execute"
6. View response

### Option 3: curl (Command Line)

**Test Client Service:**
```bash
# Create a client
curl -X POST http://localhost:8090/api/clients \
  -H "Content-Type: application/json" \
  -H "X-User-Id: testuser" \
  -d '{
    "name": "Test App",
    "description": "Testing",
    "scopes": ["read", "write"],
    "rateLimit": 1000
  }'

# List clients
curl http://localhost:8090/api/clients \
  -H "X-User-Id: testuser"
```

**Test Access Control:**
```bash
# Create permission
curl -X POST http://localhost:8090/api/access-control/permissions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "database.read",
    "resource": "database",
    "action": "read",
    "description": "Read databases"
  }'

# Create role
curl -X POST http://localhost:8090/api/access-control/roles \
  -H "Content-Type: application/json" \
  -d '{
    "name": "viewer",
    "description": "Read-only",
    "permissions": ["database.read"]
  }'

# Assign role to user
curl -X POST http://localhost:8090/api/access-control/users/alice/roles \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin" \
  -d '{"roleName": "viewer"}'

# Check permission
curl "http://localhost:8090/api/access-control/users/alice/check?resource=database&action=read"
```

**Test Throttle:**
```bash
# Check rate limit
curl http://localhost:8090/api/throttle/check/testuser

# Set custom limit
curl -X POST http://localhost:8090/api/throttle/limits/testuser \
  -H "Content-Type: application/json" \
  -d '{"requestsPerMinute": 10}'

# Test rate limiting (run 15 times)
for i in {1..15}; do
  echo "Request $i:"
  curl http://localhost:8090/api/throttle/check/testuser
  echo ""
done
```

**Test Seed Data:**
```bash
# Generate 10 sample connections
curl -X POST "http://localhost:8090/api/seed-data/connections?count=10" \
  -H "X-User-Id: testuser"

# Cleanup
curl -X DELETE http://localhost:8090/api/seed-data/cleanup \
  -H "X-User-Id: testuser"
```

### Option 4: GraphQL Playground

1. Open http://localhost:8090/graphiql

2. **Query Example:**
```graphql
query {
  clients(userId: "testuser") {
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

3. **Mutation Example:**
```graphql
mutation {
  createClient(
    userId: "testuser"
    name: "GraphQL Client"
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

## 🗄️ View Database Contents

1. Open http://localhost:8090/h2-console
2. Login with:
   - JDBC URL: `jdbc:h2:file:./data/oneapi-admin`
   - Username: `sa`
   - Password: (empty)
3. Click "Connect"
4. View tables:
   - `API_CLIENTS` - Client credentials
   - `PERMISSIONS` - Available permissions
   - `ROLES` - Role definitions
   - `USER_ROLES` - User-role assignments
   - `DATABASE_CONNECTIONS` - Database configs
   - And more...

## 📊 Sample Testing Workflow

### Complete Client Lifecycle Test

1. **Create Client** (Postman: "Create API Client")
   ```
   POST /api/clients
   ```
   Response saves `clientId` and `clientSecret` automatically!

2. **List Clients** (Postman: "List All Clients")
   ```
   GET /api/clients
   ```

3. **Validate Credentials** (Postman: "Validate Client Credentials")
   ```
   POST /api/clients/validate
   ```
   Uses auto-saved credentials!

4. **Deactivate** (Postman: "Deactivate Client")
   ```
   PATCH /api/clients/{clientId}/status
   ```

5. **Delete** (Postman: "Delete Client")
   ```
   DELETE /api/clients/{clientId}
   ```

### Complete Access Control Test

1. **Create 3 permissions** (Run in Postman folder "Permissions")
2. **Create 2 roles** (Run in Postman folder "Roles")
3. **Assign roles to users** (Run in Postman folder "User Roles")
4. **Check permissions** (Test permission checks)

### Rate Limiting Stress Test

In Postman:
1. Go to "Throttle Service" → "Stress Test - 20 Requests"
2. Click the "Run" icon (Runner)
3. Set iterations to 20
4. Run collection
5. Watch requests get throttled after hitting limit!

## 🔧 Troubleshooting

### Port 8090 Already in Use
```bash
# Find what's using port 8090
lsof -i :8090

# Kill the process
kill -9 <PID>
```

### Database Locked Error
```bash
# Stop app and remove lock file
rm ./data/oneapi-admin.lock.db
```

### Clear All Data
```bash
# Stop app first, then:
rm -rf ./data/
# Restart app - fresh database will be created
```

### Check Logs
The application logs to console. Look for:
```
Started OneApiAdminApplication in X.XXX seconds
```

## 📝 Next Steps

1. ✅ Start the application
2. ✅ Import Postman collection
3. ✅ Test Client Service endpoints
4. ✅ Test Access Control endpoints
5. ✅ Test Throttle Service
6. ✅ View data in H2 console
7. ✅ Try GraphQL queries

## 🎯 Key Features to Test

### Client Service
- ✅ Create client with auto-generated credentials
- ✅ Credentials hashed with SHA-256
- ✅ Scope-based permissions
- ✅ Rate limit per client
- ✅ Activate/deactivate clients

### Access Control
- ✅ Fine-grained permissions (resource + action)
- ✅ Role-based access control
- ✅ Dynamic role assignments
- ✅ Permission checking
- ✅ Role expiration support

### Throttle Service
- ✅ 60 requests/minute default
- ✅ Custom limits per user
- ✅ Real-time request counting
- ✅ Automatic cleanup

### Seed Data
- ✅ Generate test connections
- ✅ Multiple database types
- ✅ Realistic data

## 📚 Documentation

- **TESTING_GUIDE.md** - Detailed testing instructions
- **README.md** - Project overview
- **Swagger UI** - Interactive API docs
- **GraphQL Schema** - /graphql endpoint

## ✅ Database Information

**Admin App Database:**
- **Type**: H2 (embedded, file-based)
- **Location**: `./data/oneapi-admin.mv.db`
- **Auto-created**: Yes (on first start)
- **Schema**: Auto-created from JPA entities
- **Persistence**: Data persists between restarts

**Security App Database:**
- Check `oneapi-security-app/src/main/resources/` for configuration
- Likely also uses H2 for development

## 🎉 Success Indicators

You'll know everything is working when:

1. ✅ App starts without errors
2. ✅ Swagger UI loads at http://localhost:8090/swagger-ui.html
3. ✅ H2 console accessible
4. ✅ Can create a client and see auto-generated credentials
5. ✅ Database shows tables (check H2 console)
6. ✅ Rate limiting works (test with 15 requests)
7. ✅ GraphQL queries return data

Happy Testing! 🚀
