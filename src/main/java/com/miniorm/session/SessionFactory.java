package com.miniorm.session;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class SessionFactory {
    private final DataSource dataSource;

    public SessionFactory(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        this.dataSource = new HikariDataSource(config);
    }

    public MiniSession openSession() throws Exception {
        return new MiniSession(dataSource);
    }
}