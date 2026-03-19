
# Start application
cd /Users/ravi/JavaProjects/oneapi-platform/oneapi-app
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
mvn spring-boot:run

# In another terminal, run tests
chmod +x /tmp/COMPREHENSIVE_UAT_TESTS.sh
bash /tmp/COMPREHENSIVE_UAT_TESTS.sh | tee /tmp/uat-results.txt
Option 2: Postman Collection
Import /tmp/OneAPI-Platform-Postman-Collection.json into Postman
Run "01 - Authentication > Login as Admin" first
Execute remaining requests in order
JWT tokens are automatically captured and used
5. Test Credentials
Username	Password	Role
admin	admin123	ROLE_ADMIN, ROLE_USER
john	admin123	ROLE_USER
jane	admin123	ROLE_USER
6. Key Test Scenarios Covered
Authentication - Login for all user types
Datasource Management - CRUD operations with renamed endpoints
Catalog Management - Create catalogs linked to datasources via connection_id
Metadata Discovery - Discover schemas, tables, columns from H2 database
Query Execution - Create sessions and execute SQL queries
Saved Queries - Save and execute queries linked to datasources
Reports - Create and execute reports
Schedules - Schedule reports for automated execution
Permissions - Grant database, table, and column permissions
Data Export - Export to CSV and JSON formats
User Preferences - Manage user settings
Health Checks - Monitor application health
7. Files Generated
File	Location	Purpose
UAT Test Script	/tmp/COMPREHENSIVE_UAT_TESTS.sh	Automated endpoint testing
Postman Collection	/tmp/OneAPI-Platform-Postman-Collection.json	Manual/automated API testing
Documentation	/tmp/UAT-Testing-Documentation.md	Complete UAT guide
8. Next Steps
Start the application:
cd /Users/ravi/JavaProjects/oneapi-platform/oneapi-app
mvn spring-boot:run
Run UAT tests:
bash /tmp/COMPREHENSIVE_UAT_TESTS.sh
Import Postman collection for manual testing
Review results and address any failures
All deliverables are ready for UAT testing. The application successfully builds, the DatabaseConnection to Datasource renaming is complete, and comprehensive test coverage has been provided through both automated scripts and Postman collections.