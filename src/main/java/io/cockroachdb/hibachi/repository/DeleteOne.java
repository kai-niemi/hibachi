package io.cockroachdb.hibachi.repository;

import javax.sql.DataSource;

public class DeleteOne extends AbstractTask {
    public DeleteOne(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void run() {
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            findNext(false)
                    .ifPresent(profileEntity -> sampleRepository.deleteById(profileEntity.getId()));
        });
    }
}
