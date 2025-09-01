package io.cockroachdb.hibachi.web.workload;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import io.cockroachdb.hibachi.repository.DeleteOne;
import io.cockroachdb.hibachi.repository.FullScan;
import io.cockroachdb.hibachi.repository.InsertBatch;
import io.cockroachdb.hibachi.repository.InsertOne;
import io.cockroachdb.hibachi.repository.PointRead;
import io.cockroachdb.hibachi.repository.SelectOne;
import io.cockroachdb.hibachi.repository.SelectVersion;
import io.cockroachdb.hibachi.repository.UpdateOne;

public enum WorkloadType {
    open_close("Open and close",
            false,
            true,
            "Open and close connections") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return new InsertOne(dataSource);
        }
    },
    singleton_insert("Singleton insert",
            false,
            true,
            "Single insert statements") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return new InsertOne(dataSource);
        }
    },
    batch_insert("Batch insert",
            false,
            true,
            "Batch insert statements of 32 items") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return new InsertBatch(dataSource);
        }
    },
    point_read_update("Point read and update",
            true,
            true,
            "Point lookup read followed by an update") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return new UpdateOne(dataSource);
        }
    },
    point_read_delete("Point read and delete",
            true,
            true,
            "Point lookup read followed by a delete") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return new DeleteOne(dataSource);
        }
    },
    point_read("Point read",
            false,
            true,
            "Single point lookup read") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return new PointRead(dataSource, false);
        }
    },
    point_read_historical("Point read historical",
            false,
            true,
            "Single point lookup exact staleness read (follower)") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return new PointRead(dataSource, true);
        }
    },
    full_scan("Full table scan",
            false,
            true,
            "A full table scan using order by random") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return new FullScan(dataSource);
        }
    },
    select_one("Select one",
            false,
            true,
            "A 'select 1' statement") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return new SelectOne(dataSource);
        }
    },
    select_version("Select version",
            false,
            true,
            "A 'select version()' statement") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return new SelectVersion(dataSource);
        }
    },
    random_wait("Random wait",
            false,
            false,
            "Random waits with 5% chance of outliers") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return () -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                try {
                    if (random.nextDouble(1.0) > 0.95) {
                        TimeUnit.MILLISECONDS.sleep(random.nextLong(500, 2000));
                    } else {
                        TimeUnit.MILLISECONDS.sleep(random.nextLong(0, 10));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };
        }
    },
    fixed_wait("Fixed wait",
            false,
            false,
            "Fixed waits of 500ms") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return () -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };
        }
    };

    private final String displayValue;

    private final boolean explicit;

    private final boolean requiresDataSource;

    private final String description;

    WorkloadType(String displayValue,
                 boolean explicit,
                 boolean requiresDataSource,
                 String description) {
        this.displayValue = displayValue;
        this.explicit = explicit;
        this.requiresDataSource = requiresDataSource;
        this.description = description;
    }

    public boolean isExplicit() {
        return explicit;
    }

    public boolean isRequiresDataSource() {
        return requiresDataSource;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public String getDescription() {
        return description;
    }

    public abstract Runnable startWorkload(DataSource dataSource);
}
