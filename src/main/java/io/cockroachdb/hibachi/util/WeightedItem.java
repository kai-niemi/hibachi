package io.cockroachdb.hibachi.util;

@FunctionalInterface
public interface WeightedItem {
    double getWeight();
}
