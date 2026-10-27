package io.mosip.liveness.api;

import java.util.Arrays;

public record FaceFrame(byte[] imageData, long captureTimestamp, int width, int height, int detectedFaceCount,
                        boolean faceQualityAcceptable) {

    public FaceFrame {
        imageData = imageData == null ? new byte[0] : Arrays.copyOf(imageData, imageData.length);
    }

    public boolean hasValidImage() {
        return imageData.length > 0 && width > 0 && height > 0;
    }
}
