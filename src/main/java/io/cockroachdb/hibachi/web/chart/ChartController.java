package io.cockroachdb.hibachi.web.chart;

import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.metadata.HikariDataSourcePoolMetadata;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.zaxxer.hikari.HikariDataSource;

import io.cockroachdb.hibachi.web.editor.DataSourceCreatedEvent;
import io.cockroachdb.hibachi.web.common.MessagePublisher;
import io.cockroachdb.hibachi.web.common.TopicName;
import io.cockroachdb.hibachi.web.common.WebController;
import io.cockroachdb.hibachi.web.workload.WorkloadManager;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Chart JS data paint callback methods.
 */
@WebController
@RequestMapping(value = "/chart")
public class ChartController {
    @Autowired
    @Qualifier("threadPoolTimeSeries")
    private TimeSeries threadPoolTimeSeries;

    @Autowired
    @Qualifier("cpuTimeSeries")
    private TimeSeries cpuTimeSeries;

    @Autowired
    @Qualifier("memoryTimeSeries")
    private TimeSeries memoryTimeSeries;

    @Autowired
    @Qualifier("workloadTimeSeries")
    private TimeSeries workloadTimeSeries;

    private final Map<String, TimeSeries> connectionPoolTimeSeries = new HashMap<>();

    @Autowired
    private WorkloadManager workloadManager;

    @Autowired
    private MessagePublisher messagePublisher;

    @Autowired
    private MeterRegistry meterRegistry;

    private final Duration samplePeriod = Duration.ofMinutes(5);

    @Scheduled(fixedRate = 5, initialDelay = 1, timeUnit = TimeUnit.SECONDS)
    public void updateCharts() {
        messagePublisher.convertAndSend(TopicName.CHART_POOL_UPDATE, null);
        messagePublisher.convertAndSend(TopicName.CHART_VM_UPDATE, null);
        messagePublisher.convertAndSend(TopicName.CHART_WORKLOAD_UPDATE, null);
    }

    @Scheduled(fixedRate = 5, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
    public void updateDataPoints() {
        workloadManager.snapshotDataPoints(samplePeriod);
        threadPoolTimeSeries.snapshotDataPoints(samplePeriod);
        cpuTimeSeries.snapshotDataPoints(samplePeriod);
        memoryTimeSeries.snapshotDataPoints(samplePeriod);
        workloadTimeSeries.snapshotDataPoints(samplePeriod);
        connectionPoolTimeSeries.values()
                .forEach(timeSeries -> timeSeries.snapshotDataPoints(samplePeriod));
    }

    @EventListener
    public void handle(DataSourceCreatedEvent event) {
        try {
            HikariDataSource dataSource = event.getDataSource().unwrap(HikariDataSource.class);
            Objects.requireNonNull(dataSource.getPoolName(), "pool name is required");
            connectionPoolTimeSeries.put(dataSource.getPoolName().toLowerCase(), registerDataSource(dataSource));
        } catch (SQLException e) {
            throw new ApplicationContextException("", e);
        }
    }

    private TimeSeries registerDataSource(HikariDataSource dataSource) {
        final HikariDataSourcePoolMetadata metadata = new HikariDataSourcePoolMetadata(dataSource);

        Gauge.builder("hibachi.pool.connections.active", metadata, HikariDataSourcePoolMetadata::getActive)
                .tag("name", dataSource.getPoolName())
                .strongReference(true)
                .description("Active connections")
                .register(meterRegistry);
        Gauge.builder("hibachi.pool.connections.idle", metadata, HikariDataSourcePoolMetadata::getIdle)
                .tag("name", dataSource.getPoolName())
                .strongReference(true)
                .description("Idle connections")
                .register(meterRegistry);
        Gauge.builder("hibachi.pool.connections.min", metadata, HikariDataSourcePoolMetadata::getMin)
                .tag("name", dataSource.getPoolName())
                .strongReference(true)
                .description("Min connections")
                .register(meterRegistry);
        Gauge.builder("hibachi.pool.connections.max", metadata, HikariDataSourcePoolMetadata::getMax)
                .tag("name", dataSource.getPoolName())
                .strongReference(true)
                .description("Max connections")
                .register(meterRegistry);

        return new TimeSeries(meterRegistry, List.of(
                        meterRegistry.find("hibachi.pool.connections.active").tag("name", dataSource.getPoolName()),
                        meterRegistry.find("hibachi.pool.connections.idle").tag("name", dataSource.getPoolName()),
                        meterRegistry.find("hibachi.pool.connections.min").tag("name", dataSource.getPoolName()),
                        meterRegistry.find("hibachi.pool.connections.max").tag("name", dataSource.getPoolName())
        ));
    }

    @GetMapping("/pool")
    public Callable<String> getPoolChartsPage(Model model) {
        return () -> "pool-charts";
    }

    @GetMapping(value = "/pool/data-points/{name}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getConnectionPoolDataPoints(@PathVariable("name") String name) {
        if (connectionPoolTimeSeries.containsKey(name)) {
            return connectionPoolTimeSeries.get(name).getDataPoints();
        }
        return List.of();
    }

    @GetMapping("/vm")
    public Callable<String> getVMChartsPage(Model model) {
        return () -> "vm-charts";
    }

    @GetMapping(value = "/vm/thread/data-points",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getThreadPoolDataPoints() {
        return threadPoolTimeSeries.getDataPoints();
    }

    @GetMapping(value = "/vm/cpu/data-points",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getCpuDataPoints() {
        return cpuTimeSeries.getDataPoints();
    }

    @GetMapping(value = "/vm/mem/data-points",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getMemDataPoints() {
        return memoryTimeSeries.getDataPoints();
    }

    @GetMapping("/workload")
    public Callable<String> getWorkloadChartsPage(Model model) {
        return () -> "workload-charts";
    }

    @GetMapping(value = "/workload/data-points",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getWorkloadDataPoints() {
        return workloadTimeSeries.getDataPoints();
    }

    @GetMapping(value = "/workload/data-points/p99",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getWorkloadDataPointsP99(Pageable page) {
        return workloadManager.getDataPoints(Metrics::getP99, page);
    }

    @GetMapping(value = "/workload/data-points/p999",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getWorkloadDataPointsP999(Pageable page) {
        return workloadManager.getDataPoints(Metrics::getP999, page);
    }

    @GetMapping(value = "/workload/data-points/tps",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getWorkloadDataPointsTPS(Pageable page) {
        return workloadManager.getDataPoints(Metrics::getOpsPerSec, page);
    }
}
