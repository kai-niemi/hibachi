package io.cockroachdb.hibachi.repository;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

public class FullScan extends AbstractTask {
    public FullScan(JdbcTemplate jdbcTemplate) {

        super(jdbcTemplate);
    }

    @Override
    public void run() {
        sampleRepository.findRandom();
    }
}
