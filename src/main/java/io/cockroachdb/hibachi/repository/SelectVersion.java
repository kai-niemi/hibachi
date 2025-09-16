package io.cockroachdb.hibachi.repository;

import org.springframework.jdbc.core.JdbcTemplate;

public class SelectVersion  extends AbstractTask {
    public SelectVersion(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public void run() {
        jdbcTemplate.queryForObject("select version()", String.class);
    }
}

