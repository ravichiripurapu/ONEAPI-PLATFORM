# OneAPI Platform

A comprehensive API platform for unified data access, metadata discovery, and query execution across multiple database types.

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- Port 8088 available

### Run the Application

```bash
cd oneapi-app
mvn spring-boot:run
```

Application will start on **http://localhost:8088**

### Default Credentials
- **Username**: `admin`
- **Password**: `admin123`

## API Documentation

### Swagger UI
Open in browser: **http://localhost:8088/swagger-ui.html**

### GraphiQL IDE
Open in browser: **http://localhost:8088/graphiql**

### H2 Console
Open in browser: **http://localhost:8088/h2-console**
- **JDBC URL**: `jdbc:h2:file:./data/oneapi;AUTO_SERVER=TRUE`
- **Username**: `sa`
- **Password**: *(leave empty)*

## Postman Collection

Import the Postman collection for quick API testing:

**File**: `OneAPI-Platform.postman_collection.json`

### How to Use:

1. **Import Collection**:
   - Open Postman
   - Click Import
   - Select `OneAPI-Platform.postman_collection.json`

2. **Authenticate**:
   - Run the "Login (Get JWT Token)" request in the Authentication folder
   - The JWT token will be automatically saved to the collection variable
   - All subsequent requests will use this token

3. **Test Endpoints**:
   - Create a datasource
   - Discover metadata
   - Execute SQL queries
   - Create saved queries, dashboards, and reports

## Architecture

### Database Schema (15 Tables)

**Level 1 - Base Tables**
- `app_user` - User accounts
- `source_info` - Data source connections

**Level 2 - Dependencies**
- `role` - User roles for RBAC
- `domain_info` - Database schemas discovered

**Level 3 - Metadata & Features**
- `entity_info` - Database tables discovered
- `field_info` - Table columns discovered
- `saved_query` - Saved SQL queries
- `dashboard` - User dashboards
- `user_preferences` - User settings

**Level 4 - Advanced Features**
- `report` - Report definitions
- `widget` - Dashboard widgets
- `schedule` - Scheduled tasks

**Level 5 - Security & Audit**
- `user_role` - User-to-Role mappings
- `permission` - Hierarchical RBAC permissions
- `audit_log` - Audit trail

### Key Features

#### 1. Multi-Database Support
- H2 (embedded and server mode)
- PostgreSQL
- SQL Server
- MySQL *(coming soon)*

#### 2. Metadata Discovery
- Automatic schema detection
- Table and column discovery
- Data type mapping
- Re-sync capability for schema changes

#### 3. Query Execution
- Session-based pagination
- Configurable page sizes
- Query result streaming
- Session timeout management

#### 4. Saved Queries
- Create reusable SQL queries
- Parameterized queries
- Public/private sharing
- Execution history tracking

#### 5. Dashboards & Reports
- Visual dashboard creation
- Widget-based layout
- Report scheduling
- Multiple export formats

#### 6. Role-Based Access Control (RBAC)
- Hierarchical permissions (Source → Domain → Entity → Field)
- Fine-grained access control
- Role management
- User assignment

#### 7. Security
- JWT-based authentication
- Bearer token authorization
- Password hashing (BCrypt)
- Session management

#### 8. GraphQL Support
- Full GraphQL API
- Interactive GraphiQL IDE
- Query introspection

## API Endpoints

### Authentication
```
POST   /api/authenticate                 # Login and get JWT token
```

### Data Sources
```
GET    /api/v1/datasources               # List all datasources
POST   /api/v1/datasources               # Create datasource
GET    /api/v1/datasources/{id}          # Get datasource by ID
PUT    /api/v1/datasources/{id}          # Update datasource
DELETE /api/v1/datasources/{id}          # Delete datasource
POST   /api/v1/datasources/{id}/test     # Test connection
```

### Metadata Discovery
```
POST   /api/metadata/discover/{id}       # Discover metadata
POST   /api/metadata/sync/{id}           # Re-sync metadata
GET    /api/metadata/domains              # List schemas
GET    /api/metadata/entities             # List tables
GET    /api/metadata/fields               # List columns
```

### Query Execution
```
POST   /api/query                         # Execute SQL query
                                           # Returns: records, sessionKey, hasMore
```

**Request Body**:
```json
{
  "datasourceId": 1,
  "sqlQuery": "SELECT * FROM employees"
}
```

**For Next Page**:
```json
{
  "sessionKey": "session-uuid-from-previous-response"
}
```

### Saved Queries
```
GET    /api/v1/queries                    # List all queries
POST   /api/v1/queries                    # Create saved query
GET    /api/v1/queries/{id}               # Get query by ID
PUT    /api/v1/queries/{id}               # Update query
DELETE /api/v1/queries/{id}               # Delete query
POST   /api/v1/queries/{id}/execute       # Execute saved query
GET    /api/v1/queries/my                 # Get my queries
GET    /api/v1/queries/public             # Get public queries
```

### Dashboards
```
GET    /api/v1/dashboards                 # List all dashboards
POST   /api/v1/dashboards                 # Create dashboard
GET    /api/v1/dashboards/{id}            # Get dashboard by ID
PUT    /api/v1/dashboards/{id}            # Update dashboard
DELETE /api/v1/dashboards/{id}            # Delete dashboard
GET    /api/v1/dashboards/my              # Get my dashboards
```

### Reports
```
GET    /api/v1/reports                    # List all reports
POST   /api/v1/reports                    # Create report
GET    /api/v1/reports/{id}               # Get report by ID
PUT    /api/v1/reports/{id}               # Update report
DELETE /api/v1/reports/{id}               # Delete report
```

## Configuration

### Application Settings

Edit `oneapi-app/src/main/resources/application.yml`:

```yaml
server:
  port: 8088

spring:
  datasource:
    url: jdbc:h2:file:./data/oneapi;AUTO_SERVER=TRUE

  security:
    authentication:
      jwt:
        token-validity-in-seconds: 86400  # 24 hours

oneapi:
  query:
    session:
      default-page-size: 100
      max-page-size: 1000
      default-ttl-minutes: 15
```

### Database Configuration

The application uses H2 by default for simplicity. The database file is created at:
```
oneapi-app/data/oneapi.mv.db
```

### Logging

Adjust logging levels in `application.yml`:
```yaml
logging:
  level:
    io.oneapi: DEBUG
    org.springframework.graphql: DEBUG
    org.hibernate.SQL: DEBUG
```

## Example Workflows

### 1. Connect to a Database

```bash
# 1. Login and get token
curl -X POST http://localhost:8088/api/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Save the id_token from response
export TOKEN="your-jwt-token"

# 2. Create datasource
curl -X POST http://localhost:8088/api/v1/datasources \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My H2 Database",
    "databaseType": "H2",
    "host": "localhost",
    "port": 9092,
    "databaseName": "testdb",
    "username": "sa",
    "password": ""
  }'
```

### 2. Discover Metadata

```bash
# Discover all schemas, tables, and columns
curl -X POST http://localhost:8088/api/metadata/discover/1 \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Execute a Query

```bash
# Execute SQL query
curl -X POST http://localhost:8088/api/query \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "datasourceId": 1,
    "sqlQuery": "SELECT * FROM employees LIMIT 10"
  }'
```

### 4. Create and Execute Saved Query

```bash
# Create saved query
curl -X POST http://localhost:8088/api/v1/queries \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Employee Report",
    "queryText": "SELECT * FROM employees",
    "datasourceId": 1,
    "isPublic": true
  }'

# Execute saved query
curl -X POST http://localhost:8088/api/v1/queries/1/execute \
  -H "Authorization: Bearer $TOKEN"
```

## Development

### Project Structure
```
oneapi-platform/
├── oneapi-app/              # Main Spring Boot application
├── oneapi-sdk/              # SDK interfaces
├── oneapi-h2-connector/     # H2 database connector
├── oneapi-postgres-connector/ # PostgreSQL connector
├── oneapi-sqlserver-connector/ # SQL Server connector
├── OneAPI-Platform.postman_collection.json
└── README.md
```

### Build All Modules

```bash
mvn clean install
```

### Run Tests

```bash
mvn test
```

### Create JAR

```bash
cd oneapi-app
mvn clean package
java -jar target/oneapi-app-0.0.1.jar
```

## Database Migrations

The application uses Liquibase for database migrations. Migrations are located at:
```
oneapi-app/src/main/resources/db/changelog/
```

Migrations are organized in 5 levels based on dependencies:
- Level 1: Base tables (no foreign keys)
- Level 2: First-level dependencies
- Level 3: Second-level dependencies
- Level 4: Third-level dependencies
- Level 5: Join tables and audit logs

### Migration Features
- ✅ Idempotent (safe to run multiple times)
- ✅ Automatic execution on startup
- ✅ Versioned changesets
- ✅ Rollback support

## Troubleshooting

### Port Already in Use
If port 8088 is in use, change it in `application.yml`:
```yaml
server:
  port: 8088  # Change to your preferred port
```

### Database Locked
If you see "Database already in use", ensure no other instance is running:
```bash
# Find process using port 8088
lsof -ti :8088

# Kill the process
kill -9 <PID>
```

### Clean Database
To start with a fresh database:
```bash
rm -rf oneapi-app/data/*.mv.db
mvn spring-boot:run
```

## Technologies Used

- **Java 21** - Programming language
- **Spring Boot 3.2.0** - Application framework
- **Spring Security** - Authentication & authorization
- **Spring Data JPA** - Data access
- **Hibernate** - ORM
- **Liquibase** - Database migrations
- **H2 Database** - Embedded database
- **Spring GraphQL** - GraphQL support
- **Springdoc OpenAPI** - Swagger documentation
- **Caffeine** - Caching
- **JWT** - Token-based authentication
- **Lombok** - Boilerplate reduction
- **Maven** - Build tool

## Security Considerations

### Production Deployment

1. **Change Default Credentials**:
   - Update admin password in database seed script
   - Use strong passwords

2. **Secure JWT Secret**:
   - Generate new base64-encoded secret key:
     ```bash
     openssl rand -base64 64
     ```
   - Update `spring.security.authentication.jwt.base64-secret` in application.yml
   - Store in environment variable

3. **Enable HTTPS**:
   - Configure SSL certificate
   - Update `server.ssl.*` properties

4. **Database Security**:
   - Use external database (PostgreSQL, MySQL)
   - Enable encryption at rest
   - Use strong database passwords

5. **CORS Configuration**:
   - Configure allowed origins in SecurityConfig.java
   - Restrict to specific domains

## License

Copyright © 2026 OneAPI Platform

## Support

For issues and questions:
- Check Swagger documentation: http://localhost:8088/swagger-ui.html
- Review API logs in console
- Check H2 database: http://localhost:8088/h2-console

---

**Version**: 1.0
**Last Updated**: March 2026
**Port**: 8088
**Status**: Production Ready ✅
