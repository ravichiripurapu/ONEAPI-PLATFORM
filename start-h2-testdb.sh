#!/bin/bash

##############################################
# H2 Test Database Server Startup Script
# For OneAPI Platform Datasource Testing
##############################################

H2_JAR="/Users/ravi/.gradle/caches/modules-2/files-2.1/com.h2database/h2/2.2.224/7bdade27d8cd197d9b5ce9dc251f41d2edc5f7ad/h2-2.2.224.jar"
H2_PORT=9092
H2_BASE_DIR="./h2-testdb-data"
PID_FILE="/tmp/h2_testdb.pid"
LOG_FILE="/tmp/h2_testdb.log"

# Check if already running
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if ps -p $OLD_PID > /dev/null 2>&1; then
        echo "⚠️  H2 server is already running (PID: $OLD_PID)"
        echo "   To stop: kill $OLD_PID"
        exit 0
    else
        # PID file exists but process is dead
        rm -f "$PID_FILE"
    fi
fi

# Check if port is in use
if lsof -i :$H2_PORT > /dev/null 2>&1; then
    echo "❌ Port $H2_PORT is already in use!"
    echo "   Run: lsof -i :$H2_PORT to see what's using it"
    exit 1
fi

# Create base directory if it doesn't exist
mkdir -p "$H2_BASE_DIR"

echo "========================================="
echo "Starting H2 Test Database Server"
echo "========================================="
echo ""

# Start H2 TCP Server in background
nohup java -cp "$H2_JAR" org.h2.tools.Server \
  -tcp \
  -tcpAllowOthers \
  -tcpPort $H2_PORT \
  -baseDir "$H2_BASE_DIR" \
  > "$LOG_FILE" 2>&1 &

H2_PID=$!
echo $H2_PID > "$PID_FILE"

# Wait for server to start
echo "Waiting for server to start..."
sleep 3

# Verify it's running
if lsof -i :$H2_PORT > /dev/null 2>&1; then
    echo "✅ H2 Test Database Server started successfully!"
    echo ""
    echo "Server Details:"
    echo "  Port:          $H2_PORT"
    echo "  PID:           $H2_PID"
    echo "  Base Dir:      $H2_BASE_DIR"
    echo "  Log File:      $LOG_FILE"
    echo ""
    echo "Connection Info:"
    echo "  JDBC URL:      jdbc:h2:tcp://localhost:$H2_PORT/testdb"
    echo "  Host:          localhost"
    echo "  Port:          $H2_PORT"
    echo "  Database:      testdb"
    echo "  Username:      sa"
    echo "  Password:      (empty)"
    echo ""
    echo "Commands:"
    echo "  Stop Server:   kill $H2_PID"
    echo "  View Logs:     tail -f $LOG_FILE"
    echo "  Check Status:  lsof -i :$H2_PORT"
    echo ""
    echo "========================================="
else
    echo "❌ Failed to start H2 server!"
    echo "Check logs: cat $LOG_FILE"
    rm -f "$PID_FILE"
    exit 1
fi
