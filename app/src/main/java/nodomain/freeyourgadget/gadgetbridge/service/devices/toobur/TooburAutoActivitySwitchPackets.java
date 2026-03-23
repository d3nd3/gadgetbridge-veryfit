/*  Copyright (C) 2025 idowatch / TOOBUR — SET 0x03 0x49 activity auto-detect (VBUS_EVT_APP_SET_ACTIVITY_SWITCH) */
package nodomain.freeyourgadget.gadgetbridge.service.devices.toobur;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.devices.id115.ID115Constants;

/**
 * VeryFit {@code 03 49 …} (11 bytes): nine payload bytes after {@code 03 49} map to JSON fields in order:
 * walk, run, bicycle, auto_pause, auto_end_remind, elliptical, rowing, swim, smart_rope (each 0/1).
 * <p>
 * Off capture: {@code 03 49 00 00 00 00 00 00 00 00 00}; walk+run capture:
 * {@code 03 49 01 01 00 00 00 00 00 00 00}.
 */
public final class TooburAutoActivitySwitchPackets {

    /** Preference value: detection off (all zeros). */
    public static final String PRESET_OFF = "off";
    /** Walk + run only (logcat capture). */
    public static final String PRESET_WALK_RUN = "walk_run";
    /** Walk, run, bicycle. */
    public static final String PRESET_WALK_RUN_BIKE = "walk_run_bike";
    /** Walk, run, bike, swim. */
    public static final String PRESET_WALK_RUN_BIKE_SWIM = "walk_run_bike_swim";
    /** All sport-type flags on; auto_pause and auto_end_remind left 0 unless noted. */
    public static final String PRESET_ALL_SPORTS = "all_sports";

    private TooburAutoActivitySwitchPackets() {
    }

    /**
     * @param presetKey one of {@link #PRESET_OFF}, {@link #PRESET_WALK_RUN}, …
     */
    @NonNull
    public static byte[] build(@NonNull String presetKey) {
        byte[] nine = nineBytesForPreset(presetKey);
        byte[] out = new byte[11];
        out[0] = ID115Constants.CMD_ID_SETTINGS;
        out[1] = ID115Constants.CMD_KEY_SET_ACTIVITY_SWITCH;
        System.arraycopy(nine, 0, out, 2, 9);
        return out;
    }

    private static byte[] nineBytesForPreset(String presetKey) {
        switch (presetKey) {
            case PRESET_WALK_RUN:
                return new byte[]{1, 1, 0, 0, 0, 0, 0, 0, 0};
            case PRESET_WALK_RUN_BIKE:
                return new byte[]{1, 1, 1, 0, 0, 0, 0, 0, 0};
            case PRESET_WALK_RUN_BIKE_SWIM:
                return new byte[]{1, 1, 1, 0, 0, 0, 0, 1, 0};
            case PRESET_ALL_SPORTS:
                return new byte[]{1, 1, 1, 0, 1, 1, 1, 1, 1};
            case PRESET_OFF:
            default:
                return new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
        }
    }
}
