package io.cockroachdb.hibachi.repository;

import javax.sql.DataSource;

public class UpdateOne extends AbstractTask {
    public UpdateOne(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void run() {
        findNext(false)
                .ifPresent(sampleRepository::updateSingleton);
    }
}
