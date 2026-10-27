package io.mosip.liveness.api;

public class LivenessEngineException extends Exception {
    public LivenessEngineException(String message) {
        super(message);
    }

    public LivenessEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
