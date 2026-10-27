package io.mosip.liveness.mock;

import io.mosip.liveness.api.*;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

public final class ScriptedLivenessPadEngine implements LivenessPadEngine {
    private final Queue<PassiveLivenessResult> passiveResults;
    private final Queue<ActiveChallengeResult> activeResults;

    public ScriptedLivenessPadEngine(Collection<PassiveLivenessResult> passiveResults,
                                     Collection<ActiveChallengeResult> activeResults) {
        this.passiveResults = new ArrayDeque<>(Objects.requireNonNull(passiveResults, "passiveResults cannot be null"));
        this.activeResults = new ArrayDeque<>(Objects.requireNonNull(activeResults, "activeResults cannot be null"));
    }

    @Override
    public boolean supports(OperationMode mode) {
        return true;
    }

    @Override
    public PassiveLivenessResult evaluatePassive(List<FaceFrame> frames, WorkflowType workflowType) throws LivenessEngineException {
        PassiveLivenessResult result = passiveResults.poll();
        if (result == null) {
            throw new LivenessEngineException("No scripted passive result available");
        }
        return result;
    }

    @Override
    public ActiveChallengeResult evaluateActive(ChallengeType challengeType, List<FaceFrame> frames, WorkflowType workflowType)
        throws LivenessEngineException {
        ActiveChallengeResult result = activeResults.poll();
        if (result == null) {
            throw new LivenessEngineException("No scripted active result available");
        }
        return result;
    }
}
