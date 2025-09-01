package io.cockroachdb.hibachi.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import io.cockroachdb.hibachi.web.chart.TimeSeries;
import io.cockroachdb.hibachi.web.workload.WorkloadModel;
import io.cockroachdb.hibachi.web.workload.WorkloadManager;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
@Order(Ordered.LOWEST_PRECEDENCE-1)
public class InstrumentationConfig {
    @Bean
    public TimeSeries threadPoolTimeSeries(@Autowired MeterRegistry registry) {
        return new TimeSeries(registry, List.of(
                registry.find("jvm.threads.live"),
                registry.find("jvm.threads.peak")
        ));
    }

    @Bean
    public TimeSeries cpuTimeSeries(@Autowired MeterRegistry registry) {
        return new TimeSeries(registry, List.of(
                registry.find("process.cpu.usage"),
                registry.find("process.cpu.count"),
                registry.find("system.cpu.usage"),
                registry.find("system.cpu.count")
        ));
    }

    @Bean
    public TimeSeries memoryTimeSeries(@Autowired MeterRegistry registry) {
        return new TimeSeries(registry, List.of(
                registry.find("jvm.memory.max"),
                registry.find("jvm.memory.used"),
                registry.find("jvm.memory.committed")
        ));
    }

    @Bean
    public TimeSeries workloadTimeSeries(@Autowired MeterRegistry registry, @Autowired WorkloadManager workloadManager) {
        Gauge.builder("hibachi.workloads.total", workloadManager, value ->
                        value.getWorkloads().size())
                .description("Total amount of workloads")
                .register(registry);

        Gauge.builder("hibachi.workloads.active", workloadManager, value ->
                        value.getWorkloads(WorkloadModel::isRunning).size())
                .description("Active workloads not finished, failed or cancelled")
                .register(registry);

        Gauge.builder("hibachi.workloads.cancelled", workloadManager, value ->
                        value.getWorkloads(WorkloadModel::isCancelled).size())
                .description("Active workloads cancelled")
                .register(registry);

        Gauge.builder("hibachi.workloads.errors", workloadManager, value ->
                        value.getWorkloads(workload -> !workload.getProblems().isEmpty())
                                .size())
                .description("Workloads with at least one error")
                .register(registry);

        return new TimeSeries(registry, List.of(
                registry.find("hibachi.workloads.total"),
                registry.find("hibachi.workloads.active"),
                registry.find("hibachi.workloads.cancelled"),
                registry.find("hibachi.workloads.errors")
        ));
    }
}
