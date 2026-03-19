package io.oneapi.admin.service;

import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service to generate and populate sample data using JavaFaker.
 * Creates realistic test data for employees, customers, products, and orders.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FakerDataService {

    private final JdbcTemplate jdbcTemplate;
    private final Faker faker = new Faker();

    // Configuration
    private static final int NUM_EMPLOYEES = 50;
    private static final int NUM_CUSTOMERS = 100;
    private static final int NUM_PRODUCTS = 200;
    private static final int NUM_ORDERS = 300;

    private static final String[] DEPARTMENTS = {
            "Engineering", "Sales", "Marketing", "HR", "Finance",
            "Operations", "IT", "Customer Service", "R&D", "Legal"
    };

    private static final String[] POSITIONS = {
            "Manager", "Senior Engineer", "Engineer", "Analyst", "Specialist",
            "Director", "Coordinator", "Associate", "Lead", "Consultant"
    };

    private static final String[] PRODUCT_CATEGORIES = {
            "Electronics", "Clothing", "Home & Garden", "Sports & Outdoors",
            "Books", "Toys", "Food & Beverage", "Health & Beauty", "Automotive"
    };

    private static final String[] ORDER_STATUSES = {
            "PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"
    };

    private static final String[] PAYMENT_METHODS = {
            "Credit Card", "Debit Card", "PayPal", "Bank Transfer", "Cash on Delivery"
    };

    /**
     * Initialize sample data when application starts.
     * Only runs if tables are empty.
     * DISABLED: Commented out due to H2 not supporting RETURNING clause
     */
    // @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeSampleData() {
        try {
            // Check if data already exists
            Long employeeCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sample_employees", Long.class);

            if (employeeCount != null && employeeCount > 0) {
                log.info("Sample data already exists. Skipping initialization.");
                return;
            }

            log.info("Starting sample data generation with JavaFaker...");

            long startTime = System.currentTimeMillis();

            generateEmployees();
            generateCustomers();
            generateProducts();
            generateOrders();

            long endTime = System.currentTimeMillis();
            log.info("Sample data generation completed in {} ms", (endTime - startTime));

            logSampleDataStats();

        } catch (Exception e) {
            log.error("Error initializing sample data", e);
        }
    }

    /**
     * Generate sample employees with realistic data
     */
    private void generateEmployees() {
        log.info("Generating {} sample employees...", NUM_EMPLOYEES);

        List<Long> employeeIds = new ArrayList<>();

        for (int i = 0; i < NUM_EMPLOYEES; i++) {
            String employeeId = String.format("EMP%05d", i + 1);
            String firstName = faker.name().firstName();
            String lastName = faker.name().lastName();
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() +
                    "@" + faker.company().name().replaceAll("[^a-zA-Z]", "").toLowerCase() + ".com";
            String phone = faker.phoneNumber().phoneNumber();
            String department = DEPARTMENTS[faker.random().nextInt(DEPARTMENTS.length)];
            String position = POSITIONS[faker.random().nextInt(POSITIONS.length)];
            BigDecimal salary = BigDecimal.valueOf(
                    faker.number().numberBetween(40000, 150000)
            ).setScale(2, RoundingMode.HALF_UP);
            Date hireDate = new Date(faker.date().past(3650, TimeUnit.DAYS).getTime());
            boolean isActive = faker.random().nextDouble() > 0.1; // 90% active

            Long id = jdbcTemplate.queryForObject(
                    "INSERT INTO sample_employees (employee_id, first_name, last_name, email, phone, " +
                            "department, position, salary, hire_date, is_active) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                            "RETURNING id",
                    Long.class,
                    employeeId, firstName, lastName, email, phone, department, position,
                    salary, hireDate, isActive
            );

            employeeIds.add(id);
        }

        // Update some employees with managers
        for (int i = 10; i < NUM_EMPLOYEES; i++) {
            Long employeeId = employeeIds.get(i);
            Long managerId = employeeIds.get(faker.random().nextInt(10)); // First 10 as managers

            jdbcTemplate.update(
                    "UPDATE sample_employees SET manager_id = ? WHERE id = ?",
                    managerId, employeeId
            );
        }

        log.info("Generated {} employees", NUM_EMPLOYEES);
    }

    /**
     * Generate sample customers with realistic data
     */
    private void generateCustomers() {
        log.info("Generating {} sample customers...", NUM_CUSTOMERS);

        for (int i = 0; i < NUM_CUSTOMERS; i++) {
            String customerId = String.format("CUST%06d", i + 1);
            String companyName = faker.company().name();
            String contactName = faker.name().fullName();
            String email = faker.internet().emailAddress();
            String phone = faker.phoneNumber().phoneNumber();
            String address = faker.address().streetAddress();
            String city = faker.address().city();
            String state = faker.address().state();
            String country = faker.address().country();
            String postalCode = faker.address().zipCode();
            BigDecimal creditLimit = BigDecimal.valueOf(
                    faker.number().numberBetween(5000, 100000)
            ).setScale(2, RoundingMode.HALF_UP);
            Date customerSince = new Date(faker.date().past(1825, TimeUnit.DAYS).getTime());
            boolean isActive = faker.random().nextDouble() > 0.05; // 95% active

            jdbcTemplate.update(
                    "INSERT INTO sample_customers (customer_id, company_name, contact_name, email, phone, " +
                            "address, city, state, country, postal_code, credit_limit, customer_since, is_active) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    customerId, companyName, contactName, email, phone, address, city, state,
                    country, postalCode, creditLimit, customerSince, isActive
            );
        }

        log.info("Generated {} customers", NUM_CUSTOMERS);
    }

    /**
     * Generate sample products with realistic data
     */
    private void generateProducts() {
        log.info("Generating {} sample products...", NUM_PRODUCTS);

        for (int i = 0; i < NUM_PRODUCTS; i++) {
            String productId = String.format("PROD%06d", i + 1);
            String productName = faker.commerce().productName();
            String category = PRODUCT_CATEGORIES[faker.random().nextInt(PRODUCT_CATEGORIES.length)];
            String description = faker.lorem().paragraph(3);
            BigDecimal unitPrice = BigDecimal.valueOf(
                    faker.number().numberBetween(5, 2000)
            ).setScale(2, RoundingMode.HALF_UP);
            int stockQuantity = faker.number().numberBetween(0, 1000);
            int reorderLevel = faker.number().numberBetween(10, 100);
            String supplier = faker.company().name();
            boolean isAvailable = stockQuantity > 0;

            jdbcTemplate.update(
                    "INSERT INTO sample_products (product_id, product_name, category, description, " +
                            "unit_price, stock_quantity, reorder_level, supplier, is_available) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    productId, productName, category, description, unitPrice, stockQuantity,
                    reorderLevel, supplier, isAvailable
            );
        }

        log.info("Generated {} products", NUM_PRODUCTS);
    }

    /**
     * Generate sample orders with order items
     */
    private void generateOrders() {
        log.info("Generating {} sample orders...", NUM_ORDERS);

        // Get all customer and product IDs
        List<Long> customerIds = jdbcTemplate.queryForList(
                "SELECT id FROM sample_customers", Long.class);
        List<Long> productIds = jdbcTemplate.queryForList(
                "SELECT id FROM sample_products", Long.class);

        for (int i = 0; i < NUM_ORDERS; i++) {
            String orderId = String.format("ORD%07d", i + 1);
            Long customerId = customerIds.get(faker.random().nextInt(customerIds.size()));
            Timestamp orderDate = new Timestamp(faker.date().past(365, TimeUnit.DAYS).getTime());
            Timestamp shipDate = new Timestamp(orderDate.getTime() +
                    faker.number().numberBetween(1, 7) * 24 * 60 * 60 * 1000L);
            LocalDate requiredDate = orderDate.toLocalDateTime().toLocalDate().plusDays(
                    faker.number().numberBetween(3, 14));
            String status = ORDER_STATUSES[faker.random().nextInt(ORDER_STATUSES.length)];
            String shippingAddress = faker.address().streetAddress();
            String shippingCity = faker.address().city();
            String shippingCountry = faker.address().country();
            String paymentMethod = PAYMENT_METHODS[faker.random().nextInt(PAYMENT_METHODS.length)];
            String notes = faker.lorem().sentence();

            // Insert order
            Long orderDbId = jdbcTemplate.queryForObject(
                    "INSERT INTO sample_orders (order_id, customer_id, order_date, ship_date, " +
                            "required_date, status, shipping_address, shipping_city, shipping_country, " +
                            "payment_method, notes) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                    Long.class,
                    orderId, customerId, orderDate, shipDate, Date.valueOf(requiredDate), status,
                    shippingAddress, shippingCity, shippingCountry, paymentMethod, notes
            );

            // Generate 1-5 order items per order
            int numItems = faker.number().numberBetween(1, 6);
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal taxAmount = BigDecimal.ZERO;

            for (int j = 0; j < numItems; j++) {
                Long productId = productIds.get(faker.random().nextInt(productIds.size()));

                // Get product price
                BigDecimal unitPrice = jdbcTemplate.queryForObject(
                        "SELECT unit_price FROM sample_products WHERE id = ?",
                        BigDecimal.class, productId
                );

                int quantity = faker.number().numberBetween(1, 10);
                BigDecimal discount = BigDecimal.valueOf(
                        faker.number().numberBetween(0, 30) / 100.0
                ).setScale(2, RoundingMode.HALF_UP);
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity))
                        .multiply(BigDecimal.ONE.subtract(discount))
                        .setScale(2, RoundingMode.HALF_UP);

                jdbcTemplate.update(
                        "INSERT INTO sample_order_items (order_id, product_id, quantity, unit_price, " +
                                "discount, line_total) VALUES (?, ?, ?, ?, ?, ?)",
                        orderDbId, productId, quantity, unitPrice, discount, lineTotal
                );

                totalAmount = totalAmount.add(lineTotal);
            }

            // Calculate tax (10%)
            taxAmount = totalAmount.multiply(BigDecimal.valueOf(0.10))
                    .setScale(2, RoundingMode.HALF_UP);

            // Update order totals
            jdbcTemplate.update(
                    "UPDATE sample_orders SET total_amount = ?, tax_amount = ? WHERE id = ?",
                    totalAmount, taxAmount, orderDbId
            );
        }

        log.info("Generated {} orders with items", NUM_ORDERS);
    }

    /**
     * Log statistics about generated data
     */
    private void logSampleDataStats() {
        log.info("=== Sample Data Statistics ===");

        Long employeeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sample_employees", Long.class);
        log.info("Employees: {}", employeeCount);

        Long customerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sample_customers", Long.class);
        log.info("Customers: {}", customerCount);

        Long productCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sample_products", Long.class);
        log.info("Products: {}", productCount);

        Long orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sample_orders", Long.class);
        log.info("Orders: {}", orderCount);

        Long orderItemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sample_order_items", Long.class);
        log.info("Order Items: {}", orderItemCount);

        log.info("==============================");
    }

    /**
     * Clear all sample data (useful for testing)
     */
    @Transactional
    public void clearSampleData() {
        log.info("Clearing all sample data...");

        jdbcTemplate.execute("DELETE FROM sample_order_items");
        jdbcTemplate.execute("DELETE FROM sample_orders");
        jdbcTemplate.execute("DELETE FROM sample_products");
        jdbcTemplate.execute("DELETE FROM sample_customers");
        jdbcTemplate.execute("DELETE FROM sample_employees");

        log.info("Sample data cleared");
    }

    /**
     * Regenerate all sample data
     */
    @Transactional
    public void regenerateSampleData() {
        clearSampleData();
        generateEmployees();
        generateCustomers();
        generateProducts();
        generateOrders();
        logSampleDataStats();
    }
}
