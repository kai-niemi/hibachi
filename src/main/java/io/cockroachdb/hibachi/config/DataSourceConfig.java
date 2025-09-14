package io.cockroachdb.hibachi.config;

import java.util.function.Function;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Role;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;

import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

import io.cockroachdb.hibachi.web.editor.model.DataSourceModel;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class DataSourceConfig implements BeanClassLoaderAware {
    public static final String SQL_TRACE_LOGGER = "io.cockroachdb.hibachi.SQL_TRACE";

    private final Logger logger = LoggerFactory.getLogger(SQL_TRACE_LOGGER);

    private ClassLoader classLoader;

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Autowired
    private MeterRegistry registry;

    @Bean
    @Lazy
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Function<DataSourceModel, ClosableDataSource> dataSourceFactory() {
        return model -> {
            HikariConfig config = model.getHikariConfig();
            config.setJdbcUrl(model.getUrl());
            config.setUsername(model.getUserName());
            config.setPassword(model.getPassword());

            config.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(registry));

            HikariDataSource dataSource = DataSourceBuilder.create(classLoader)
                    .type(HikariDataSource.class)
                    .driverClassName(model.getDriverClassName())
                    .url(model.getUrl())
                    .username(model.getUserName())
                    .password(model.getPassword())
                    .build();

            config.copyStateTo(dataSource);

            return new ClosableDataSource(
                    model.isTraceLogging() ? loggingProxyDataSource(dataSource) : dataSource);
        };
    }

    private DataSource loggingProxyDataSource(DataSource dataSource) {
        DefaultQueryLogEntryCreator creator = new DefaultQueryLogEntryCreator();
        creator.setMultiline(true);

        SLF4JQueryLoggingListener listener = new SLF4JQueryLoggingListener();
        listener.setLogger(logger);
        listener.setLogLevel(SLF4JLogLevel.DEBUG);
        listener.setQueryLogEntryCreator(creator);
        listener.setWriteConnectionId(true);

        return ProxyDataSourceBuilder
                .create(dataSource)
                .name("SQL-Trace")
                .listener(listener)
                .asJson()
                .build();
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }
}
