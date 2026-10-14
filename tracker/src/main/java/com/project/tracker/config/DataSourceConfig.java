package com.project.tracker.config;

import com.project.tracker.config.ReadReplicaRoutingDataSource.Route;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Two Hikari pools (primary + replica) behind one routing DataSource.
 *
 * Why the LazyConnectionDataSourceProxy: Spring marks a transaction read-only only after the
 * transaction manager has begun it, and Hibernate grabs a connection while beginning. Without
 * the proxy the routing decision is made too early and every read lands on the primary.
 * The proxy hands out a placeholder and fetches the real connection on the first SQL statement,
 * when the read-only flag is already set.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource primaryDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @ConfigurationProperties("app.datasource.replica.hikari")
    public HikariDataSource replicaDataSource(DataSourceProperties properties,
                                              @Value("${app.datasource.replica.url:}") String replicaUrl) {
        HikariDataSource ds = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        if (StringUtils.hasText(replicaUrl)) {
            ds.setJdbcUrl(replicaUrl);
        }
        return ds;
    }

    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("primaryDataSource") DataSource primary,
                                 @Qualifier("replicaDataSource") DataSource replica) {
        ReadReplicaRoutingDataSource routing = new ReadReplicaRoutingDataSource();
        routing.setTargetDataSources(Map.of(Route.PRIMARY, primary, Route.REPLICA, replica));
        routing.setDefaultTargetDataSource(primary);
        routing.afterPropertiesSet();
        return new LazyConnectionDataSourceProxy(routing);
    }
}