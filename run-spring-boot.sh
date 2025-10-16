#!/bin/bash

# Cantaloupe Spring Boot Launcher Script
# This script helps run Cantaloupe with Spring Boot instead of the original Jetty setup

set -e

# Function to check if a port is in use
check_port() {
    local port=$1
    if lsof -i :$port > /dev/null 2>&1; then
        echo "Warning: Port $port is already in use!"
        echo "Running processes on port $port:"
        lsof -i :$port
        echo ""
        echo "You can:"
        echo "  1. Kill the process: kill \$(lsof -ti :$port)"
        echo "  2. Use a different port: SERVER_PORT=8183 $0"
        echo "  3. Configure a different port in cantaloupe.properties"
        return 1
    fi
    return 0
}

# Get the port from environment or default
SERVER_PORT=${SERVER_PORT:-8182}

# Check if cantaloupe.properties exists
if [ ! -f "cantaloupe.properties" ]; then
    echo "Creating cantaloupe.properties from sample..."
    cp cantaloupe.properties.sample cantaloupe.properties
    echo "Please edit cantaloupe.properties to configure your settings."
fi

# Check if the port is available
if ! check_port $SERVER_PORT; then
    exit 1
fi

# Build the application
echo "Building Cantaloupe with Spring Boot..."
if ! mvn clean compile jar:jar spring-boot:repackage -Dmaven.test.skip=true -q; then
    echo "Error: Build failed. Check Maven output above."
    exit 1
fi

# Find the JAR file
JAR_FILE=$(find target -name "cantaloupe-*.jar" -not -name "*-sources.jar" | head -n1)

if [ -z "$JAR_FILE" ]; then
    echo "Error: Could not find built JAR file in target directory"
    echo "Available files in target:"
    ls -la target/ || echo "Target directory does not exist"
    exit 1
fi

echo "Starting Cantaloupe Image Server with Spring Boot..."
echo "JAR: $JAR_FILE"
echo "Config: cantaloupe.properties"
echo "Port: $SERVER_PORT"
echo ""
echo "Once started, you can access:"
echo "  - Image Server: http://localhost:$SERVER_PORT/"
echo "  - Health Check: http://localhost:$SERVER_PORT/health"
echo "  - Admin Panel: http://localhost:$SERVER_PORT/admin (if enabled)"
echo "  - IIIF API: http://localhost:$SERVER_PORT/iiif/2/"
echo "  - Status: http://localhost:$SERVER_PORT/status"
echo ""

# Run with Spring Boot
java -Dcantaloupe.config=cantaloupe.properties \
     -Dspring.profiles.active=production \
     -Dserver.port=$SERVER_PORT \
     -Xmx2g \
     -jar "$JAR_FILE" "$@"
