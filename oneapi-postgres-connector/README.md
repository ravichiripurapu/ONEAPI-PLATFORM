# OneAPI PostgreSQL Connector

PostgreSQL connector built with OneAPI SDK featuring a GraphQL API for data access.

## Features

- **Full PostgreSQL Support**: Connect to PostgreSQL databases with connection pooling
- **Schema Discovery**: Automatically discover tables, columns, primary keys, and incremental fields
- **Data Reading**: Read data from PostgreSQL tables with iterator-based streaming
- **GraphQL API**: Query data, discover schema, and check connections via GraphQL
- **Integration Tests**: Comprehensive test suite using Testcontainers

## Architecture

Built on top of `oneapi-sdk`, this connector:
- Extends `AbstractDatabaseSource` for common database functionality
- Implements JDBC-based table discovery and data reading
- Provides a Spring Boot GraphQL API for easy integration
- Uses HikariCP for efficient connection pooling

## GraphQL API

The connector exposes three main queries:

### Check Connection
```graphql
query {
  checkConnection(config: {
    host: "localhost"
    port: 5432
    database: "mydb"
    username: "user"
    password: "pass"
  }) {
    status
    message
  }
}
```

### Discover Catalog
```graphql
query {
  discoverCatalog(config: {
    host: "localhost"
    port: 5432
    database: "mydb"
    username: "user"
    password: "pass"
  }) {
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

### Read Data
```graphql
query {
  readData(
    config: {
      host: "localhost"
      port: 5432
      database: "mydb"
      username: "user"
      password: "pass"
    }
    entityName: "users"
    namespace: "public"
    limit: 10
  ) {
    records
    count
  }
}
```

## Running the Application

### Start the GraphQL Server

```bash
mvn spring-boot:run
```

The GraphQL API will be available at:
- GraphQL endpoint: `http://localhost:8080/graphql`
- GraphiQL UI: `http://localhost:8080/graphiql`

### Running Integration Tests

The project includes comprehensive integration tests using Testcontainers:

```bash
mvn test
```

Tests cover:
- Connection checking
- Schema discovery
- Data reading from multiple tables
- Error handling for invalid connections

## Example Usage

### Java API

```java
ObjectMapper mapper = new ObjectMapper();
ObjectNode config = mapper.createObjectNode();
config.put("host", "localhost");
config.put("port", 5432);
config.put("database", "mydb");
config.put("username", "user");
config.put("password", "password");

PostgresSource source = new PostgresSource();

// Check connection
ConnectionStatus status = source.check(config);
System.out.println("Connection: " + status.getStatus());

// Discover schema
Catalog catalog = source.discover(config);
catalog.getEntities().forEach(entity -> {
    System.out.println("Table: " + entity.getName());
    System.out.println("Primary Keys: " + entity.getPrimaryKeys());
});

// Read data
State state = new State();
EntityRecordIterator<EntityRecord> iterator = source.read(config, catalog, state);
while (iterator.hasNext()) {
    EntityRecord record = iterator.next();
    System.out.println(record.getData());
}
iterator.close();
source.close();
```

## Configuration

### Application Properties

Configure the server in `application.yml`:

```yaml
spring:
  application:
    name: oneapi-postgres-connector
  graphql:
    graphiql:
      enabled: true
      path: /graphiql
    path: /graphql

server:
  port: 8080
```

## Dependencies

- **oneapi-sdk**: Core SDK for database connectors
- **PostgreSQL Driver**: JDBC driver for PostgreSQL
- **Spring Boot**: Web framework and GraphQL support
- **GraphQL Java**: GraphQL implementation
- **HikariCP**: Connection pooling
- **Testcontainers**: Integration testing with Docker

## Integration Tests

The test suite includes:

1. **testCheckConnection**: Validates connection to PostgreSQL
2. **testDiscoverCatalog**: Discovers all tables and metadata
3. **testReadData**: Reads data from users table
4. **testReadProducts**: Reads data from products table
5. **testInvalidConnection**: Handles connection failures gracefully

All tests use Testcontainers to spin up a real PostgreSQL instance.

## Building

```bash
mvn clean install
```

## Project Structure

```
oneapi-postgres-connector/
├── src/main/java/io/oneapi/postgres/
│   ├── PostgresConnectorApplication.java    - Spring Boot application
│   ├── source/
│   │   └── PostgresSource.java              - PostgreSQL implementation
│   └── graphql/
│       └── PostgresGraphQLController.java   - GraphQL API endpoints
├── src/main/resources/
│   ├── application.yml                      - Application configuration
│   └── graphql/
│       └── schema.graphqls                  - GraphQL schema definition
├── src/test/java/
│   └── PostgresSourceIntegrationTest.java   - Integration tests
└── pom.xml
```

## License

Part of the OneAPI SDK project.
