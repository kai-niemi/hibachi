package io.cockroachdb.hibachi.model;

@FunctionalInterface
public interface ConfigStrategy {
    ConfigModel applySettings(ConfigModel configModel);
}
