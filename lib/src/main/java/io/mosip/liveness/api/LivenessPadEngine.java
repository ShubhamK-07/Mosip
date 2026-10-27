package io.mosip.liveness.api;

import java.util.List;

public interface LivenessPadEngine {
    boolean supports(OperationMode mode);

    PassiveLivenessResult evaluatePassive(List<FaceFrame> frames, WorkflowType workflowType) throws LivenessEngineException;

    ActiveChallengeResult evaluateActive(ChallengeType challengeType, List<FaceFrame> frames,
                                         WorkflowType workflowType) throws LivenessEngineException;
}
