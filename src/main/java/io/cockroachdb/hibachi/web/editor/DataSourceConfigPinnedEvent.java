package io.cockroachdb.hibachi.web.editor;

import org.springframework.context.ApplicationEvent;

public class DataSourceConfigPinnedEvent extends ApplicationEvent {
    private final ConfigModel configModel;

    public DataSourceConfigPinnedEvent(Object source, ConfigModel configModel) {
        super(source);
        this.configModel = configModel;
    }

    public ConfigModel getConfigModel() {
        return configModel;
    }
}
