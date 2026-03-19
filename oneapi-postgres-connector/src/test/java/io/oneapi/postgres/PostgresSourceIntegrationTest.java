package io.oneapi.postgres;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.oneapi.postgres.source.PostgresSource;
import io.oneapi.sdk.model.*;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostgresSourceIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @BeforeAll
    static void setupDatabase() throws Exception {
        // Create test table and insert data
        String jdbcUrl = postgres.getJdbcUrl();
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "testuser", "testpass");
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE users (" +
                    "id SERIAL PRIMARY KEY, " +
                    "name VARCHAR(100), " +
                    "email VARCHAR(100), " +
                    "age INTEGER, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            stmt.execute("INSERT INTO users (name, email, age) VALUES " +
                    "('Alice', 'alice@example.com', 30), " +
                    "('Bob', 'bob@example.com', 25), " +
                    "('Charlie', 'charlie@example.com', 35)");

            stmt.execute("CREATE TABLE products (" +
                    "id SERIAL PRIMARY KEY, " +
                    "name VARCHAR(100), " +
                    "price NUMERIC(10, 2)" +
                    ")");

            stmt.execute("INSERT INTO products (name, price) VALUES " +
                    "('Laptop', 999.99), " +
                    "('Mouse', 29.99)");
        }
    }

    private JsonNode createConfig() {
        ObjectNode config = MAPPER.createObjectNode();
        config.put("host", postgres.getHost());
        config.put("port", postgres.getMappedPort(5432));
        config.put("database", "testdb");
        config.put("username", "testuser");
        config.put("password", "testpass");
        return config;
    }

    @Test
    @Order(1)
    void testCheckConnection() throws Exception {
        PostgresSource source = new PostgresSource();
        ConnectionStatus status = source.check(createConfig());
        source.close();

        assertEquals(ConnectionStatus.Status.SUCCEEDED, status.getStatus());
    }

    @Test
    @Order(2)
    void testDiscoverCatalog() throws Exception {
        PostgresSource source = new PostgresSource();
        Domain catalog = source.discover(createConfig());
        source.close();

        assertNotNull(catalog);
        assertNotNull(catalog.getEntities());
        assertTrue(catalog.getEntities().size() >= 2, "Should discover at least 2 tables");

        // Find users table
        DataEntity usersEntity = catalog.getEntities().stream()
                .filter(e -> e.getName().equals("users"))
                .findFirst()
                .orElse(null);

        assertNotNull(usersEntity, "Should find users table");
        assertEquals("public", usersEntity.getNamespace());
        assertNotNull(usersEntity.getIncrementalFields());
        assertFalse(usersEntity.getIncrementalFields().isEmpty(), "Should have incremental fields");
    }

    @Test
    @Order(3)
    void testReadData() throws Exception {
        PostgresSource source = new PostgresSource();

        // Create catalog with users table
        DataEntity usersEntity = new DataEntity();
        usersEntity.setName("users");
        usersEntity.setNamespace("public");

        Domain catalog = new Domain(List.of(usersEntity));
        State state = new State();

        EntityRecordIterator<EntityRecord> iterator = source.read(createConfig(), catalog, state);

        List<EntityRecord> records = new ArrayList<>();
        while (iterator.hasNext()) {
            records.add(iterator.next());
        }
        iterator.close();
        source.close();

        assertEquals(3, records.size(), "Should read 3 users");

        EntityRecord firstRecord = records.get(0);
        assertNotNull(firstRecord.getData());
        assertTrue(firstRecord.getData().containsKey("id"));
        assertTrue(firstRecord.getData().containsKey("name"));
        assertTrue(firstRecord.getData().containsKey("email"));
        assertTrue(firstRecord.getData().containsKey("age"));
    }

    @Test
    @Order(4)
    void testReadProducts() throws Exception {
        PostgresSource source = new PostgresSource();

        DataEntity productsEntity = new DataEntity();
        productsEntity.setName("products");
        productsEntity.setNamespace("public");

        Domain catalog = new Domain(List.of(productsEntity));
        State state = new State();

        EntityRecordIterator<EntityRecord> iterator = source.read(createConfig(), catalog, state);

        List<EntityRecord> records = new ArrayList<>();
        while (iterator.hasNext()) {
            records.add(iterator.next());
        }
        iterator.close();
        source.close();

        assertEquals(2, records.size(), "Should read 2 products");

        EntityRecord firstRecord = records.get(0);
        assertNotNull(firstRecord.getData());
        assertTrue(firstRecord.getData().containsKey("name"));
        assertTrue(firstRecord.getData().containsKey("price"));
    }

    @Test
    @Order(5)
    void testInvalidConnection() throws Exception {
        ObjectNode badConfig = MAPPER.createObjectNode();
        badConfig.put("host", "invalid-host");
        badConfig.put("port", 5432);
        badConfig.put("database", "testdb");
        badConfig.put("username", "testuser");
        badConfig.put("password", "testpass");

        PostgresSource source = new PostgresSource();
        ConnectionStatus status = source.check(badConfig);
        source.close();

        assertEquals(ConnectionStatus.Status.FAILED, status.getStatus());
        assertNotNull(status.getMessage());
    }
}
