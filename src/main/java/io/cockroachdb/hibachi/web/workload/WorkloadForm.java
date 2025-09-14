package io.cockroachdb.hibachi.web.workload;

import java.util.List;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.cockroachdb.hibachi.web.editor.Slot;

@Validated
public class WorkloadForm {
    @NotNull(message = "Workload type must be selected")
    private WorkloadType workloadType;

    @NotNull(message = "Duration is empty")
    @Min(value = 10, message = "Duration must be > 10s")
    private Long duration;

    @NotNull(message = "Pause is empty")
    @Min(value = 0, message = "Pause must be >= 0")
    private Long pause;

    @NotNull(message = "Pause variation is empty")
    @Min(value = 0, message = "Pause variation must be >= 0")
    private Long pauseVariation;

    @NotNull(message = "Thread count is empty")
    @Min(value = 1, message = "Thread count must be > 0")
    private Integer count = 1;

    private List<Slot> slots = List.of();

    private Slot slot;

    public Long getPause() {
        return pause;
    }

    public void setPause(Long pause) {
        this.pause = pause;
    }

    public Long getPauseVariation() {
        return pauseVariation;
    }

    public void setPauseVariation(Long pauseVariation) {
        this.pauseVariation = pauseVariation;
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

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public WorkloadType getWorkloadType() {
        return workloadType;
    }

    public void setWorkloadType(WorkloadType workloadType) {
        this.workloadType = workloadType;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
