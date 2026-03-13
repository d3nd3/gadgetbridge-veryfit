/*  Copyright (C) 2025 idowatch / TOOBUR device support

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.toobur;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.id115.ID115Constants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.id115.ID115Support;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;

/**
 * TOOBUR device support: extends ID115 with battery (level + voltage), device info,
 * live data (steps/HR), find device/phone, music control, and device-specific settings.
 * GET replies and live data are received on 0x0AF7.
 */
public class TooburSupport extends ID115Support {
    private static final Logger LOG = LoggerFactory.getLogger(TooburSupport.class);

    /** Preference key: music on device enabled (SET 0x03 0x2A). */
    public static final String PREF_TOOBUR_MUSIC_ENABLED = "toobur_music_enabled";
    /** Preference key: call/notification alert enabled (SET 0x03 0x30). */
    public static final String PREF_TOOBUR_CALL_ALERT_ENABLED = "toobur_call_alert_enabled";
    /** Preference key: do not disturb enabled (SET 0x03 0x29). */
    public static final String PREF_TOOBUR_DND_ENABLED = "toobur_dnd_enabled";
    /** Preference key: raise-to-wake / up hand gesture (SET 0x03 0x28). */
    public static final String PREF_TOOBUR_RAISE_TO_WAKE = "toobur_raise_to_wake";
    /** Preference key: one-key SOS (SET 0x03 0x2C). */
    public static final String PREF_TOOBUR_SOS_ENABLED = "toobur_sos_enabled";
    /** Preference key: heart rate mode — off / auto / manual (SET 0x03 0x25). Values: "off", "auto", "manual". */
    public static final String PREF_TOOBUR_HR_MODE = "toobur_heart_rate_mode";
    /** Preference key: real-time HR stream on device (SET 0x03 0x52, second byte). */
    public static final String PREF_TOOBUR_REALTIME_HR_ENABLED = "toobur_realtime_hr_enabled";
    /** Preference key: weather on watch (SET 0x03 0x2D). Only if func table weather bit is set. */
    public static final String PREF_TOOBUR_WEATHER_ENABLED = "toobur_weather_enabled";

    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        normalWriteCharacteristic = getCharacteristic(ID115Constants.UUID_CHARACTERISTIC_WRITE_NORMAL);
        healthWriteCharacteristic = getCharacteristic(ID115Constants.UUID_CHARACTERISTIC_WRITE_HEALTH);

        builder.setDeviceState(GBDevice.State.INITIALIZING);

        // Enable notifications on 0x0AF7 so we receive GET replies (battery, device info)
        builder.notify(ID115Constants.UUID_CHARACTERISTIC_NOTIFY_NORMAL, true);

        setTime(builder)
                .setWrist(builder)
                .setScreenOrientation(builder)
                .setGoal(builder);

        // Request battery info (GET 0x02 0x05); reply will arrive via onCharacteristicChanged
        builder.write(normalWriteCharacteristic, new byte[]{
                ID115Constants.CMD_ID_GET_INFO,
                ID115Constants.CMD_KEY_GET_BATT_INFO
        });
        // Request device info (GET 0x02 0x01) for firmware/version on the card
        builder.write(normalWriteCharacteristic, new byte[]{
                ID115Constants.CMD_ID_GET_INFO,
                ID115Constants.CMD_KEY_GET_DEVICE_INFO
        });
        // Request live data (GET 0x02 0xA0) for steps, calories, distance, active time, HR
        builder.write(normalWriteCharacteristic, new byte[]{
                ID115Constants.CMD_ID_GET_INFO,
                ID115Constants.CMD_KEY_GET_LIVE_DATA
        });

        builder.setDeviceState(GBDevice.State.INITIALIZED);
        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        return builder;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          byte[] data) {
        UUID uuid = characteristic.getUuid();
        if (!ID115Constants.UUID_CHARACTERISTIC_NOTIFY_NORMAL.equals(uuid)) {
            return super.onCharacteristicChanged(gatt, characteristic, data);
        }

        if (data == null || data.length < 2) {
            return super.onCharacteristicChanged(gatt, characteristic, data);
        }

        byte cmd = data[0];
        byte key = data[1];

        // Battery info reply (protocol_device_batt_info): head(2), type(1), voltage(2 LE mV), status(1), level(1)
        if (cmd == ID115Constants.CMD_ID_GET_INFO && key == ID115Constants.CMD_KEY_GET_BATT_INFO && data.length >= 7) {
            int voltageMv = (data[3] & 0xFF) | ((data[4] & 0xFF) << 8);
            int status = data[5] & 0xFF;
            int level = data[6] & 0xFF;

            batteryCmd.batteryIndex = 0;
            batteryCmd.level = level <= 100 ? level : GBDevice.BATTERY_UNKNOWN;
            batteryCmd.voltage = voltageMv > 0 ? voltageMv / 1000f : -1f;
            batteryCmd.state = statusToBatteryState(status);
            handleGBDeviceEvent(batteryCmd);
            LOG.debug("TOOBUR battery: {}%, {} mV, status {}", batteryCmd.level, voltageMv, status);
            return true;
        }

        // Device info reply (protocol_device_info): head(2), device_id(2), version(1), mode(1), ...
        if (cmd == ID115Constants.CMD_ID_GET_INFO && key == ID115Constants.CMD_KEY_GET_DEVICE_INFO && data.length >= 8) {
            int deviceId = (data[2] & 0xFF) | ((data[3] & 0xFF) << 8);
            int version = data[4] & 0xFF;
            versionCmd.fwVersion = String.format("v%d", version);
            versionCmd.fwVersion2 = String.format("ID 0x%04X", deviceId);
            handleGBDeviceEvent(versionCmd);
            LOG.debug("TOOBUR device info: id=0x{}, version={}", Integer.toHexString(deviceId), version);
            return true;
        }

        // Live data reply (GET 0x02 0xA0): head(2), steps(4 LE), calories(4 LE), distance(4 LE), active_time(4 LE), heart_rate(1)
        if (cmd == ID115Constants.CMD_ID_GET_INFO && key == ID115Constants.CMD_KEY_GET_LIVE_DATA && data.length >= 19) {
            int steps = (data[2] & 0xFF) | ((data[3] & 0xFF) << 8) | ((data[4] & 0xFF) << 16) | ((data[5] & 0xFF) << 24);
            int calories = (data[6] & 0xFF) | ((data[7] & 0xFF) << 8) | ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 24);
            int distance = (data[10] & 0xFF) | ((data[11] & 0xFF) << 8) | ((data[12] & 0xFF) << 16) | ((data[13] & 0xFF) << 24);
            int activeTime = (data[14] & 0xFF) | ((data[15] & 0xFF) << 8) | ((data[16] & 0xFF) << 16) | ((data[17] & 0xFF) << 24);
            int heartRate = data[18] & 0xFF;
            LOG.debug("TOOBUR live data: steps={}, calories={}, distance={}, activeTime={}, HR={}", steps, calories, distance, activeTime, heartRate);
            return true;
        }

        return super.onCharacteristicChanged(gatt, characteristic, data);
    }

    private static BatteryState statusToBatteryState(int status) {
        switch (status) {
            case 1:
                return BatteryState.BATTERY_CHARGING;
            case 2:
                return BatteryState.BATTERY_CHARGING_FULL;
            case 3:
                return BatteryState.BATTERY_LOW;
            default:
                return BatteryState.BATTERY_NORMAL;
        }
    }

    @Override
    public void onFindDevice(boolean start) {
        try {
            TransactionBuilder builder = performInitialized("find_device");
            builder.write(normalWriteCharacteristic, new byte[]{
                    ID115Constants.CMD_ID_APP_CONTROL,
                    ID115Constants.CMD_KEY_APP_FIND_DEVICE,
                    start ? ID115Constants.CMD_ARG_APP_START : ID115Constants.CMD_ARG_APP_STOP
            });
            builder.queue();
        } catch (IOException e) {
            LOG.warn("Unable to send find device", e);
        }
    }

    @Override
    public void onFindPhone(boolean start) {
        try {
            TransactionBuilder builder = performInitialized("find_phone");
            // SET 0x03 0x26: status (1=start), timeout_seconds (e.g. 30)
            builder.write(normalWriteCharacteristic, new byte[]{
                    ID115Constants.CMD_ID_SETTINGS,
                    ID115Constants.CMD_KEY_SET_FIND_PHONE,
                    (byte) (start ? 1 : 0),
                    (byte) 30
            });
            builder.queue();
        } catch (IOException e) {
            LOG.warn("Unable to send find phone", e);
        }
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        try {
            TransactionBuilder builder = performInitialized("music_control");
            // Control 0x06 0x01: 0x00 = start/play, 0x01 = stop/pause
            byte cmd = (stateSpec.state == MusicStateSpec.STATE_PLAYING)
                    ? ID115Constants.CMD_ARG_APP_START
                    : ID115Constants.CMD_ARG_APP_STOP;
            builder.write(normalWriteCharacteristic, new byte[]{
                    ID115Constants.CMD_ID_APP_CONTROL,
                    ID115Constants.CMD_KEY_APP_MUSIC,
                    cmd
            });
            builder.queue();
        } catch (IOException e) {
            LOG.warn("Unable to send music control", e);
        }
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        try {
            int slots = getDevice().getType().getDeviceCoordinator().getAlarmSlotCount(getDevice());
            TransactionBuilder builder = performInitialized("set_alarms");

            for (int slot = 0; slot < slots; slot++) {
                Alarm alarm = null;
                for (Alarm a : alarms) {
                    if (a.getPosition() == slot) {
                        alarm = a;
                        break;
                    }
                }
                byte status = (alarm != null && alarm.getEnabled()) ? ID115Constants.CMD_ARG_MUSIC_ON : (byte) 0x55;  // 0xAA on, 0x55 off
                int hour = alarm != null ? alarm.getHour() : 0;
                int minute = alarm != null ? alarm.getMinute() : 0;
                int repeat = alarm != null ? (alarm.getRepetition() & 0x7F) : 0;
                int snoozeMin = (alarm != null && alarm.getSnooze()) ? 5 : 0;
                // protocol_set_alarm: type 0=wake_up, 1=sleep, 2=sport, ...; we use 0
                builder.write(normalWriteCharacteristic, new byte[]{
                        ID115Constants.CMD_ID_SETTINGS,
                        ID115Constants.CMD_KEY_SET_ALARM,
                        (byte) slot,
                        status,
                        (byte) 0,
                        (byte) hour,
                        (byte) minute,
                        (byte) repeat,
                        (byte) snoozeMin
                });
            }
            builder.queue();
            LOG.info("Sent {} alarm slot(s) to device", slots);
        } catch (IOException e) {
            LOG.warn("Unable to send alarms", e);
        }
    }

    @Override
    public void onSendConfiguration(String config) {
        try {
            TransactionBuilder builder = performInitialized("config_" + config);
            var prefs = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress());

            switch (config) {
                case DeviceSettingsPreferenceConst.PREF_WEARLOCATION:
                    setWrist(builder);
                    break;
                case DeviceSettingsPreferenceConst.PREF_SCREEN_ORIENTATION:
                    setScreenOrientation(builder);
                    break;
                case PREF_TOOBUR_MUSIC_ENABLED:
                    builder.write(normalWriteCharacteristic, new byte[]{
                            ID115Constants.CMD_ID_SETTINGS,
                            ID115Constants.CMD_KEY_SET_MUSIC_SWITCH,
                            prefs.getBoolean(PREF_TOOBUR_MUSIC_ENABLED, true) ? ID115Constants.CMD_ARG_MUSIC_ON : ID115Constants.CMD_ARG_MUSIC_OFF
                    });
                    break;
                case PREF_TOOBUR_CALL_ALERT_ENABLED:
                    boolean callOn = prefs.getBoolean(PREF_TOOBUR_CALL_ALERT_ENABLED, true);
                    builder.write(normalWriteCharacteristic, new byte[]{
                            ID115Constants.CMD_ID_SETTINGS,
                            ID115Constants.CMD_KEY_SET_NOTICE,
                            (byte) (callOn ? 1 : 0), 0, 0, (byte) (callOn ? 1 : 0), 3
                    });
                    break;
                case PREF_TOOBUR_DND_ENABLED:
                    boolean dndOn = prefs.getBoolean(PREF_TOOBUR_DND_ENABLED, false);
                    builder.write(normalWriteCharacteristic, new byte[]{
                            ID115Constants.CMD_ID_SETTINGS,
                            ID115Constants.CMD_KEY_SET_DO_NOT_DISTURB,
                            (byte) (dndOn ? 1 : 0), 0, 0, 0, 0
                    });
                    break;
                case PREF_TOOBUR_RAISE_TO_WAKE:
                    builder.write(normalWriteCharacteristic, new byte[]{
                            ID115Constants.CMD_ID_SETTINGS,
                            ID115Constants.CMD_KEY_SET_UP_HAND_GESTURE,
                            prefs.getBoolean(PREF_TOOBUR_RAISE_TO_WAKE, true) ? ID115Constants.CMD_ARG_GESTURE_ON : ID115Constants.CMD_ARG_GESTURE_OFF,
                            3
                    });
                    break;
                case PREF_TOOBUR_SOS_ENABLED:
                    builder.write(normalWriteCharacteristic, new byte[]{
                            ID115Constants.CMD_ID_SETTINGS,
                            ID115Constants.CMD_KEY_SET_ONE_KEY_SOS,
                            prefs.getBoolean(PREF_TOOBUR_SOS_ENABLED, false) ? ID115Constants.CMD_ARG_SOS_ON : ID115Constants.CMD_ARG_SOS_OFF
                    });
                    break;
                case PREF_TOOBUR_HR_MODE: {
                    String mode = prefs.getString(PREF_TOOBUR_HR_MODE, "auto");
                    byte modeByte = ID115Constants.CMD_ARG_HR_MODE_AUTO;
                    if ("off".equals(mode)) {
                        modeByte = ID115Constants.CMD_ARG_HR_MODE_OFF;
                    } else if ("manual".equals(mode)) {
                        modeByte = ID115Constants.CMD_ARG_HR_MODE_MANUAL;
                    }
                    builder.write(normalWriteCharacteristic, new byte[]{
                            ID115Constants.CMD_ID_SETTINGS,
                            ID115Constants.CMD_KEY_SET_HR_MODE,
                            modeByte
                    });
                    break;
                }
                case PREF_TOOBUR_REALTIME_HR_ENABLED:
                    // SET 0x03 0x52: gsensor_status, heart_rate_sensor_status (0/1 or 0x55/0xAA)
                    byte hrSensor = prefs.getBoolean(PREF_TOOBUR_REALTIME_HR_ENABLED, true)
                            ? (byte) 0x01 : 0x55;
                    builder.write(normalWriteCharacteristic, new byte[]{
                            ID115Constants.CMD_ID_SETTINGS,
                            ID115Constants.CMD_KEY_SET_REALTIME_SENSOR_STATUS,
                            0x01,  // gsensor on
                            hrSensor
                    });
                    break;
                case PREF_TOOBUR_WEATHER_ENABLED:
                    // SET 0x03 0x2D: cmd1, cmd2, cmd3 — 1,0,0 = on; 0,0,0 = off (only if func table weather)
                    byte weatherOn = prefs.getBoolean(PREF_TOOBUR_WEATHER_ENABLED, false) ? (byte) 1 : 0;
                    builder.write(normalWriteCharacteristic, new byte[]{
                            ID115Constants.CMD_ID_SETTINGS,
                            ID115Constants.CMD_KEY_SET_WEATHER_SWITCH,
                            weatherOn, 0, 0
                    });
                    break;
                default:
                    return;
            }
            builder.queue();
        } catch (IOException e) {
            LOG.warn("Unable to send configuration {}", config, e);
        }
    }
}
