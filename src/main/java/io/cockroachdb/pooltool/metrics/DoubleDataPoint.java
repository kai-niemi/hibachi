package io.cockroachdb.pooltool.metrics;

import java.time.Instant;

public class DoubleDataPoint extends DataPoint<String, Double> {
    public DoubleDataPoint(Instant instant) {
        super(instant);
    }
}

