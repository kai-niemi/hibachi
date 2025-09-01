package io.cockroachdb.hibachi.repository;

import javax.sql.DataSource;

public class InsertOne extends AbstractTask {
    public InsertOne(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void run() {
        sampleRepository.insertSingleton();
    }
}
