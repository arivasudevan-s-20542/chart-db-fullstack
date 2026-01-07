package com.chartdb.catalyst;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zc.component.zcql.ZCQL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Catalyst Data Store Configuration.
 * Configures database connection for Zoho Catalyst environment.
 * 
 * Catalyst supports:
 * 1. Built-in Data Store (NoSQL-like with ZCQL)
 * 2. Cloud SQL (PostgreSQL compatible) - Recommended for this app
 */
@Configuration
@Profile("catalyst")
public class CatalystDataStoreConfig {
    
    private static final Logger LOGGER = Logger.getLogger(CatalystDataStoreConfig.class.getName());
    
    @Value("${catalyst.database.url:#{null}}")
    private String databaseUrl;
    
    @Value("${catalyst.database.username:#{null}}")
    private String databaseUsername;
    
    @Value("${catalyst.database.password:#{null}}")
    private String databasePassword;
    
    /**
     * Configure DataSource for Catalyst Cloud SQL.
     * If Cloud SQL is not configured, returns null and the app
     * will use Catalyst's built-in Data Store via ZCQL.
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        // Check if Cloud SQL is configured
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            LOGGER.info("Configuring Catalyst Cloud SQL DataSource");
            return createCloudSqlDataSource();
        }
        
        // Fall back to environment variables
        String envDbUrl = System.getenv("DATABASE_URL");
        if (envDbUrl != null && !envDbUrl.isEmpty()) {
            LOGGER.info("Configuring DataSource from environment variable");
            return createDataSourceFromEnv();
        }
        
        LOGGER.warning("No SQL database configured. Using Catalyst Data Store.");
        return createMinimalDataSource();
    }
    
    private DataSource createCloudSqlDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(databaseUrl);
        config.setUsername(databaseUsername);
        config.setPassword(databasePassword);
        config.setMaximumPoolSize(5); // Catalyst has connection limits
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        return new HikariDataSource(config);
    }
    
    private DataSource createDataSourceFromEnv() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv("DATABASE_URL"));
        config.setUsername(System.getenv("DATABASE_USERNAME"));
        config.setPassword(System.getenv("DATABASE_PASSWORD"));
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        
        return new HikariDataSource(config);
    }
    
    /**
     * Creates a minimal DataSource that will fail on first use.
     * This is used when no SQL database is configured,
     * indicating the app should use Catalyst Data Store APIs instead.
     */
    private DataSource createMinimalDataSource() {
        return new DataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                throw new SQLException("No SQL database configured. Use Catalyst Data Store APIs.");
            }
            
            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                throw new SQLException("No SQL database configured. Use Catalyst Data Store APIs.");
            }
            
            @Override
            public java.io.PrintWriter getLogWriter() { return null; }
            
            @Override
            public void setLogWriter(java.io.PrintWriter out) {}
            
            @Override
            public void setLoginTimeout(int seconds) {}
            
            @Override
            public int getLoginTimeout() { return 0; }
            
            @Override
            public java.util.logging.Logger getParentLogger() { return LOGGER; }
            
            @Override
            public <T> T unwrap(Class<T> iface) { return null; }
            
            @Override
            public boolean isWrapperFor(Class<?> iface) { return false; }
        };
    }
}
