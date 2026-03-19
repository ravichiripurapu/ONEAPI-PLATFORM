#!/bin/bash
# OneAPI Platform - Quick Start Script

set -e

echo "================================================================="
echo "  OneAPI Platform - Quick Start"
echo "================================================================="
echo ""

# Check Java
echo "Checking Java version..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    echo "✓ Java $JAVA_VERSION detected"
    if [ "$JAVA_VERSION" -lt 21 ]; then
        echo "⚠ Warning: Java 21 or higher recommended"
    fi
else
    echo "✗ Java not found. Please install Java 21+"
    exit 1
fi

echo ""
echo "Step 1: Building project..."
./gradlew clean build -x test

if [ $? -eq 0 ]; then
    echo "✓ Build successful!"
else
    echo "✗ Build failed"
    exit 1
fi

echo ""
echo "Step 2: Starting application..."
echo "Application will run on http://localhost:8088"
echo ""
echo "Default credentials:"
echo "  Username: admin"
echo "  Password: admin123"
echo ""
echo "Press Ctrl+C to stop"
echo ""

./gradlew :oneapi-app:bootRun
