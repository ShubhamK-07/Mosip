package io.mosip.liveness.api;

public interface ChallengePrompter {
    void onPassiveCheckStarted(WorkflowType workflowType, int attemptNumber);

    void onChallengeRequested(WorkflowType workflowType, ChallengeType challengeType, int challengeIndex, int totalChallenges);

    void onVerificationFailed(WorkflowType workflowType, FailureReason failureReason, int attemptNumber, boolean canRetry);
}
