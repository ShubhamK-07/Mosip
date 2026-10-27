package io.mosip.liveness.api;

import java.time.Duration;
import java.util.Optional;

public interface FaceFrameStream extends AutoCloseable {
    Optional<FaceFrame> nextFrame(Duration timeout) throws DeviceAccessException;

    boolean isConnected();

    @Override
    default void close() {
    }
}
