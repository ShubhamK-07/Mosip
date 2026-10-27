package io.mosip.liveness.api;

public class DeviceAccessException extends Exception {
    public DeviceAccessException(String message) {
        super(message);
    }

    public DeviceAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
