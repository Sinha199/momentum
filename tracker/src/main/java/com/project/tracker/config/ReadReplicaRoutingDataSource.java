package com.project.tracker.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Picks the pool per connection: read-only transactions go to the replica, everything else
 * (writes, schema update, data.sql) to the primary.
 */
public class ReadReplicaRoutingDataSource extends AbstractRoutingDataSource {

    public enum Route { PRIMARY, REPLICA }

    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly() ? Route.REPLICA : Route.PRIMARY;
    }
}