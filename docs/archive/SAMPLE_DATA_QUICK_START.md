# Sample Data Quick Start

## What Was Created

### 1. Liquibase Database Migrations
- **5 Sample Tables**: employees, customers, products, orders, order_items
- **Automatic Schema Creation**: Tables created on application startup
- **Proper Relationships**: Foreign keys, indexes, and constraints

### 2. Automatic Data Generation
- **50 Employees** with departments, positions, salaries, managers
- **100 Customers** with companies, addresses, credit limits
- **200 Products** with categories, prices, stock levels
- **300 Orders** with order items, totals, shipping info
- **Realistic Data**: Using JavaFaker library for authentic test data

### 3. Management API
- **Regenerate Data**: `POST /api/sample-data/regenerate`
- **Clear Data**: `DELETE /api/sample-data/clear`
- **Get Info**: `GET /api/sample-data/info`

## Quick Test

### Step 1: Start the Application
```bash
cd /Users/ravi/JavaProjects/oneapi-platform
./start-admin-app.sh
```

### Step 2: Wait for Data Generation
Watch the console for:
```
INFO - Starting sample data generation with JavaFaker...
INFO - Generated 50 employees
INFO - Generated 100 customers
INFO - Generated 200 products
INFO - Generated 300 orders with items
INFO - Sample data generation completed
```

### Step 3: Connect to H2 Console
1. Open: http://localhost:8090/h2-console
2. Settings:
   - JDBC URL: `jdbc:h2:file:./data/oneapi-admin`
   - Username: `sa`
   - Password: (empty)
3. Click "Connect"

### Step 4: Query Sample Data
```sql
-- View all sample tables
SELECT * FROM sample_employees LIMIT 10;
SELECT * FROM sample_customers LIMIT 10;
SELECT * FROM sample_products LIMIT 10;
SELECT * FROM sample_orders LIMIT 10;

-- Complex query: Orders with customer and product details
SELECT
    o.order_id,
    o.order_date,
    c.company_name,
    p.product_name,
    oi.quantity,
    oi.line_total
FROM sample_orders o
JOIN sample_customers c ON o.customer_id = c.id
JOIN sample_order_items oi ON o.id = oi.order_id
JOIN sample_products p ON oi.product_id = p.id
LIMIT 20;
```

### Step 5: Test OneAPI Endpoints

#### Create a Database Connection
```bash
curl -X POST http://localhost:8090/api/connections \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sample H2 DB",
    "type": "H2",
    "host": "localhost",
    "port": 9092,
    "database": "./data/oneapi-admin",
    "username": "sa",
    "password": ""
  }'
```

#### Query via OneAPI
Use the connection ID from above response to query:
```bash
curl -X POST http://localhost:8090/api/query/execute \
  -H "Content-Type: application/json" \
  -d '{
    "connectionId": 1,
    "sql": "SELECT * FROM sample_employees WHERE department = '\''Engineering'\''"
  }'
```

### Step 6: Use Swagger UI
1. Open: http://localhost:8090/swagger-ui.html
2. Navigate to "Sample Data" section
3. Try the endpoints:
   - Get sample data info
   - Regenerate sample data
   - Clear sample data

## Sample Queries for Testing

### Basic Queries
```sql
-- Employees by department
SELECT department, COUNT(*) as count
FROM sample_employees
GROUP BY department;

-- Products by category
SELECT category, AVG(unit_price) as avg_price
FROM sample_products
GROUP BY category;

-- Orders by status
SELECT status, COUNT(*) as count, SUM(total_amount) as total
FROM sample_orders
GROUP BY status;
```

### Advanced Queries
```sql
-- Top customers by order value
SELECT
    c.company_name,
    c.contact_name,
    COUNT(o.id) as order_count,
    SUM(o.total_amount) as total_spent
FROM sample_customers c
JOIN sample_orders o ON c.id = o.customer_id
GROUP BY c.id, c.company_name, c.contact_name
ORDER BY total_spent DESC
LIMIT 10;

-- Best selling products
SELECT
    p.product_name,
    p.category,
    SUM(oi.quantity) as units_sold,
    SUM(oi.line_total) as revenue
FROM sample_products p
JOIN sample_order_items oi ON p.id = oi.product_id
GROUP BY p.id, p.product_name, p.category
ORDER BY revenue DESC
LIMIT 10;

-- Employee hierarchy
SELECT
    e.employee_id,
    e.first_name || ' ' || e.last_name as employee_name,
    e.position,
    m.first_name || ' ' || m.last_name as manager_name
FROM sample_employees e
LEFT JOIN sample_employees m ON e.manager_id = m.id
WHERE e.is_active = true;
```

## Postman Testing

### Import Collection
1. Open Postman
2. Import: `OneAPI-Platform-Postman-Collection.json`
3. Look for "Sample Data" folder
4. Run requests to test the API

### Key Requests
- **Regenerate Data**: Clears and recreates all sample data
- **Get Info**: Shows table names and configuration
- **Clear Data**: Removes all sample records

## Data Volume

| Table | Records | Features |
|-------|---------|----------|
| Employees | 50 | 10 departments, manager relationships |
| Customers | 100 | Companies, addresses, credit limits |
| Products | 200 | 9 categories, pricing, stock |
| Orders | 300 | 5 statuses, customer links |
| Order Items | ~900 | 1-5 items per order |

## Troubleshooting

### Data Not Generated?
```bash
# Check if tables have data
# In H2 Console:
SELECT COUNT(*) FROM sample_employees;

# If 0, manually regenerate:
curl -X POST http://localhost:8090/api/sample-data/regenerate
```

### Want Fresh Data?
```bash
# Option 1: Use API
curl -X POST http://localhost:8090/api/sample-data/regenerate

# Option 2: Delete database file and restart
rm -rf data/
./start-admin-app.sh
```

### Tables Not Created?
Check Liquibase logs on startup:
```
INFO - Liquibase: Running Changelog: db/changelog/db.changelog-master.xml
INFO - Liquibase: Successfully acquired change log lock
INFO - Liquibase: Creating database history table with name: DATABASECHANGELOG
```

## Configuration Files

| File | Purpose |
|------|---------|
| `db/changelog/db.changelog-master.xml` | Liquibase master changelog |
| `db/changelog/changes/01-create-sample-tables.xml` | Table schemas |
| `FakerDataService.java` | Data generation logic |
| `SampleDataController.java` | REST API endpoints |
| `application.yml` | Liquibase configuration |

## What's Next?

1. ✅ **Data Generated**: Sample tables populated automatically
2. ✅ **Ready to Query**: Use H2 Console or OneAPI endpoints
3. ✅ **API Available**: Manage data via REST API
4. 🎯 **Start Testing**: Use sample data to test OneAPI features
5. 🎯 **Build Features**: Sample DB ready for development

## Full Documentation

For complete details, see: [LIQUIBASE_SAMPLE_DATA_GUIDE.md](LIQUIBASE_SAMPLE_DATA_GUIDE.md)
