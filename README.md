# OneAPI Platform

A unified API platform for database metadata discovery, query management, and data integration built with Spring Boot 3.2.0 and Java 21.

## Architecture Overview

OneAPI Platform follows a clean, modular architecture:

```
oneapi-platform/
├── oneapi-sdk/              # Core SDK with database connector abstractions
├── oneapi-h2-connector/     # H2 database connector implementation
├── oneapi-postgres-connector/   # PostgreSQL connector implementation
├── oneapi-sqlserver-connector/  # SQL Server connector implementation
└── oneapi-app/              # Spring Boot REST API application
```

### Core Concepts

- **Source** → **Domain** → **Entity** → **Field**
  - **SourceInfo**: Database connection configuration (H2, PostgreSQL, SQL Server, etc.)
  - **DomainInfo**: Database schema/namespace discovered from source
  - **EntityInfo**: Table/view metadata within a domain
  - **FieldInfo**: Column/field metadata within an entity

## Features

- **Metadata Discovery**: Automatic discovery of schemas, tables, and columns from connected databases
- **Multi-Database Support**: H2, PostgreSQL, SQL Server (MySQL, Oracle, MariaDB planned)
- **Query Management**: Save, execute, and manage SQL queries
- **Security**: JWT-based authentication with role-based access control (ADMIN, USER)
- **RESTful API**: Comprehensive REST endpoints for all operations
- **Liquibase Migrations**: Database schema versioning and migration management

## Quick Start

### Prerequisites

- Java 21
- Maven 3.8+
- H2/PostgreSQL/SQL Server database (for testing)

### Build All Components

```bash
# Build SDK
cd oneapi-sdk && mvn clean install -DskipTests

# Build Connectors
cd ../oneapi-h2-connector && mvn clean install -DskipTests
cd ../oneapi-postgres-connector && mvn clean install -DskipTests
cd ../oneapi-sqlserver-connector && mvn clean install -DskipTests

# Build Application
cd ../oneapi-app && mvn clean install -DskipTests
```

### Run Application

```bash
cd oneapi-app
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Documentation

See [POSTMAN_COLLECTION.json](./POSTMAN_COLLECTION.json) for complete API documentation with examples.

### Quick API Reference

**Authentication:**
- `POST /api/authenticate` - Login and get JWT token

**Source Management:**
- `POST /api/sources` - Create database source
- `GET /api/sources` - List all sources
- `GET /api/sources/{id}` - Get source by ID
- `PUT /api/sources/{id}` - Update source
- `DELETE /api/sources/{id}` - Delete source
- `POST /api/sources/test` - Test connection

**Metadata Discovery:**
- `POST /api/v1/metadata/discover/{sourceId}` - Discover metadata

**Domains (Schemas):**
- `GET /api/v1/metadata/domains` - List all domains
- `GET /api/v1/metadata/domains/{id}` - Get domain by ID
- `GET /api/v1/metadata/domains/source/{sourceId}` - Get domains by source

**Entities (Tables):**
- `GET /api/v1/metadata/entities` - List all entities
- `GET /api/v1/metadata/entities/{id}` - Get entity by ID
- `GET /api/v1/metadata/entities/source/{sourceId}` - Get entities by source
- `GET /api/v1/metadata/entities/domain/{domainId}` - Get entities by domain
- `GET /api/v1/metadata/entities/search` - Search entities

**Fields (Columns):**
- `GET /api/v1/metadata/fields` - List all fields
- `GET /api/v1/metadata/fields/{id}` - Get field by ID
- `GET /api/v1/metadata/entities/{entityId}/fields` - Get fields by entity

### Default Users

- **Admin**: `admin` / `admin123` (ROLE_ADMIN, ROLE_USER)
- **User**: `user` / `user123` (ROLE_USER)

## Testing

### Run UAT Tests

```bash
# Start H2 server (for testing)
java -cp ~/.m2/repository/com/h2database/h2/2.2.224/h2-2.2.224.jar \
  org.h2.tools.Server -tcp -tcpPort 9092 -tcpAllowOthers -ifNotExists

# Run comprehensive UAT
bash /tmp/FINAL_UAT_WITH_FIELDS.sh
```

**Expected**: ✅ 18/18 endpoints passing

## Technology Stack

- Java 21, Spring Boot 3.2.0, Spring Data JPA, Spring Security
- Liquibase 4.24.0, H2 Database 2.2.224
- Jackson, Lombok, Maven

## Database Schema

### Core Tables

- `source_info`: Database connections
- `domain_info`: Discovered schemas
- `entity_info`: Discovered tables
- `field_info`: Discovered columns

All managed via Liquibase migrations in `oneapi-app/src/main/resources/db/changelog/changes/`

## Development

### Clean Architecture

The platform uses **field-based architecture**:
- SDK `DataEntity` contains `List<Field<?>> fields`
- No JSON Schema serialization overhead
- Type-safe, direct object access
- Clean flow: `TableInfo` → `DataEntity` → `FieldInfo`

### Adding New Database Connector

1. Extend `AbstractDatabaseSource<DataType, JdbcDatabase>`
2. Implement `discoverInternal()` to query INFORMATION_SCHEMA
3. Implement `discoverPrimaryKeys()`
4. Add connector to `ConnectorFactory`

See existing connectors for examples.

## License

Copyright © 2026 OneAPI Platform
