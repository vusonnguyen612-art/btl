#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

M2_REPO="${HOME}/.m2/repository"

CP="target/classes"
CP="${CP}:${M2_REPO}/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar"
CP="${CP}:${M2_REPO}/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar"
CP="${CP}:${M2_REPO}/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
CP="${CP}:${M2_REPO}/org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar"

exec java -cp "$CP" Network.AuctionServer 8989
