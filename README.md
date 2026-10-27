# Mosip

## Face liveness/PAD foundation

This repository now contains a vendor-neutral Java foundation for Desktop/Android face liveness and PAD workflows in `lib/`.

### Implemented building blocks

- Common face stream abstraction (`FaceFrameStream`) for L0/L1 adapters.
- Liveness/PAD engine contract (`LivenessPadEngine`) for passive and active checks.
- Workflow-specific policy model (`WorkflowLivenessPolicies`) for resident/operator/supervisor.
- Passive-to-active orchestration (`LivenessVerificationService`) with:
  - passive threshold evaluation,
  - dynamic challenge invocation,
  - active challenge validation,
  - retry policy enforcement,
  - offline/online mode checks,
  - error/failure reason classification.
- Dynamic challenge selection contract and default randomized selector.
- Mock stream and scripted liveness engine for development and automated testing.

### Test coverage

Focused unit tests cover:

- passive success,
- passive fallback to active challenge,
- PAD attack rejection,
- retry exhaustion handling,
- workflow-specific policy behavior.

Run:

```bash
./gradlew test
```
