package io.cockroachdb.hibachi.web.editor.model;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.zaxxer.hikari.HikariConfig;

@JsonIncludeProperties(value = {
        "maximumPoolSize", "minimumIdle",
        "connectionTimeout", "validationTimeout", "idleTimeout", "maxLifetime",
        "keepaliveTime", "initializationFailTimeout",
        "transactionIsolation", "readOnly", "autoCommit", "dataSourceProperties"})
@JsonPropertyOrder(value = {
        "maximumPoolSize", "minimumIdle",
        "connectionTimeout", "validationTimeout", "idleTimeout", "maxLifetime",
        "keepaliveTime", "initializationFailTimeout",
        "transactionIsolation", "readOnly", "autoCommit", "dataSourceProperties"})
public class HikariConfigModel extends HikariConfig {

}
