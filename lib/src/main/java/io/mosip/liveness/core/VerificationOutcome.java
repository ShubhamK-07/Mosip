package io.mosip.liveness.core;

import io.mosip.liveness.api.FailureReason;
import io.mosip.liveness.api.WorkflowType;

import java.util.Optional;

public record VerificationOutcome(boolean success,
                                  WorkflowType workflowType,
                                  int attemptsUsed,
                                  Optional<FailureReason> failureReason) {

    public VerificationOutcome {
        if (attemptsUsed < 0) {
            throw new IllegalArgumentException("attemptsUsed cannot be negative");
        }
        failureReason = failureReason == null ? Optional.empty() : failureReason;
    }

    public static VerificationOutcome success(WorkflowType workflowType, int attemptsUsed) {
        return new VerificationOutcome(true, workflowType, attemptsUsed, Optional.empty());
    }

    public static VerificationOutcome failure(WorkflowType workflowType, int attemptsUsed, FailureReason reason) {
        return new VerificationOutcome(false, workflowType, attemptsUsed, Optional.of(reason));
    }
}
