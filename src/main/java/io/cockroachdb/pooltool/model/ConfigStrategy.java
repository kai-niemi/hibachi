package io.cockroachdb.pooltool.model;

@FunctionalInterface
public interface ConfigStrategy {
    ConfigModel applySettings(ConfigModel configModel);
}
