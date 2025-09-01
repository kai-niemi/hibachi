package io.cockroachdb.hibachi.web.editor;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SpringModel {
    @JsonProperty("datasource")
//    @JsonIncludeProperties({"driverClassName", "url", "userName"}) // omit password
    private DataSourceModel dataSourceModel;

    public SpringModel(DataSourceModel dataSourceModel) {
        this.dataSourceModel = dataSourceModel;
    }
}
