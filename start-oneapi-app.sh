#!/bin/bash

# OneAPI Unified App - Quick Start Script
# This script builds and starts the OneAPI unified application

set -e

echo "========================================="
echo "OneAPI Unified App - Quick Start"
echo "========================================="
echo ""

# Set Java Home
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
echo "✓ Using Java: $JAVA_HOME"
echo ""

# Build the application
echo "Building oneapi-app..."
mvn clean install -DskipTests -pl oneapi-app -am

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Build successful!"
    echo ""
    echo "========================================="
    echo "Starting OneAPI Unified Application..."
    echo "========================================="
    echo ""
    echo "Access URLs:"
    echo "  Application:  http://localhost:8080"
    echo "  Swagger UI:   http://localhost:8080/swagger-ui.html"
    echo "  GraphQL:      http://localhost:8080/graphiql"
    echo "  H2 Console:   http://localhost:8080/h2-console"
    echo ""
    echo "Press Ctrl+C to stop"
    echo "========================================="
    echo ""

    # Run the application
    cd oneapi-app
    mvn spring-boot:run
else
    echo ""
    echo "✗ Build failed. Please check the errors above."
    exit 1
fi
