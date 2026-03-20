#!/bin/bash

##############################################
# H2 Test Database Server Stop Script
##############################################

PID_FILE="/tmp/h2_testdb.pid"
H2_PORT=9092

echo "========================================="
echo "Stopping H2 Test Database Server"
echo "========================================="
echo ""

# Check if PID file exists
if [ -f "$PID_FILE" ]; then
    H2_PID=$(cat "$PID_FILE")

    # Check if process is running
    if ps -p $H2_PID > /dev/null 2>&1; then
        echo "Stopping H2 server (PID: $H2_PID)..."
        kill $H2_PID
        sleep 2

        # Force kill if still running
        if ps -p $H2_PID > /dev/null 2>&1; then
            echo "Force killing..."
            kill -9 $H2_PID
        fi

        rm -f "$PID_FILE"
        echo "✅ H2 server stopped successfully"
    else
        echo "⚠️  H2 server not running (stale PID file)"
        rm -f "$PID_FILE"
    fi
else
    # No PID file, check if port is in use
    if lsof -i :$H2_PORT > /dev/null 2>&1; then
        echo "⚠️  Port $H2_PORT is in use but no PID file found"
        echo "   Manually kill with: lsof -ti :$H2_PORT | xargs kill -9"
    else
        echo "ℹ️  H2 server is not running"
    fi
fi

echo ""
echo "========================================="
