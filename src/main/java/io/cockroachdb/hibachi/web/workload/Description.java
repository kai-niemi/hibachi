package io.cockroachdb.hibachi.web.workload;

@FunctionalInterface
public interface Description {
    String displayValue();

    default String phaseName() {
        return "";
    }
}
