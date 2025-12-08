#!/bin/bash

# Mnemocast Engine - Postgres Database Setup Script
# This script creates the database and runs the schema migration

set -e

# Configuration
DB_NAME="${POSTGRES_DB:-mnemocast}"
DB_USER="${POSTGRES_USER:-postgres}"
DB_PASSWORD="${POSTGRES_PASSWORD:-root}"
DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5432}"

SCHEMA_FILE="infra/local-dev/postgres/init.sql"

echo "=== Mnemocast Postgres Database Setup ==="
echo ""

# Check if schema file exists
if [ ! -f "$SCHEMA_FILE" ]; then
    echo "❌ Error: Schema file not found: $SCHEMA_FILE"
    exit 1
fi

echo "Database: $DB_NAME"
echo "User: $DB_USER"
echo "Host: $DB_HOST:$DB_PORT"
echo ""

# Method 1: Try with password authentication
echo "Attempting to connect with password authentication..."
export PGPASSWORD="$DB_PASSWORD"

# Check if database exists
DB_EXISTS=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -lqt 2>/dev/null | cut -d \| -f 1 | grep -w "$DB_NAME" | wc -l)

if [ "$DB_EXISTS" -eq 0 ]; then
    echo "Creating database '$DB_NAME'..."
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME;" 2>/dev/null || {
        echo "⚠️  Could not create database (might already exist or permission issue)"
    }
else
    echo "✅ Database '$DB_NAME' already exists"
fi

# Run schema migration
echo ""
echo "Running schema migration..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$SCHEMA_FILE" 2>&1

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Schema migration completed successfully!"
    echo ""
    echo "Verifying tables..."
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "\dt" 2>&1
    echo ""
    echo "✅ Setup complete!"
else
    echo ""
    echo "❌ Schema migration failed. Check the error messages above."
    echo ""
    echo "Troubleshooting:"
    echo "1. Make sure Postgres is running: sudo systemctl status postgresql"
    echo "2. Check password is correct: export POSTGRES_PASSWORD=your-password"
    echo "3. Try running manually: psql -U $DB_USER -d $DB_NAME -f $SCHEMA_FILE"
    exit 1
fi

