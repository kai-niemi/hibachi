package io.cockroachdb.pooltool.workload.repository;

import org.springframework.jdbc.core.JdbcTemplate;

public class FullScan extends AbstractWorkload {
    public FullScan(JdbcTemplate jdbcTemplate) {

        super(jdbcTemplate);
    }

    @Override
    public void run() {
        sampleRepository.findRandom();
    }
}
