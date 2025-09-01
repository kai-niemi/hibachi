package io.cockroachdb.hibachi.repository;

import javax.sql.DataSource;

public class SelectOne extends AbstractTask {
    public SelectOne(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void run() {
        jdbcTemplate.execute("select 1");
    }
}
