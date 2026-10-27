package io.mosip.liveness.core;

import io.mosip.liveness.api.ChallengeSelector;
import io.mosip.liveness.api.ChallengeType;
import io.mosip.liveness.api.LivenessPolicy;
import io.mosip.liveness.api.WorkflowType;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class DefaultChallengeSelector implements ChallengeSelector {
    private final SecureRandom secureRandom;

    public DefaultChallengeSelector(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom cannot be null");
    }

    @Override
    public List<ChallengeType> selectChallenges(LivenessPolicy policy, WorkflowType workflowType, int attemptNumber) {
        List<ChallengeType> pool = new ArrayList<>(policy.supportedChallengeTypes());
        Collections.shuffle(pool, secureRandom);
        int requiredCount = Math.min(policy.minimumActiveChallenges(), pool.size());
        return List.copyOf(pool.subList(0, requiredCount));
    }
}
