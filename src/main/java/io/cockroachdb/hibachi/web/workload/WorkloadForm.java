package io.cockroachdb.hibachi.web.workload;

import java.util.List;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.cockroachdb.hibachi.web.editor.Slot;

@Validated
public class WorkloadForm {
    @NotNull(message = "Workload type must be selected")
    private WorkloadType workloadType;

    @Min(value = 10, message = "Duration must be > 10s")
    private long duration;

    @Min(value = 0, message = "Wait time must be >= 0")
    @Max(value = 1, message = "Wait time must be <= 1")
    private double probability;

    @Min(value = 0, message = "Wait time must be >= 0")
    private long waitTime;

    @Min(value = 0, message = "Wait time variation must be >= 0")
    private long waitTimeVariation;

    @Min(value = 1, message = "Thread count must be > 0")
    private int count = 1;

    private List<Slot> slots = List.of();

    private Slot slot;

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    public long getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(long waitTime) {
        this.waitTime = waitTime;
    }

    public long getWaitTimeVariation() {
        return waitTimeVariation;
    }

    public void setWaitTimeVariation(long waitTimeVariation) {
        this.waitTimeVariation = waitTimeVariation;
    }

    public List<Slot> getSlots() {
        return slots;
    }

    public void setSlots(List<Slot> slots) {
        this.slots = slots;
    }

    public Slot getSlot() {
        return slot;
    }

    public void setSlot(Slot slot) {
        this.slot = slot;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public WorkloadType getWorkloadType() {
        return workloadType;
    }

    public void setWorkloadType(WorkloadType workloadType) {
        this.workloadType = workloadType;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
