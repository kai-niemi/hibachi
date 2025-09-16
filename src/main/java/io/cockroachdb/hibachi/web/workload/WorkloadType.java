package io.cockroachdb.hibachi.web.workload;

import javax.sql.DataSource;

import io.cockroachdb.hibachi.repository.FullScan;
import io.cockroachdb.hibachi.repository.InsertBatch;
import io.cockroachdb.hibachi.repository.InsertOne;
import io.cockroachdb.hibachi.repository.OpenClose;
import io.cockroachdb.hibachi.repository.PointRead;
import io.cockroachdb.hibachi.repository.SelectOne;
import io.cockroachdb.hibachi.repository.SelectVersion;

public enum WorkloadType {
    open_close("Open close",
            "Open and close connections") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return new OpenClose(dataSource);
        }
    },
    singleton_insert("Singleton insert",
            "Single insert statements") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return new InsertOne(dataSource);
        }
    },
    batch_insert("Batch insert",
            "Batch of 32 insert statements") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return new InsertBatch(dataSource);
        }
    },
    point_read("Point read",
            "Single point read") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return new PointRead(dataSource, false);
        }
    },
    point_read_historical("Historical point read",
            "Single point exact staleness read") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return new PointRead(dataSource, true);
        }
    },
    full_scan("Full table scan",
            "Full table scan using order by random") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return new FullScan(dataSource);
        }
    },
    select_one("Select one query",
            "A 'select 1' statement") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return new SelectOne(dataSource);
        }
    },
    select_version("Select version query",
            "A 'select version()' statement") {
        @Override
        public Runnable startWorkload(DataSource dataSource) {
            return new SelectVersion(dataSource);
        }
    };

    private final String displayValue;

    private final String description;

    WorkloadType(String displayValue,
                 String description) {
        this.displayValue = displayValue;
        this.description = description;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public String getDescription() {
        return description;
    }

    public abstract Runnable startWorkload(DataSource dataSource);
}
