package io.cockroachdb.hibachi.repository;

import javax.sql.DataSource;

public class InsertBatch extends AbstractTask {
    public InsertBatch(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void run() {
        sampleRepository.insertBatch(32);
    }
}

