/*  Copyright (C) 2025 idowatch / TOOBUR — device list card quick toggles */
package nodomain.freeyourgadget.gadgetbridge.devices.toobur;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCardAction;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.toobur.TooburAutoActivitySwitchPackets;
import nodomain.freeyourgadget.gadgetbridge.service.devices.toobur.TooburSupport;

/**
 * Four slots on {@link nodomain.freeyourgadget.gadgetbridge.R.layout#device_itemv2}: HR, SpO₂, stress, auto activity.
 */
public final class TooburDeviceCardActions {

    private TooburDeviceCardActions() {
    }

    @NonNull
    public static List<DeviceCardAction> create() {
        return Arrays.asList(
                new HrCardAction(),
                new Spo2CardAction(),
                new StressCardAction(),
                new AutoActivityCardAction()
        );
    }

    private static final class HrCardAction implements DeviceCardAction {
        @Override
        public int getIcon(GBDevice device) {
            return R.drawable.ic_heart;
        }

        @Override
        public String getDescription(GBDevice device, Context context) {
            return context.getString(R.string.toobur_device_card_hr_desc);
        }

        @Override
        public String getLabel(GBDevice device, Context context) {
            SharedPreferences p = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());
            boolean on = TooburSupport.isHrContinuousEnabled(p);
            return on ? context.getString(R.string.on) : context.getString(R.string.off);
        }

        @Override
        public void onClick(GBDevice device, Context context) {
            SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());
            boolean next = !TooburSupport.isHrContinuousEnabled(prefs);
            prefs.edit().putBoolean(TooburSupport.PREF_TOOBUR_HR_CONTINUOUS_ENABLED, next).apply();
            GBApplication.deviceService(device).onSendConfiguration(TooburSupport.PREF_TOOBUR_HR_CONTINUOUS_ENABLED);
            device.sendDeviceUpdateIntent(context);
        }
    }

    private static final class Spo2CardAction implements DeviceCardAction {
        @Override
        public int getIcon(GBDevice device) {
            return R.drawable.ic_activity_graphs;
        }

        @Override
        public String getDescription(GBDevice device, Context context) {
            return context.getString(R.string.toobur_device_card_spo2_desc);
        }

        @Override
        public String getLabel(GBDevice device, Context context) {
            SharedPreferences p = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());
            boolean on = p.getBoolean(TooburSupport.PREF_TOOBUR_SPO2_CONTINUOUS_ENABLED, true);
            return on ? context.getString(R.string.on) : context.getString(R.string.off);
        }

        @Override
        public void onClick(GBDevice device, Context context) {
            SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());
            boolean next = !prefs.getBoolean(TooburSupport.PREF_TOOBUR_SPO2_CONTINUOUS_ENABLED, true);
            prefs.edit().putBoolean(TooburSupport.PREF_TOOBUR_SPO2_CONTINUOUS_ENABLED, next).apply();
            GBApplication.deviceService(device).onSendConfiguration(TooburSupport.PREF_TOOBUR_SPO2_CONTINUOUS_ENABLED);
            device.sendDeviceUpdateIntent(context);
        }
    }

    private static final class StressCardAction implements DeviceCardAction {
        @Override
        public int getIcon(GBDevice device) {
            return R.drawable.ic_activity_unknown;
        }

        @Override
        public String getDescription(GBDevice device, Context context) {
            return context.getString(R.string.toobur_device_card_stress_desc);
        }

        @Override
        public String getLabel(GBDevice device, Context context) {
            SharedPreferences p = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());
            boolean on = p.getBoolean(TooburSupport.PREF_TOOBUR_PRESSURE_CONTINUOUS_ENABLED, true);
            return on ? context.getString(R.string.on) : context.getString(R.string.off);
        }

        @Override
        public void onClick(GBDevice device, Context context) {
            SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());
            boolean next = !prefs.getBoolean(TooburSupport.PREF_TOOBUR_PRESSURE_CONTINUOUS_ENABLED, true);
            prefs.edit().putBoolean(TooburSupport.PREF_TOOBUR_PRESSURE_CONTINUOUS_ENABLED, next).apply();
            GBApplication.deviceService(device).onSendConfiguration(TooburSupport.PREF_TOOBUR_PRESSURE_CONTINUOUS_ENABLED);
            device.sendDeviceUpdateIntent(context);
        }
    }

    /**
     * Tap toggles preset between {@link TooburAutoActivitySwitchPackets#PRESET_OFF} and
     * {@link TooburAutoActivitySwitchPackets#PRESET_WALK_RUN} (full preset list stays in device settings).
     */
    private static final class AutoActivityCardAction implements DeviceCardAction {
        @Override
        public int getIcon(GBDevice device) {
            return R.drawable.ic_activity_tracks;
        }

        @Override
        public String getDescription(GBDevice device, Context context) {
            return context.getString(R.string.toobur_device_card_auto_desc);
        }

        @Override
        public String getLabel(GBDevice device, Context context) {
            SharedPreferences p = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());
            String preset = p.getString(TooburSupport.PREF_TOOBUR_AUTO_ACTIVITY_PRESET, TooburAutoActivitySwitchPackets.PRESET_OFF);
            return shortAutoLabel(context, preset);
        }

        @Override
        public void onClick(GBDevice device, Context context) {
            SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());
            String cur = prefs.getString(TooburSupport.PREF_TOOBUR_AUTO_ACTIVITY_PRESET, TooburAutoActivitySwitchPackets.PRESET_OFF);
            if (cur == null) {
                cur = TooburAutoActivitySwitchPackets.PRESET_OFF;
            }
            String next = TooburAutoActivitySwitchPackets.PRESET_OFF.equals(cur)
                    ? TooburAutoActivitySwitchPackets.PRESET_WALK_RUN
                    : TooburAutoActivitySwitchPackets.PRESET_OFF;
            prefs.edit().putString(TooburSupport.PREF_TOOBUR_AUTO_ACTIVITY_PRESET, next).apply();
            GBApplication.deviceService(device).onSendConfiguration(TooburSupport.PREF_TOOBUR_AUTO_ACTIVITY_PRESET);
            device.sendDeviceUpdateIntent(context);
        }
    }

    @NonNull
    private static String shortAutoLabel(Context context, String preset) {
        if (preset == null || TooburAutoActivitySwitchPackets.PRESET_OFF.equals(preset)) {
            return context.getString(R.string.off);
        }
        switch (preset) {
            case TooburAutoActivitySwitchPackets.PRESET_WALK_RUN:
                return context.getString(R.string.toobur_device_card_auto_label_wr);
            case TooburAutoActivitySwitchPackets.PRESET_WALK_RUN_BIKE:
                return context.getString(R.string.toobur_device_card_auto_label_wrb);
            case TooburAutoActivitySwitchPackets.PRESET_WALK_RUN_BIKE_SWIM:
                return context.getString(R.string.toobur_device_card_auto_label_wrb_sw);
            case TooburAutoActivitySwitchPackets.PRESET_ALL_SPORTS:
                return context.getString(R.string.toobur_device_card_auto_label_all);
            default:
                return context.getString(R.string.on);
        }
    }
}
