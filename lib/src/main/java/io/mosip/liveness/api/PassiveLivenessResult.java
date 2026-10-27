package io.mosip.liveness.api;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public record PassiveLivenessResult(double confidence, boolean presentationAttackDetected, Set<FailureReason> issues) {

    public PassiveLivenessResult {
        if (confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("confidence must be within [0, 1]");
        }
        issues = issues == null || issues.isEmpty()
            ? Collections.emptySet()
            : Collections.unmodifiableSet(EnumSet.copyOf(issues));
    }
}
