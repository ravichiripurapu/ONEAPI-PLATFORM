#!/bin/bash

# OneAPI Admin App - Quick Start Script

echo "==================================="
echo "OneAPI Admin App - Starting..."
echo "==================================="

# Set JAVA_HOME
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home

# Change to admin app directory
cd "$(dirname "$0")/oneapi-admin-app"

echo ""
echo "Building application..."
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
    echo ""
    echo "Starting application on port 8090..."
    echo ""
    echo "📍 Access points:"
    echo "   - Swagger UI:  http://localhost:8090/swagger-ui.html"
    echo "   - GraphQL:     http://localhost:8090/graphiql"
    echo "   - H2 Console:  http://localhost:8090/h2-console"
    echo "   - API Docs:    http://localhost:8090/api-docs"
    echo ""
    echo "Press Ctrl+C to stop the application"
    echo ""

    java -jar target/oneapi-admin-app-0.0.1.jar
else
    echo ""
    echo "❌ Build failed. Check the errors above."
    exit 1
fi
