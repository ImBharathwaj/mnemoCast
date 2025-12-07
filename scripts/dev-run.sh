#!/bin/bash
# Development run script for Mnemocast Engine

echo "Starting Mnemocast Engine..."
cd "$(dirname "$0")/../backend"
sbt "project engineApi" run

