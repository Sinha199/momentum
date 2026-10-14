#!/bin/bash
# Runs once, the first time the primary's data volume is created.
# Creates the replication user, allows it in pg_hba.conf, and reserves a replication slot
# so the primary keeps WAL until the replica has consumed it.
set -e
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" <<SQL
CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD '${REPLICATION_PASSWORD}';
SELECT pg_create_physical_replication_slot('replica_1');
SQL
echo "host replication replicator all scram-sha-256" >> "$PGDATA/pg_hba.conf"