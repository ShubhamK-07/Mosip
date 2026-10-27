package io.mosip.liveness.api;

public record ActiveChallengeResult(ChallengeType challengeType,
                                    boolean successful,
                                    boolean timedOut,
                                    boolean presentationAttackDetected,
                                    double confidence) {

    public ActiveChallengeResult {
        if (confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("confidence must be within [0, 1]");
        }
    }
}
