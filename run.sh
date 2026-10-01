#!/bin/bash

# Simple launcher for the demo

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

mvn -q clean package
java -jar "$(ls target/jooby-pac4j-demo-*.jar | head -n1)"
