package com.loom.server.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;

abstract class JdbcRepositorySupport {

    protected final JdbcClient jdbcClient;
    protected final JdbcJsonSupport jsonSupport;

    protected JdbcRepositorySupport(JdbcClient jdbcClient, JdbcJsonSupport jsonSupport) {
        this.jdbcClient = jdbcClient;
        this.jsonSupport = jsonSupport;
    }

    protected Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
