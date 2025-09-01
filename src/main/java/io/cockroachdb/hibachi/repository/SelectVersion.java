package io.cockroachdb.hibachi.repository;

import javax.sql.DataSource;

public class SelectVersion  extends AbstractTask {
    public SelectVersion(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void run() {
        jdbcTemplate.queryForObject("select version()", String.class);
    }
}

