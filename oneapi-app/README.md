# OneAPI Admin App

A comprehensive admin application for managing database connections and schema versions with both REST and GraphQL APIs.

## Features

- **Multi-Database Support**: Connect to PostgreSQL, MySQL, Oracle, SQL Server, and MariaDB
- **Schema Version Management**: Capture and store database schemas with versioning
- **Dual API**: Complete REST and GraphQL APIs for all operations
- **Connection Testing**: Test database connections before saving
- **Schema Discovery**: Automatically discover tables, columns, primary keys, and metadata
- **H2 Database**: Built-in H2 database for metadata storage
- **Swagger UI**: Interactive API documentation at `/swagger-ui.html`
- **GraphiQL**: Interactive GraphQL playground at `/graphiql`
- **H2 Console**: Database console at `/h2-console`

## Architecture

```
oneapi-admin-app/
├── Entities
│   ├── DatabaseConnection (Connection configurations)
│   └── SchemaVersion (Schema snapshots with versioning)
├── Services
│   ├── DatabaseConnectionService (Connection management)
│   └── SchemaVersionService (Schema capture and retrieval)
├── REST API (Port 8090)
│   ├── /api/connections/* (Connection endpoints)
│   └── /api/schemas/* (Schema endpoints)
└── GraphQL API
    ├── /graphql (GraphQL endpoint)
    └── /graphiql (GraphQL playground)
```

## Quick Start

### 1. Build and Run

```bash
cd oneapi-admin-app
mvn clean install
mvn spring-boot:run
```

The application will start on **http://localhost:8090**

### 2. Access UIs

- **Swagger UI**: http://localhost:8090/swagger-ui.html
- **GraphiQL**: http://localhost:8090/graphiql
- **H2 Console**: http://localhost:8090/h2-console
  - JDBC URL: `jdbc:h2:file:./data/oneapi-admin`
  - Username: `sa`
  - Password: (empty)

## REST API Examples

### Database Connections

#### Create Connection

```bash
curl -X POST http://localhost:8090/api/connections \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my-postgres-db",
    "type": "POSTGRESQL",
    "host": "localhost",
    "port": 5432,
    "database": "mydb",
    "username": "user",
    "password": "password"
  }'
```

#### List All Connections

```bash
curl http://localhost:8090/api/connections
```

#### Get Connection by ID

```bash
curl http://localhost:8090/api/connections/1
```

#### Test Connection

```bash
curl -X POST http://localhost:8090/api/connections/1/test
```

#### Update Connection

```bash
curl -X PUT http://localhost:8090/api/connections/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "updated-postgres-db",
    "type": "POSTGRESQL",
    "host": "localhost",
    "port": 5432,
    "database": "mydb",
    "username": "newuser",
    "password": "newpassword",
    "active": true
  }'
```

#### Delete Connection

```bash
curl -X DELETE http://localhost:8090/api/connections/1
```

### Schema Versions

#### Capture Schema

```bash
curl -X POST "http://localhost:8090/api/schemas/capture/1?description=Initial+schema+capture"
```

#### Get All Schema Versions for Connection

```bash
curl http://localhost:8090/api/schemas/connection/1
```

#### Get Specific Schema Version

```bash
curl http://localhost:8090/api/schemas/connection/1/version/1
```

#### Get Latest Schema

```bash
curl http://localhost:8090/api/schemas/connection/1/latest
```

#### Get Latest Catalog

```bash
curl http://localhost:8090/api/schemas/connection/1/latest/catalog
```

## GraphQL API Examples

### Queries

#### Get All Connections

```graphql
query {
  getAllConnections {
    id
    name
    type
    host
    port
    database
    username
    active
    createdAt
    updatedAt
  }
}
```

#### Get Connection by ID

```graphql
query {
  getConnection(id: 1) {
    id
    name
    type
    host
    port
    database
  }
}
```

#### Test Connection

```graphql
query {
  testConnectionById(id: 1) {
    success
    message
    status
  }
}
```

#### Get Schema Versions

```graphql
query {
  getSchemaVersions(connectionId: 1) {
    id
    version
    connectionName
    capturedAt
    description
    tableCount
    totalColumns
  }
}
```

#### Get Latest Schema

```graphql
query {
  getLatestSchema(connectionId: 1) {
    id
    version
    schemaJson
    capturedAt
    tableCount
    totalColumns
  }
}
```

#### Get Latest Catalog

```graphql
query {
  getLatestCatalog(connectionId: 1) {
    entities {
      name
      namespace
      syncMode
      hasIncrementalField
      incrementalFields
      primaryKeys
    }
  }
}
```

### Mutations

#### Create Connection

```graphql
mutation {
  createConnection(input: {
    name: "my-postgres-db"
    type: "POSTGRESQL"
    host: "localhost"
    port: 5432
    database: "mydb"
    username: "user"
    password: "password"
    active: true
  }) {
    id
    name
    type
    createdAt
  }
}
```

#### Update Connection

```graphql
mutation {
  updateConnection(id: 1, input: {
    name: "updated-postgres-db"
    type: "POSTGRESQL"
    host: "localhost"
    port: 5432
    database: "mydb"
    username: "newuser"
    password: "newpassword"
  }) {
    id
    name
    updatedAt
  }
}
```

#### Capture Schema

```graphql
mutation {
  captureSchema(connectionId: 1, description: "Initial schema capture") {
    id
    version
    capturedAt
    tableCount
    totalColumns
  }
}
```

#### Delete Connection

```graphql
mutation {
  deleteConnection(id: 1)
}
```

#### Delete Schema Version

```graphql
mutation {
  deleteSchemaVersion(id: 1)
}
```

## Database Schema

### DatabaseConnection Table

| Column | Type | Description |
|--------|------|-------------|
| id | Long | Primary key |
| name | String | Unique connection name |
| type | Enum | Database type (POSTGRESQL, MYSQL, etc.) |
| host | String | Database host |
| port | Integer | Database port |
| database | String | Database name |
| username | String | Username |
| password | String | Password (encrypted) |
| additional_params | String | Additional connection parameters |
| active | Boolean | Connection active status |
| created_at | DateTime | Creation timestamp |
| updated_at | DateTime | Last update timestamp |

### SchemaVersion Table

| Column | Type | Description |
|--------|------|-------------|
| id | Long | Primary key |
| connection_id | Long | Foreign key to DatabaseConnection |
| version | Integer | Version number (auto-incremented) |
| schema_json | Text | Complete schema in JSON format |
| captured_at | DateTime | When schema was captured |
| description | String | Version description |
| table_count | Integer | Number of tables |
| total_columns | Integer | Total number of columns |

## Supported Database Types

- **POSTGRESQL** - PostgreSQL databases
- **MYSQL** - MySQL databases
- **ORACLE** - Oracle databases (requires implementation)
- **MSSQL** - Microsoft SQL Server (requires implementation)
- **MARIADB** - MariaDB databases (requires implementation)

Currently, PostgreSQL is fully implemented. Other database types can be added by:
1. Creating a connector extending `AbstractDatabaseSource`
2. Adding it to `DatabaseConnectionService.createSource()`

## Configuration

### Application Properties (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/oneapi-admin
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: update

  graphql:
    graphiql:
      enabled: true
      path: /graphiql

server:
  port: 8090
```

### Environment Variables

You can override configuration with environment variables:

```bash
export SERVER_PORT=8090
export SPRING_DATASOURCE_URL=jdbc:h2:file:./custom-data/admin
```

## Development

### Running Tests

```bash
mvn test
```

### Building

```bash
mvn clean package
```

### Running the JAR

```bash
java -jar target/oneapi-admin-app-0.0.1.jar
```

## Use Cases

### 1. Database Migration Tracking

Capture schema before and after migrations to track changes:

```bash
# Before migration
curl -X POST "http://localhost:8090/api/schemas/capture/1?description=Before+migration+v2.0"

# After migration
curl -X POST "http://localhost:8090/api/schemas/capture/1?description=After+migration+v2.0"

# Compare versions
curl http://localhost:8090/api/schemas/connection/1
```

### 2. Multi-Environment Management

Manage connections to dev, staging, and production databases:

```bash
# Add dev database
curl -X POST http://localhost:8090/api/connections \
  -H "Content-Type: application/json" \
  -d '{"name": "dev-db", "type": "POSTGRESQL", "host": "dev.db.com", ...}'

# Add staging database
curl -X POST http://localhost:8090/api/connections \
  -H "Content-Type: application/json" \
  -d '{"name": "staging-db", "type": "POSTGRESQL", "host": "staging.db.com", ...}'
```

### 3. Schema Documentation

Generate schema documentation by capturing current state:

```bash
curl http://localhost:8090/api/schemas/connection/1/latest/catalog
```

## API Endpoints Summary

### REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/connections | Create connection |
| GET | /api/connections | List all connections |
| GET | /api/connections/{id} | Get connection by ID |
| GET | /api/connections/name/{name} | Get connection by name |
| PUT | /api/connections/{id} | Update connection |
| DELETE | /api/connections/{id} | Delete connection |
| POST | /api/connections/{id}/test | Test connection |
| POST | /api/connections/test | Test without saving |
| POST | /api/schemas/capture/{connectionId} | Capture schema |
| GET | /api/schemas/connection/{connectionId} | Get all versions |
| GET | /api/schemas/connection/{connectionId}/version/{version} | Get specific version |
| GET | /api/schemas/connection/{connectionId}/latest | Get latest version |
| GET | /api/schemas/connection/{connectionId}/latest/catalog | Get latest catalog |
| DELETE | /api/schemas/{id} | Delete schema version |

### GraphQL API

Available at `/graphql` and `/graphiql`

**Queries:**
- getAllConnections
- getConnection
- getConnectionByName
- testConnectionById
- getSchemaVersions
- getSchemaVersion
- getLatestSchema
- getLatestCatalog

**Mutations:**
- createConnection
- updateConnection
- deleteConnection
- captureSchema
- deleteSchemaVersion

## License

Part of the OneAPI SDK project.
