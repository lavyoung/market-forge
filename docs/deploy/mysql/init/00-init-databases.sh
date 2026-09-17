#!/usr/bin/env bash

set -Eeuo pipefail

mysql --protocol=socket \
    -uroot \
    -p"${MYSQL_ROOT_PASSWORD}" <<EOSQL
CREATE DATABASE IF NOT EXISTS \`market-forge-001\`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS \`market-forge-002\`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

GRANT ALL PRIVILEGES ON \`market-forge-001\`.* TO '${MYSQL_USER}'@'%';
GRANT ALL PRIVILEGES ON \`market-forge-002\`.* TO '${MYSQL_USER}'@'%';

FLUSH PRIVILEGES;
EOSQL