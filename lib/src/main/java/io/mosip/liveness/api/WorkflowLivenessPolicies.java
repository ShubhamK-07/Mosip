package io.mosip.liveness.api;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class WorkflowLivenessPolicies {
    private final EnumMap<WorkflowType, LivenessPolicy> policies;

    public WorkflowLivenessPolicies(Map<WorkflowType, LivenessPolicy> policies) {
        Objects.requireNonNull(policies, "policies cannot be null");
        this.policies = new EnumMap<>(WorkflowType.class);
        this.policies.putAll(policies);
        for (WorkflowType workflowType : WorkflowType.values()) {
            this.policies.putIfAbsent(workflowType, LivenessPolicy.defaultEnabled());
        }
    }

    public LivenessPolicy forWorkflow(WorkflowType workflowType) {
        return policies.get(Objects.requireNonNull(workflowType, "workflowType cannot be null"));
    }
}
