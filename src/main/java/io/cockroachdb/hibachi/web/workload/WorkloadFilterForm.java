package io.cockroachdb.hibachi.web.workload;

import io.cockroachdb.hibachi.web.workload.WorkloadStatus;

public class WorkloadFilterForm {
    private WorkloadStatus status;

    public WorkloadStatus getStatus() {
        return status;
    }

    public WorkloadFilterForm setStatus(WorkloadStatus status) {
        this.status = status;
        return this;
    }

    @Override
    public String toString() {
        return "WorkloadFilterForm{" +
               "status=" + status +
               '}';
    }
}
