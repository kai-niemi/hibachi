package io.cockroachdb.hibachi.repository;

import javax.sql.DataSource;

public class UpdateOne extends AbstractTask {
    public UpdateOne(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void run() {
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            findNext(false)
                    .ifPresent(sampleRepository::updateSingleton);
        });
    }
}
