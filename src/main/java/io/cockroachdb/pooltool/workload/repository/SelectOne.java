package io.cockroachdb.pooltool.workload.repository;

import org.springframework.jdbc.core.JdbcTemplate;

public class SelectOne extends AbstractWorkload {
    public SelectOne(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public void run() {
        jdbcTemplate.execute("select 1");
    }
}
