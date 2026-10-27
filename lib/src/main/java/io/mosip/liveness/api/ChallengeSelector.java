package io.mosip.liveness.api;

import java.util.List;

public interface ChallengeSelector {
    List<ChallengeType> selectChallenges(LivenessPolicy policy, WorkflowType workflowType, int attemptNumber);
}
