package io.mosip.liveness.mock;

import io.mosip.liveness.api.DeviceAccessException;
import io.mosip.liveness.api.FaceFrame;
import io.mosip.liveness.api.FaceFrameStream;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

public final class InMemoryFaceFrameStream implements FaceFrameStream {
    private final Queue<FaceFrame> frames;
    private boolean connected;

    public InMemoryFaceFrameStream(Collection<FaceFrame> frames) {
        this.frames = new ArrayDeque<>(Objects.requireNonNull(frames, "frames cannot be null"));
        this.connected = true;
    }

    public void disconnect() {
        this.connected = false;
    }

    @Override
    public Optional<FaceFrame> nextFrame(Duration timeout) throws DeviceAccessException {
        if (!connected) {
            throw new DeviceAccessException("Device disconnected");
        }
        return Optional.ofNullable(frames.poll());
    }

    @Override
    public boolean isConnected() {
        return connected;
    }
}
