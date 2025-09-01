package io.cockroachdb.hibachi.web.editor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpringModel {
    @JsonProperty("datasource")
    private DataSourceModel dataSourceModel;

    public SpringModel(DataSourceModel dataSourceModel) {
        this.dataSourceModel = dataSourceModel;
    }
}
