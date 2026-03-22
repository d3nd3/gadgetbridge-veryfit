/*  Copyright (C) 2025 idowatch / TOOBUR device support

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.toobur;

import java.io.Serializable;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

/**
 * Minimal {@link ActivitySample} for {@link nodomain.freeyourgadget.gadgetbridge.model.DeviceService#ACTION_REALTIME_SAMPLES}
 * when ID115 DB entities do not carry HR. Used after GET 0x02 0xA0 live snapshot.
 */
public final class TooburRealtimeActivitySample implements ActivitySample, Serializable {
    private static final long serialVersionUID = 1L;

    private final int timestamp;
    private int heartRate;

    public TooburRealtimeActivitySample(int timestampSeconds, int heartRateBpm) {
        this.timestamp = timestampSeconds;
        this.heartRate = heartRateBpm;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public SampleProvider<?> getProvider() {
        return null;
    }

    @Override
    public int getRawKind() {
        return ActivityKind.UNKNOWN.getCode();
    }

    @Override
    public ActivityKind getKind() {
        return ActivityKind.UNKNOWN;
    }

    @Override
    public int getRawIntensity() {
        return NOT_MEASURED;
    }

    @Override
    public float getIntensity() {
        return 0f;
    }

    @Override
    public int getSteps() {
        return NOT_MEASURED;
    }

    @Override
    public int getDistanceCm() {
        return NOT_MEASURED;
    }

    @Override
    public int getActiveCalories() {
        return NOT_MEASURED;
    }

    @Override
    public int getHeartRate() {
        return heartRate;
    }

    @Override
    public void setHeartRate(int value) {
        this.heartRate = value;
    }
}
