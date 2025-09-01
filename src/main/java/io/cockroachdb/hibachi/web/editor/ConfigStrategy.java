package io.cockroachdb.hibachi.web.editor;

@FunctionalInterface
public interface ConfigStrategy {
    ConfigModel applySettings(ConfigModel configModel);
}
