package io.mosip.liveness.core;

import io.mosip.liveness.api.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class LivenessVerificationService {
    private final WorkflowLivenessPolicies workflowPolicies;
    private final LivenessPadEngine livenessPadEngine;
    private final ChallengeSelector challengeSelector;

    public LivenessVerificationService(WorkflowLivenessPolicies workflowPolicies,
                                       LivenessPadEngine livenessPadEngine,
                                       ChallengeSelector challengeSelector) {
        this.workflowPolicies = Objects.requireNonNull(workflowPolicies, "workflowPolicies cannot be null");
        this.livenessPadEngine = Objects.requireNonNull(livenessPadEngine, "livenessPadEngine cannot be null");
        this.challengeSelector = Objects.requireNonNull(challengeSelector, "challengeSelector cannot be null");
    }

    public VerificationOutcome verify(WorkflowType workflowType,
                                      OperationMode operationMode,
                                      FaceFrameStream faceFrameStream,
                                      ChallengePrompter challengePrompter) {
        LivenessPolicy policy = workflowPolicies.forWorkflow(workflowType);
        if (!policy.enabled()) {
            return VerificationOutcome.success(workflowType, 0);
        }
        if (!livenessPadEngine.supports(operationMode)) {
            return VerificationOutcome.failure(workflowType, 0, FailureReason.OFFLINE_MODE_NOT_SUPPORTED);
        }
        if (!faceFrameStream.isConnected()) {
            return VerificationOutcome.failure(workflowType, 0, FailureReason.DEVICE_UNAVAILABLE);
        }

        int maxAttempts = Math.max(1, policy.maximumRetries() + 1);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (challengePrompter != null) {
                challengePrompter.onPassiveCheckStarted(workflowType, attempt);
            }
            FailureReason attemptFailure = verifyAttempt(workflowType, policy, faceFrameStream, challengePrompter, attempt);
            if (attemptFailure == null) {
                return VerificationOutcome.success(workflowType, attempt);
            }
            if (!isRetryable(attemptFailure)) {
                return VerificationOutcome.failure(workflowType, attempt, attemptFailure);
            }
            boolean canRetry = attempt < maxAttempts;
            if (challengePrompter != null) {
                challengePrompter.onVerificationFailed(workflowType, attemptFailure, attempt, canRetry);
            }
            if (!canRetry) {
                return VerificationOutcome.failure(workflowType, attempt, FailureReason.MAX_RETRIES_EXCEEDED);
            }
        }
        return VerificationOutcome.failure(workflowType, maxAttempts, FailureReason.MAX_RETRIES_EXCEEDED);
    }

    private boolean isRetryable(FailureReason reason) {
        return switch (reason) {
            case CAMERA_UNAVAILABLE, FACE_NOT_DETECTED, MULTIPLE_FACES_DETECTED, POOR_FACE_QUALITY,
                 PASSIVE_LIVENESS_BELOW_THRESHOLD, ACTIVE_CHALLENGE_FAILED, CHALLENGE_TIMEOUT,
                 DEVICE_CONNECTION_FAILURE -> true;
            default -> false;
        };
    }

    private FailureReason verifyAttempt(WorkflowType workflowType,
                                        LivenessPolicy policy,
                                        FaceFrameStream faceFrameStream,
                                        ChallengePrompter challengePrompter,
                                        int attemptNumber) {
        try {
            List<FaceFrame> passiveFrames = captureFrames(faceFrameStream, policy.challengeTimeout(), policy.passiveFrameSampleSize());
            FailureReason frameValidationFailure = validateFrames(passiveFrames);
            if (frameValidationFailure != null) {
                return frameValidationFailure;
            }

            PassiveLivenessResult passiveResult = livenessPadEngine.evaluatePassive(passiveFrames, workflowType);
            if (passiveResult.presentationAttackDetected()) {
                return FailureReason.PAD_ATTACK_DETECTED;
            }
            if (passiveResult.confidence() >= policy.passiveThreshold()) {
                return null;
            }
            if (!policy.activeEnabled()) {
                return FailureReason.PASSIVE_LIVENESS_BELOW_THRESHOLD;
            }

            List<ChallengeType> selectedChallenges = challengeSelector.selectChallenges(policy, workflowType, attemptNumber);
            if (selectedChallenges.isEmpty()) {
                return FailureReason.ACTIVE_CHALLENGE_FAILED;
            }
            for (int i = 0; i < selectedChallenges.size(); i++) {
                ChallengeType challengeType = selectedChallenges.get(i);
                if (challengePrompter != null) {
                    challengePrompter.onChallengeRequested(workflowType, challengeType, i + 1, selectedChallenges.size());
                }
                List<FaceFrame> challengeFrames = captureFrames(faceFrameStream, policy.challengeTimeout(), policy.activeFrameSampleSize());
                frameValidationFailure = validateFrames(challengeFrames);
                if (frameValidationFailure != null) {
                    return frameValidationFailure;
                }
                ActiveChallengeResult challengeResult = livenessPadEngine.evaluateActive(challengeType, challengeFrames, workflowType);
                if (challengeResult.presentationAttackDetected()) {
                    return FailureReason.PAD_ATTACK_DETECTED;
                }
                if (challengeResult.timedOut()) {
                    return FailureReason.CHALLENGE_TIMEOUT;
                }
                if (!challengeResult.successful() || challengeResult.confidence() < policy.passiveThreshold()) {
                    return FailureReason.ACTIVE_CHALLENGE_FAILED;
                }
            }
            return null;
        } catch (DeviceAccessException e) {
            return FailureReason.DEVICE_CONNECTION_FAILURE;
        } catch (LivenessEngineException e) {
            return FailureReason.LIVENESS_ENGINE_ERROR;
        }
    }

    private List<FaceFrame> captureFrames(FaceFrameStream faceFrameStream, Duration timeout, int expectedFrames)
        throws DeviceAccessException {
        List<FaceFrame> frames = new ArrayList<>(expectedFrames);
        Instant deadline = Instant.now().plus(timeout);
        while (frames.size() < expectedFrames && Instant.now().isBefore(deadline)) {
            Duration remaining = Duration.between(Instant.now(), deadline);
            if (remaining.isNegative() || remaining.isZero()) {
                break;
            }
            Optional<FaceFrame> nextFrame = faceFrameStream.nextFrame(remaining);
            nextFrame.ifPresent(frames::add);
        }
        return frames;
    }

    private FailureReason validateFrames(List<FaceFrame> frames) {
        if (frames.isEmpty()) {
            return FailureReason.CAMERA_UNAVAILABLE;
        }
        for (FaceFrame frame : frames) {
            if (!frame.hasValidImage()) {
                return FailureReason.INVALID_FRAME_DATA;
            }
            if (frame.detectedFaceCount() == 0) {
                return FailureReason.FACE_NOT_DETECTED;
            }
            if (frame.detectedFaceCount() > 1) {
                return FailureReason.MULTIPLE_FACES_DETECTED;
            }
            if (!frame.faceQualityAcceptable()) {
                return FailureReason.POOR_FACE_QUALITY;
            }
        }
        return null;
    }
}
