# OneAPI App - Unified Application Migration Guide

## Overview

The **oneapi-app** is a unified application that merges the functionality of both `oneapi-admin-app` and `oneapi-security-app` into a single, simplified deployment.

## Why We Merged

### Benefits of Single Application:
1. ✅ **Simpler Deployment** - One JAR file, one configuration, one database
2. ✅ **Easier Development** - Single codebase, no inter-service communication
3. ✅ **Better Performance** - Direct method calls instead of HTTP requests
4. ✅ **Reduced Complexity** - No service discovery, unified error handling
5. ✅ **Lower Resource Usage** - Single JVM, shared connection pools

### What Changed:
- **Port**: Changed from `8090` (admin) to `8080` (unified)
- **Database**: Changed from `./data/oneapi-admin` to `./data/oneapi`
- **Package**: Changed from `io.oneapi.admin` to `io.oneapi.app`
- **Module**: New `oneapi-app` module replaces both legacy apps

## Architecture

### Merged Structure:
```
oneapi-app/
├── src/main/java/io/oneapi/app/
│   ├── OneApiApplication.java          # Main application class
│   ├── config/                          # Configurations from admin-app
│   ├── controller/                      # REST & GraphQL controllers (admin)
│   ├── dto/                            # DTOs from admin-app
│   ├── entity/                         # Entities from admin-app
│   ├── repository/                     # Repositories from admin-app
│   ├── service/                        # Services from admin-app
│   ├── connector/                      # Database connectors
│   ├── exception/                      # Exception handling
│   ├── model/                          # Models
│   └── security/                       # All security-app features
│       ├── config/                     # Security configurations
│       ├── domain/                     # User, Authority, Permissions
│       ├── repository/                 # Security repositories
│       ├── service/                    # Auth & user services
│       └── web/rest/                   # Security REST endpoints
└── src/main/resources/
    ├── application.yml                 # Unified configuration
    ├── db/changelog/                   # Admin Liquibase
    │   └── changes/
    │       ├── 01-create-sample-tables.xml
    │       └── 02-insert-sample-data.xml
    ├── config/liquibase/              # Security Liquibase
    │   ├── master.xml
    │   └── changelog/
    │       └── 00000000000000_initial_schema.xml
    └── graphql/                        # GraphQL schemas
```

## Features Included

### From Admin App:
- ✅ Database connection management
- ✅ Query execution service with sessions
- ✅ API client management
- ✅ Access control (RBAC)
- ✅ Rate limiting / throttle service
- ✅ Sample data generation with Faker
- ✅ GraphQL API
- ✅ REST API
- ✅ Swagger UI

### From Security App:
- ✅ User authentication & authorization
- ✅ JWT/OAuth2 support
- ✅ User management
- ✅ Organization management
- ✅ Database-level permissions
- ✅ Column-level permissions
- ✅ Table-level permissions
- ✅ Roles & authorities

## Configuration

### Application Settings ([application.yml](oneapi-app/src/main/resources/application.yml))

```yaml
spring:
  application:
    name: oneapi-app

  datasource:
    url: jdbc:h2:file:./data/oneapi
    username: sa
    password:

server:
  port: 8080

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

### Key Endpoints:

| URL | Purpose |
|-----|---------|
| http://localhost:8080 | Main application |
| http://localhost:8080/swagger-ui.html | Swagger API Documentation |
| http://localhost:8080/graphiql | GraphQL Playground |
| http://localhost:8080/h2-console | H2 Database Console |
| http://localhost:8080/management/health | Health Check |
| http://localhost:8080/management/metrics | Metrics |

## Quick Start

### 1. Build the Application

```bash
cd /Users/ravi/JavaProjects/oneapi-platform
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
mvn clean install -DskipTests -pl oneapi-app -am
```

### 2. Run the Application

```bash
cd oneapi-app
mvn spring-boot:run
```

Or run the JAR directly:

```bash
java -jar target/oneapi-app-0.0.1.jar
```

### 3. Access the Application

```
----------------------------------------------------------
    Application 'oneapi-app' is running!
    Access URLs:
      Local:        http://localhost:8080/
      External:     http://192.168.1.x:8080/
      H2 Console:   http://localhost:8080/h2-console
      Swagger UI:   http://localhost:8080/swagger-ui.html
      GraphQL:      http://localhost:8080/graphiql
    Profile(s):   [default]
----------------------------------------------------------
```

## Migration from Legacy Apps

### If You Were Using `oneapi-admin-app`:

**Change URLs:**
- Old: `http://localhost:8090`
- New: `http://localhost:8080`

**Update Database Path:**
- Old: `jdbc:h2:file:./data/oneapi-admin`
- New: `jdbc:h2:file:./data/oneapi`

**Update Imports (if using as library):**
```java
// Old
import io.oneapi.admin.service.DatabaseQueryService;

// New
import io.oneapi.app.service.DatabaseQueryService;
```

### If You Were Using `oneapi-security-app`:

**All security features are available** in the new app under the `/security` package path.

**Update URLs:**
- Security features are now at the same base URL: `http://localhost:8080`

## Database Schema

### Unified Liquibase Changelog

The application uses a unified Liquibase master changelog that includes both security and admin schemas:

[db/changelog/db.changelog-master.xml](oneapi-app/src/main/resources/db/changelog/db.changelog-master.xml):
```xml
<!-- Security/User Management Schema -->
<include file="config/liquibase/changelog/00000000000000_initial_schema.xml"/>

<!-- Sample Database Tables for Testing -->
<include file="db/changelog/changes/01-create-sample-tables.xml"/>
<include file="db/changelog/changes/02-insert-sample-data.xml"/>
```

### Tables Created:

**Security Tables:**
- `jhi_user` - User accounts
- `jhi_authority` - Roles/authorities
- `jhi_user_authority` - User-role mappings
- `org` - Organizations
- `database_permission` - Database-level permissions
- `table_permission` - Table-level permissions
- `column_permission` - Column-level permissions

**Admin Tables:**
- `database_connections` - Database connection configs
- `api_clients` - API client credentials
- `permissions` - RBAC permissions
- `roles` - RBAC roles
- `user_roles` - User-role assignments
- `rate_limits` - Custom rate limit configurations

**Sample Tables:**
- `sample_employees` - Test employee data
- `sample_customers` - Test customer data
- `sample_products` - Test product data
- `sample_orders` - Test order data
- `sample_order_items` - Test order line items

## Testing

### Run Tests

```bash
mvn test -pl oneapi-app
```

### Access H2 Console

1. Navigate to: http://localhost:8080/h2-console
2. Connection settings:
   - JDBC URL: `jdbc:h2:file:./data/oneapi`
   - Username: `sa`
   - Password: (empty)

### Use Swagger UI

1. Navigate to: http://localhost:8080/swagger-ui.html
2. Explore all available endpoints
3. Test APIs interactively

### Use GraphQL Playground

1. Navigate to: http://localhost:8080/graphiql
2. Write GraphQL queries
3. Explore schema documentation

## Building for Production

### Create Executable JAR

```bash
mvn clean package -pl oneapi-app -am
```

Output: `oneapi-app/target/oneapi-app-0.0.1.jar`

### Run in Production

```bash
java -jar oneapi-app-0.0.1.jar \
  --spring.profiles.active=prod \
  --server.port=8080
```

### Docker Deployment

```dockerfile
FROM eclipse-temurin:17-jre
COPY oneapi-app/target/oneapi-app-0.0.1.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

Build and run:
```bash
docker build -t oneapi-app .
docker run -p 8080:8080 oneapi-app
```

## Troubleshooting

### Issue: Port 8080 Already in Use

**Solution:** Change the port in `application.yml` or use command line:
```bash
java -jar oneapi-app-0.0.1.jar --server.port=9090
```

### Issue: Database Migration Errors

**Solution:** Clear the database and restart:
```bash
rm -rf data/
mvn spring-boot:run
```

### Issue: Build Failures

**Solution:** Clean and rebuild:
```bash
mvn clean install -U
```

## Legacy Apps

The old `oneapi-admin-app` and `oneapi-security-app` modules have been **commented out** in the parent [pom.xml](pom.xml):

```xml
<modules>
    <module>oneapi-sdk</module>
    <module>oneapi-postgres-connector</module>
    <module>oneapi-h2-connector</module>
    <module>oneapi-sqlserver-connector</module>
    <module>oneapi-app</module>
    <!-- Legacy apps - can be removed after successful migration -->
    <!-- <module>oneapi-admin-app</module> -->
    <!-- <module>oneapi-security-app</module> -->
</modules>
```

### To Completely Remove Legacy Apps:

```bash
cd /Users/ravi/JavaProjects/oneapi-platform
rm -rf oneapi-admin-app
rm -rf oneapi-security-app
```

## Sample Data

The unified app automatically generates sample data on startup using JavaFaker:
- 50 Employees with departments and managers
- 100 Customers with addresses
- 200 Products across 9 categories
- 300 Orders with line items

### Regenerate Sample Data

**Via REST API:**
```bash
curl -X POST http://localhost:8080/api/sample-data/regenerate
```

**Via Swagger UI:**
1. Navigate to http://localhost:8080/swagger-ui.html
2. Find "Sample Data" section
3. Execute `POST /api/sample-data/regenerate`

## Support & Documentation

- **Full Guide:** [LIQUIBASE_SAMPLE_DATA_GUIDE.md](LIQUIBASE_SAMPLE_DATA_GUIDE.md)
- **Quick Start:** [SAMPLE_DATA_QUICK_START.md](SAMPLE_DATA_QUICK_START.md)
- **Testing Guide:** [TESTING_GUIDE.md](TESTING_GUIDE.md)
- **Postman Collection:** [Sample-Data-Postman-Collection.json](Sample-Data-Postman-Collection.json)

## Summary

### ✅ Completed Migration Tasks:

1. Created unified `oneapi-app` module
2. Merged all source code from both apps
3. Consolidated Liquibase changelogs
4. Updated configurations
5. Fixed package names and imports
6. Successfully built the application
7. Updated parent pom.xml
8. Created comprehensive documentation

### 🎯 Next Steps:

1. Test the unified application
2. Migrate any custom configurations
3. Update deployment scripts
4. Remove legacy app directories (optional)
5. Update client applications to use new URLs

**The OneAPI Platform is now unified and ready for deployment!** 🚀
