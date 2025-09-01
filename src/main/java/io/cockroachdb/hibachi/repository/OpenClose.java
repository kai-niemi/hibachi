package io.cockroachdb.hibachi.repository;

import javax.sql.DataSource;

import org.springframework.jdbc.core.ConnectionCallback;

public class OpenClose extends AbstractTask {
    public OpenClose(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void run() {
        jdbcTemplate.execute((ConnectionCallback<Object>) conn -> null);
    }
}
