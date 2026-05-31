#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

export BTL_LAUNCHED_BY_MAVEN=true

if command -v mvn &>/dev/null; then
    exec mvn javafx:run "-Djavafx.mainClass=Launch"
else
    echo "Maven (mvn) not found in PATH. Install Maven or add it to PATH."
    exit 1
fi
