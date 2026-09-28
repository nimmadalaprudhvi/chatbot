#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/backend"
echo "Starting invoke.ai Spring Boot chatbot on http://localhost:8000"
command -v mvn >/dev/null || { echo "Maven 3.9+ is required."; exit 1; }
mvn spring-boot:run
