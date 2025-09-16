package io.cockroachdb.hibachi.repository;

import org.springframework.jdbc.core.JdbcTemplate;

public class SelectOne extends AbstractTask {
    public SelectOne(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public void run() {
        jdbcTemplate.execute("select 1");
    }
}
