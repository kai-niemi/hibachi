package io.cockroachdb.hibachi.web.workload;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.cockroachdb.hibachi.web.chart.Metrics;
import io.cockroachdb.hibachi.web.chart.Problem;
import io.cockroachdb.hibachi.util.DurationUtils;

public class WorkloadModel {
    private final Integer id;

    private final String description;

    private final Metrics metrics;

    private final Duration duration;

    private final LinkedList<Problem> problems;

    private final CompletableFuture<?> future;

    private final Instant startTime = Instant.now();

    private boolean failed;

    public WorkloadModel(Integer id,
                         String description,
                         Duration duration,
                         Metrics metrics,
                         LinkedList<Problem> problems,
                         CompletableFuture<?> future) {
        this.id = id;
        this.description = description;
        this.duration = duration;
        this.metrics = metrics;
        this.problems = problems;
        this.future = future;
    }

    public Integer getId() {
        return id;
    }

    public WorkloadStatus getStatus() {
        if (failed) {
            return WorkloadStatus.FAILED;
        } else if (isRunning()) {
            return WorkloadStatus.RUNNING;
        } else if (isCancelled()) {
            return WorkloadStatus.CANCELLED;
        } else {
            return WorkloadStatus.COMPLETED;
        }
    }

    public WorkloadModel setFailed(boolean failed) {
        this.failed = failed;
        return this;
    }

    public String getTitle() {
        return description;
    }

    public List<Problem> getProblems() {
        return Collections.unmodifiableList(problems.stream().limit(25).toList());
    }

    public Metrics getMetrics() {
        return isRunning() ? metrics : Metrics.copy(metrics);
    }

    public String getRemainingTime() {
        return isRunning() ?
                DurationUtils.durationToDisplayString(getRemainingDuration())
                : "-";
    }

    public Duration getRemainingDuration() {
        Duration remainingDuration = Duration.between(Instant.now(), startTime.plus(duration));
        return isRunning() && remainingDuration.isPositive()
                ? remainingDuration : Duration.ofSeconds(0);
    }

    public String getCompletionTime() {
        return getCompletionDuration().isPositive()
                ? DurationUtils.durationToDisplayString(getCompletionDuration())
                : "-";
    }

    public Duration getCompletionDuration() {
        return isRunning() ? Duration.ofSeconds(0)
                : Duration.between(startTime, Instant.now());
    }

    public String getStatusBadge() {
        return getStatus().getBadge();
    }

    public boolean isRunning() {
        return !future.isDone();
    }

    public boolean isCancelled() {
        return future.isCancelled();
    }

    public boolean cancel() {
        return future.cancel(true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkloadModel workloadModel = (WorkloadModel) o;
        return Objects.equals(id, workloadModel.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public boolean checkCompletion() {
        if (!future.isDone()) {
            return false;
        }
        awaitCompletion();
        return true;
    }

    public void awaitCompletion() {
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (CancellationException e) {
            // ok
        } catch (ExecutionException e) {
            setFailed(true);
        }
    }
}
