package io.mosip.liveness.api;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record LivenessPolicy(boolean enabled,
                             double passiveThreshold,
                             boolean activeEnabled,
                             int minimumActiveChallenges,
                             Set<ChallengeType> supportedChallengeTypes,
                             Duration challengeTimeout,
                             int maximumRetries,
                             int passiveFrameSampleSize,
                             int activeFrameSampleSize) {

    public LivenessPolicy {
        if (passiveThreshold < 0 || passiveThreshold > 1) {
            throw new IllegalArgumentException("passiveThreshold must be within [0, 1]");
        }
        if (minimumActiveChallenges < 0) {
            throw new IllegalArgumentException("minimumActiveChallenges cannot be negative");
        }
        if (maximumRetries < 0) {
            throw new IllegalArgumentException("maximumRetries cannot be negative");
        }
        if (passiveFrameSampleSize <= 0 || activeFrameSampleSize <= 0) {
            throw new IllegalArgumentException("frame sample sizes must be positive");
        }
        challengeTimeout = Objects.requireNonNull(challengeTimeout, "challengeTimeout cannot be null");
        if (challengeTimeout.isZero() || challengeTimeout.isNegative()) {
            throw new IllegalArgumentException("challengeTimeout must be positive");
        }
        supportedChallengeTypes = supportedChallengeTypes == null || supportedChallengeTypes.isEmpty()
            ? Collections.emptySet()
            : Collections.unmodifiableSet(EnumSet.copyOf(supportedChallengeTypes));
        if (activeEnabled && !enabled) {
            throw new IllegalArgumentException("activeEnabled cannot be true when liveness is disabled");
        }
        if (activeEnabled && supportedChallengeTypes.isEmpty() && minimumActiveChallenges > 0) {
            throw new IllegalArgumentException("supportedChallengeTypes required when active liveness is enabled");
        }
    }

    public static LivenessPolicy defaultEnabled() {
        return new LivenessPolicy(
            true,
            0.8,
            true,
            1,
            EnumSet.of(ChallengeType.BLINK, ChallengeType.SMILE, ChallengeType.TURN_HEAD_LEFT, ChallengeType.TURN_HEAD_RIGHT),
            Duration.ofSeconds(5),
            2,
            3,
            3
        );
    }
}
