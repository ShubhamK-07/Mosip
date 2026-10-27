package io.mosip.liveness.core;

import io.mosip.liveness.api.*;
import io.mosip.liveness.mock.InMemoryFaceFrameStream;
import io.mosip.liveness.mock.ScriptedLivenessPadEngine;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LivenessVerificationServiceTest {

    @Test
    void shouldPassWhenPassiveLivenessExceedsThreshold() {
        LivenessVerificationService service = createService(
            policyFor(0),
            List.of(new PassiveLivenessResult(0.92, false, Set.of())),
            List.of(),
            new FixedChallengeSelector(List.of(ChallengeType.BLINK))
        );

        VerificationOutcome outcome = service.verify(
            WorkflowType.RESIDENT_REGISTRATION,
            OperationMode.OFFLINE,
            streamWithFrames(3),
            null
        );

        assertTrue(outcome.success());
        assertEquals(1, outcome.attemptsUsed());
    }

    @Test
    void shouldFallBackToActiveChallengeWhenPassiveIsLowAndSucceed() {
        LivenessVerificationService service = createService(
            policyFor(1),
            List.of(new PassiveLivenessResult(0.35, false, Set.of())),
            List.of(new ActiveChallengeResult(ChallengeType.SMILE, true, false, false, 0.91)),
            new FixedChallengeSelector(List.of(ChallengeType.SMILE))
        );

        VerificationOutcome outcome = service.verify(
            WorkflowType.OPERATOR_AUTHENTICATION,
            OperationMode.OFFLINE,
            streamWithFrames(6),
            null
        );

        assertTrue(outcome.success());
    }

    @Test
    void shouldFailImmediatelyWhenPadAttackDetected() {
        LivenessVerificationService service = createService(
            policyFor(1),
            List.of(new PassiveLivenessResult(0.95, true, Set.of())),
            List.of(),
            new FixedChallengeSelector(List.of(ChallengeType.BLINK))
        );

        VerificationOutcome outcome = service.verify(
            WorkflowType.SUPERVISOR_AUTHENTICATION,
            OperationMode.OFFLINE,
            streamWithFrames(3),
            null
        );

        assertFalse(outcome.success());
        assertEquals(Optional.of(FailureReason.PAD_ATTACK_DETECTED), outcome.failureReason());
    }

    @Test
    void shouldFailWithMaxRetriesExceededWhenChallengesContinueFailing() {
        LivenessVerificationService service = createService(
            policyFor(2),
            List.of(
                new PassiveLivenessResult(0.3, false, Set.of()),
                new PassiveLivenessResult(0.3, false, Set.of()),
                new PassiveLivenessResult(0.3, false, Set.of())
            ),
            List.of(
                new ActiveChallengeResult(ChallengeType.BLINK, false, false, false, 0.3),
                new ActiveChallengeResult(ChallengeType.BLINK, false, false, false, 0.3),
                new ActiveChallengeResult(ChallengeType.BLINK, false, false, false, 0.3)
            ),
            new FixedChallengeSelector(List.of(ChallengeType.BLINK))
        );

        VerificationOutcome outcome = service.verify(
            WorkflowType.RESIDENT_REGISTRATION,
            OperationMode.OFFLINE,
            streamWithFrames(18),
            null
        );

        assertFalse(outcome.success());
        assertEquals(Optional.of(FailureReason.MAX_RETRIES_EXCEEDED), outcome.failureReason());
        assertEquals(3, outcome.attemptsUsed());
    }

    @Test
    void shouldUseWorkflowSpecificPolicy() {
        LivenessPolicy residentPolicy = new LivenessPolicy(
            true, 0.8, true, 1, EnumSet.of(ChallengeType.BLINK), Duration.ofSeconds(2), 1, 2, 2
        );
        LivenessPolicy operatorPolicy = new LivenessPolicy(
            false, 0.8, false, 0, Set.of(), Duration.ofSeconds(2), 0, 2, 2
        );

        Map<WorkflowType, LivenessPolicy> policies = new EnumMap<>(WorkflowType.class);
        policies.put(WorkflowType.RESIDENT_REGISTRATION, residentPolicy);
        policies.put(WorkflowType.OPERATOR_AUTHENTICATION, operatorPolicy);
        policies.put(WorkflowType.SUPERVISOR_AUTHENTICATION, residentPolicy);

        LivenessVerificationService service = new LivenessVerificationService(
            new WorkflowLivenessPolicies(policies),
            new ScriptedLivenessPadEngine(List.of(new PassiveLivenessResult(0.1, false, Set.of())), List.of()),
            new FixedChallengeSelector(List.of(ChallengeType.BLINK))
        );

        VerificationOutcome operatorOutcome = service.verify(
            WorkflowType.OPERATOR_AUTHENTICATION,
            OperationMode.OFFLINE,
            streamWithFrames(1),
            null
        );

        assertTrue(operatorOutcome.success());
        assertEquals(0, operatorOutcome.attemptsUsed());
    }

    private static LivenessVerificationService createService(LivenessPolicy policy,
                                                             List<PassiveLivenessResult> passiveResults,
                                                             List<ActiveChallengeResult> activeResults,
                                                             ChallengeSelector challengeSelector) {
        Map<WorkflowType, LivenessPolicy> policyMap = new EnumMap<>(WorkflowType.class);
        policyMap.put(WorkflowType.RESIDENT_REGISTRATION, policy);
        policyMap.put(WorkflowType.OPERATOR_AUTHENTICATION, policy);
        policyMap.put(WorkflowType.SUPERVISOR_AUTHENTICATION, policy);

        return new LivenessVerificationService(
            new WorkflowLivenessPolicies(policyMap),
            new ScriptedLivenessPadEngine(passiveResults, activeResults),
            challengeSelector
        );
    }

    private static LivenessPolicy policyFor(int maxRetries) {
        return new LivenessPolicy(
            true,
            0.8,
            true,
            1,
            EnumSet.of(ChallengeType.BLINK, ChallengeType.SMILE),
            Duration.ofSeconds(3),
            maxRetries,
            3,
            3
        );
    }

    private static InMemoryFaceFrameStream streamWithFrames(int frameCount) {
        List<FaceFrame> frames = new ArrayList<>();
        for (int i = 0; i < frameCount; i++) {
            frames.add(new FaceFrame(new byte[]{1, 2, 3}, System.currentTimeMillis(), 640, 480, 1, true));
        }
        return new InMemoryFaceFrameStream(frames);
    }

    private record FixedChallengeSelector(List<ChallengeType> challenges) implements ChallengeSelector {
        @Override
        public List<ChallengeType> selectChallenges(LivenessPolicy policy, WorkflowType workflowType, int attemptNumber) {
            return challenges;
        }
    }
}
