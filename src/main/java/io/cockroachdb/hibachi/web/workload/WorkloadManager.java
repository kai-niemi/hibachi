package io.cockroachdb.hibachi.web.workload;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Component;

import io.cockroachdb.hibachi.web.chart.Metrics;
import io.cockroachdb.hibachi.web.chart.MetricsDataPoint;

/**
 * Manager for background workloads and time series data points for call metrics.
 */
@Component
public class WorkloadManager {
    private final List<WorkloadModel> workloadModels = Collections.synchronizedList(new LinkedList<>());

    private final List<MetricsDataPoint> dataPoints = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public void addWorkload(WorkloadModel workloadModel) {
        workloadModels.add(workloadModel);

        fireUpdatedEvent();
    }

    public List<WorkloadModel> getWorkloads() {
        return Collections.unmodifiableList(new LinkedList<>(workloadModels));
    }

    public List<WorkloadModel> getWorkloads(Predicate<WorkloadModel> predicate) {
        return new ArrayList<>(workloadModels.stream()
                .filter(predicate)
                .toList());
    }

    public Page<WorkloadModel> getWorkloads(Pageable pageable, Predicate<WorkloadModel> predicate) {
        List<WorkloadModel> copy = getWorkloads();
        List<WorkloadModel> content = new ArrayList<>(copy.stream()
                .filter(predicate)
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList());
        int total = copy.size();
        return PageableExecutionUtils.getPage(content, pageable, () -> total);
    }

    public WorkloadModel getWorkloadById(Integer id) {
        return workloadModels
                .stream()
                .filter(workload -> Objects.equals(workload.getId(), id))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No workload with id: " + id));
    }

    public void deleteWorkloads() {
        workloadModels.removeIf(workload -> !workload.isRunning());

        fireUpdatedEvent();
    }

    public void deleteWorkload(Integer id) {
        WorkloadModel workloadModel = getWorkloadById(id);
        if (workloadModel.isRunning()) {
            throw new IllegalStateException("Workload is running: " + id);
        }

        workloadModels.remove(workloadModel);

        fireUpdatedEvent();
    }

    public void cancelWorkloads() {
        workloadModels.forEach(workload -> cancelWorkload(workload.getId()));
    }

    public void cancelWorkload(Integer id) {
        getWorkloadById(id).cancel();
    }

    private void fireUpdatedEvent() {
        applicationEventPublisher.publishEvent(new WorkloadUpdatedEvent(this));
    }

    // Time series functions

    public Metrics getMetricsAggregate(Pageable page) {
        List<Metrics> metrics = getWorkloads(page, workloadModel -> true)
                .stream()
                .map(WorkloadModel::getMetrics).toList();
        return Metrics.builder()
                .withUpdateTime(Instant.now())
                .withMeanTimeMillis(metrics.stream()
                        .mapToDouble(Metrics::getMeanTimeMillis).average().orElse(0))
                .withOps(metrics.stream().mapToDouble(Metrics::getOpsPerSec).sum(),
                        metrics.stream().mapToDouble(Metrics::getOpsPerMin).sum())
                .withP50(metrics.stream().mapToDouble(Metrics::getP50).average().orElse(0))
                .withP90(metrics.stream().mapToDouble(Metrics::getP90).average().orElse(0))
                .withP95(metrics.stream().mapToDouble(Metrics::getP95).average().orElse(0))
                .withP99(metrics.stream().mapToDouble(Metrics::getP99).average().orElse(0))
                .withP999(metrics.stream().mapToDouble(Metrics::getP999).average().orElse(0))
                .withMeanTimeMillis(metrics.stream().mapToDouble(Metrics::getMeanTimeMillis).average().orElse(0))
                .withSuccessful(metrics.stream().mapToInt(Metrics::getSuccess).sum())
                .withFails(metrics.stream().mapToInt(Metrics::getTransientFail).sum(),
                        metrics.stream().mapToInt(Metrics::getNonTransientFail).sum())
                .build();
    }

    public void snapshotDataPoints(Duration samplePeriod) {
        // Purge old data points older than sample period
        dataPoints.removeIf(item -> item.getInstant()
                .isBefore(Instant.now().minusSeconds(samplePeriod.toSeconds())));

        // Add new datapoint by sampling all workload metrics
        MetricsDataPoint dataPoint = new MetricsDataPoint(Instant.now());

        // Add datapoint if still running
        getWorkloads()
                .stream()
                .filter(WorkloadModel::isRunning)
                .forEach(workload -> dataPoint.putValue(workload.getId(), workload.getMetrics()));

        dataPoints.add(dataPoint);
    }

    public List<Map<String, Object>> getDataPoints(Function<Metrics, Double> mapper, Pageable page) {
        final List<Map<String, Object>> columnData = new ArrayList<>();

        {
            final Map<String, Object> headerElement = new HashMap<>();
            List<Long> labels = dataPoints
                    .stream()
                    .map(MetricsDataPoint::getInstant)
                    .toList()
                    .stream()
                    .map(Instant::toEpochMilli)
                    .toList();
            headerElement.put("data", labels.toArray());
            columnData.add(headerElement);
        }

        getWorkloads(page, (x) -> true)
                .forEach(workload -> {
                    Map<String, Object> dataElement = new HashMap<>();

                    List<Metrics> metrics = new ArrayList<>();

                    dataPoints.forEach(dataPoint -> metrics.add(
                            dataPoint.getValue(workload.getId(), Metrics.empty())));

                    List<Double> data = metrics
                            .stream()
                            .map(mapper)
                            .toList();

                    dataElement.put("id", workload.getId());
                    dataElement.put("name", "#%d".formatted(workload.getId()));
                    dataElement.put("data", data.toArray());

                    columnData.add(dataElement);
                });

        return columnData;
    }
}
