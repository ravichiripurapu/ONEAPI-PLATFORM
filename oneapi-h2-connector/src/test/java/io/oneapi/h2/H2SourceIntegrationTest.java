package io.oneapi.h2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.oneapi.h2.source.H2Source;
import io.oneapi.sdk.model.*;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for H2Source using in-memory H2 database.
 */
class H2SourceIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DATABASE_NAME = "testdb";
    private static final String JDBC_URL = "jdbc:h2:mem:" + DATABASE_NAME + ";DB_CLOSE_DELAY=-1";
    private static final String USERNAME = "sa";
    private static final String PASSWORD = "";

    private static H2Source source;
    private static JsonNode config;
    private static Connection keepAliveConn; // Keep connection open to prevent DB cleanup

    @BeforeAll
    static void setUp() throws Exception {
        // Keep a connection open to prevent H2 from destroying the in-memory database
        keepAliveConn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);

        // Create test tables
        try (Statement stmt = keepAliveConn.createStatement()) {

            // Create USERS table (uppercase to match H2 default)
            stmt.execute("DROP TABLE IF EXISTS USERS");
            stmt.execute("""
                CREATE TABLE USERS (
                    ID BIGINT PRIMARY KEY,
                    USERNAME VARCHAR(255) NOT NULL,
                    EMAIL VARCHAR(255),
                    CREATED_AT TIMESTAMP,
                    IS_ACTIVE BOOLEAN
                )
            """);

            // Insert test data
            stmt.execute("""
                INSERT INTO USERS (ID, USERNAME, EMAIL, CREATED_AT, IS_ACTIVE) VALUES
                (1, 'alice', 'alice@example.com', TIMESTAMP '2024-01-01 10:00:00', TRUE),
                (2, 'bob', 'bob@example.com', TIMESTAMP '2024-01-02 11:00:00', TRUE),
                (3, 'charlie', 'charlie@example.com', TIMESTAMP '2024-01-03 12:00:00', FALSE)
            """);

            // Create PRODUCTS table (uppercase to match H2 default)
            stmt.execute("DROP TABLE IF EXISTS PRODUCTS");
            stmt.execute("""
                CREATE TABLE PRODUCTS (
                    ID BIGINT PRIMARY KEY,
                    NAME VARCHAR(255) NOT NULL,
                    PRICE DECIMAL(10, 2),
                    STOCK_QUANTITY INTEGER,
                    CREATED_AT TIMESTAMP
                )
            """);

            stmt.execute("""
                INSERT INTO PRODUCTS (ID, NAME, PRICE, STOCK_QUANTITY, CREATED_AT) VALUES
                (101, 'Laptop', 999.99, 50, TIMESTAMP '2024-01-01 10:00:00'),
                (102, 'Mouse', 29.99, 200, TIMESTAMP '2024-01-02 11:00:00')
            """);
        }

        // Create H2Source instance
        source = new H2Source();

        // Create config - use direct JDBC URL to ensure same database instance
        ObjectNode configNode = MAPPER.createObjectNode();
        configNode.put("jdbcUrl", JDBC_URL);
        configNode.put("username", USERNAME);
        configNode.put("password", PASSWORD);
        config = configNode;
    }

    @AfterAll
    static void tearDownAll() throws Exception {
        if (source != null) {
            source.close();
        }

        // Clean up H2 database
        if (keepAliveConn != null && !keepAliveConn.isClosed()) {
            try (Statement stmt = keepAliveConn.createStatement()) {
                stmt.execute("DROP ALL OBJECTS");
            }
            keepAliveConn.close();
        }
    }

    @Test
    void testConnectionCheck() throws Exception {
        ConnectionStatus status = source.check(config);

        assertNotNull(status);
        assertEquals(ConnectionStatus.Status.SUCCEEDED, status.getStatus());
        assertNotNull(status.getMessage());
    }

    @Test
    void testConnectionCheckWithInvalidConfig() throws Exception {
        ObjectNode invalidConfig = MAPPER.createObjectNode();
        invalidConfig.put("jdbcUrl", "jdbc:h2:mem:nonexistent");
        invalidConfig.put("username", "invalid");
        invalidConfig.put("password", "wrong");

        ConnectionStatus status = source.check(invalidConfig);

        assertNotNull(status);
        assertEquals(ConnectionStatus.Status.FAILED, status.getStatus());
    }

    @Test
    void testDiscoverCatalog() throws Exception {
        Domain catalog = source.discover(config);

        assertNotNull(catalog);
        assertNotNull(catalog.getEntities());
        assertEquals(2, catalog.getEntities().size());

        // Find users table
        DataEntity usersEntity = catalog.getEntities().stream()
                .filter(e -> "USERS".equals(e.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(usersEntity, "Should find USERS table");
        assertEquals("PUBLIC", usersEntity.getNamespace());
        assertNotNull(usersEntity.getIncrementalFields());
        assertFalse(usersEntity.getIncrementalFields().isEmpty(), "Should have incremental fields");

        // Find products table
        DataEntity productsEntity = catalog.getEntities().stream()
                .filter(e -> "PRODUCTS".equals(e.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(productsEntity, "Should find PRODUCTS table");
        assertEquals("PUBLIC", productsEntity.getNamespace());
    }

    @Test
    void testReadData() throws Exception {
        // Create catalog with users table
        DataEntity usersEntity = new DataEntity();
        usersEntity.setName("USERS");
        usersEntity.setNamespace("PUBLIC");

        Domain catalog = new Domain(List.of(usersEntity));
        State state = new State();

        EntityRecordIterator<EntityRecord> iterator = source.read(config, catalog, state);

        List<EntityRecord> records = new ArrayList<>();
        while (iterator.hasNext()) {
            records.add(iterator.next());
        }
        iterator.close();

        assertEquals(3, records.size(), "Should read 3 users");

        EntityRecord firstRecord = records.get(0);
        assertNotNull(firstRecord.getData());
        assertTrue(firstRecord.getData().containsKey("ID"));
        assertTrue(firstRecord.getData().containsKey("USERNAME"));
        assertTrue(firstRecord.getData().containsKey("EMAIL"));
        assertTrue(firstRecord.getData().containsKey("IS_ACTIVE"));
    }

}
