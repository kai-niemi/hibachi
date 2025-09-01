package io.cockroachdb.hibachi.repository;

import javax.sql.DataSource;

public class FullScan extends AbstractTask {
    public FullScan(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void run() {
        sampleRepository.findRandom();
    }
}
