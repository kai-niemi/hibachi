package io.cockroachdb.hibachi.repository;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

public abstract class AbstractTask implements Runnable {
    protected final SampleRepository sampleRepository;

    protected final JdbcTemplate jdbcTemplate;

    protected final TransactionTemplate transactionTemplate;

    private final AtomicReference<Optional<SampleEntity>> latestEntity
            = new AtomicReference<>(Optional.empty());

    protected AbstractTask(DataSource dataSource) {
        this.sampleRepository = new JdbcSampleRepository(dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        transactionManager.setRollbackOnCommitFailure(true);
        transactionManager.setValidateExistingTransaction(false);

        this.transactionTemplate = new TransactionTemplate(transactionManager);

        initSchema(dataSource);
    }

    private void initSchema(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setCommentPrefix("--");
        populator.setIgnoreFailedDrops(true);
        populator.addScript(new ClassPathResource("db/create.sql"));

        DatabasePopulatorUtils.execute(populator, dataSource);
    }

    protected Optional<SampleEntity> findNext(boolean followerRead) {
        Optional<SampleEntity> e = latestEntity.get();
        if (e.isPresent()) {
            e = sampleRepository.findByNextId(e.get().getId(), followerRead);
        }
        if (e.isEmpty()) {
            e = sampleRepository.findFirst(followerRead);
        }
        latestEntity.set(e);
        return e;
    }
}

