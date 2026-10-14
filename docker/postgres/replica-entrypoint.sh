#!/bin/bash
# Streaming replica: on first start, clone the primary with pg_basebackup.
# -R writes standby.signal + primary_conninfo, so Postgres starts as a read-only hot standby.
set -e
PRIMARY_PORT="${PRIMARY_PORT:-5432}"
if [ ! -s "$PGDATA/PG_VERSION" ]; then
  echo "replica: waiting for primary at $PRIMARY_HOST:$PRIMARY_PORT"
  until pg_isready -h "$PRIMARY_HOST" -p "$PRIMARY_PORT" -U replicator; do sleep 1; done
  rm -rf "$PGDATA"/*
  PGPASSWORD="$REPLICATION_PASSWORD" pg_basebackup \
    -h "$PRIMARY_HOST" -p "$PRIMARY_PORT" -U replicator -D "$PGDATA" \
    -X stream -S replica_1 -R --progress
  chmod 700 "$PGDATA"
fi
exec postgres -c hot_standby=on