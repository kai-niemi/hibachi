package io.cockroachdb.hibachi.repository;

import org.springframework.jdbc.core.JdbcTemplate;

public class PointRead extends AbstractTask {
    private final boolean followerRead;

    public PointRead(JdbcTemplate jdbcTemplate, boolean followerRead) {
        super(jdbcTemplate);
        this.followerRead = followerRead;
    }

    @Override
    public void run() {
        findNext(followerRead).ifPresent(sampleEntity -> {
        });
    }
}
