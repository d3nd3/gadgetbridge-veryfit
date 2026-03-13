/*  Copyright (C) 2018-2024 Andreas Shimokawa, Vadim Kaushan

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
package nodomain.freeyourgadget.gadgetbridge.devices.id115;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.BASE_UUID;

public class ID115Constants {
    public static final UUID UUID_SERVICE_ID115 = UUID.fromString(String.format(BASE_UUID, "0AF0"));
    public static final UUID UUID_CHARACTERISTIC_WRITE_NORMAL = UUID.fromString(String.format(BASE_UUID, "0AF6"));
    public static final UUID UUID_CHARACTERISTIC_NOTIFY_NORMAL = UUID.fromString(String.format(BASE_UUID, "0AF7"));
    public static final UUID UUID_CHARACTERISTIC_WRITE_HEALTH = UUID.fromString(String.format(BASE_UUID, "0AF1"));
    public static final UUID UUID_CHARACTERISTIC_NOTIFY_HEALTH = UUID.fromString(String.format(BASE_UUID, "0AF2"));

    public static final byte CMD_ID_WARE_UPDATE = 0x01;
    public static final byte CMD_ID_GET_INFO = 0x02;
    public static final byte CMD_ID_SETTINGS = 0x03;
    public static final byte CMD_ID_BIND_UNBIND = 0x04;
    public static final byte CMD_ID_NOTIFY = 0x05;
    public static final byte CMD_ID_APP_CONTROL = 0x06;
    public static final byte CMD_ID_BLE_CONTROL = 0x07;
    public static final byte CMD_ID_HEALTH_DATA = 0x08;
    public static final byte CMD_ID_DUMP_STACK = 0x20;
    public static final byte CMD_ID_LOG = 0x21;
    public static final byte CMD_ID_FACTORY = (byte)0xaa;
    public static final byte CMD_ID_DEVICE_RESTART = (byte)0xf0;

    // CMD_ID_GET_INFO keys (GET 0x02)
    public static final byte CMD_KEY_GET_DEVICE_INFO = 0x01;
    public static final byte CMD_KEY_GET_FUNC_TABLE = 0x02;
    public static final byte CMD_KEY_GET_DEVICE_TIME = 0x03;
    public static final byte CMD_KEY_GET_MAC = 0x04;
    public static final byte CMD_KEY_GET_BATT_INFO = 0x05;
    public static final byte CMD_KEY_GET_SN_INFO = 0x06;
    public static final byte CMD_KEY_GET_NOTICE_STATUS = 0x10;
    public static final byte CMD_KEY_GET_LIVE_DATA = (byte) 0xA0;
    public static final byte CMD_KEY_GET_GSENSOR_PARAM = 0x21;      // GET 0x02 0x21 — rate, range, threshold (reply ≥7 bytes)

    // CMD_ID_SETTINGS
    public static final byte CMD_KEY_SET_TIME = 0x01;
    public static final byte CMD_KEY_SET_ALARM = 0x02;
    public static final byte CMD_KEY_SET_GOAL = 0x03;
    public static final byte CMD_KEY_SET_HAND = 0x22;
    public static final byte CMD_ARG_LEFT = 0x00;
    public static final byte CMD_ARG_RIGHT = 0x01;
    public static final byte CMD_KEY_SET_FIND_PHONE = 0x26;
    public static final byte CMD_KEY_SET_FACTORY_DEFAULTS = 0x27;
    public static final byte CMD_KEY_SET_UP_HAND_GESTURE = 0x28;
    public static final byte CMD_KEY_SET_DO_NOT_DISTURB = 0x29;
    public static final byte CMD_KEY_SET_MUSIC_SWITCH = 0x2A;
    public static final byte CMD_KEY_SET_DISPLAY_MODE = 0x2B;
    public static final byte CMD_KEY_SET_ONE_KEY_SOS = 0x2C;
    public static final byte CMD_KEY_SET_WEATHER_SWITCH = 0x2D;
    public static final byte CMD_KEY_SET_NOTICE = 0x30;
    public static final byte CMD_ARG_MUSIC_ON = (byte) 0xAA;
    public static final byte CMD_ARG_MUSIC_OFF = 0x55;
    public static final byte CMD_ARG_GESTURE_ON = (byte) 0xAA;
    public static final byte CMD_ARG_GESTURE_OFF = 0x55;
    public static final byte CMD_ARG_SOS_ON = (byte) 0xAA;
    public static final byte CMD_ARG_SOS_OFF = 0x55;

    // Heart rate (Angelfit / TOOBUR) — func table main_func bit 5 = heart_rate, ohter2 bit 3 = heart_rate_monitor
    public static final byte CMD_KEY_GET_HR_SENSOR_PARAM = 0x20;   // GET 0x02 0x20 — rate, led_select
    public static final byte CMD_KEY_SET_HR_INTERVAL = 0x24;       // SET 0x03 0x24 — burn_fat, aerobic, limit (1–255 bpm)
    public static final byte CMD_KEY_SET_HR_MODE = 0x25;           // SET 0x03 0x25 — 1 byte mode
    public static final byte CMD_KEY_SET_HR_SENSOR_PARAM = 0x50;   // SET 0x03 0x50 — low-level sensor config
    public static final byte CMD_KEY_SET_REALTIME_SENSOR_STATUS = 0x52; // SET 0x03 0x52 — gsensor_status, heart_rate_sensor_status (2 bytes)
    // Heart rate mode (SET 0x03 0x25): protocol_heart_rate_mode
    public static final byte CMD_ARG_HR_MODE_OFF = 0x55;           // close — heart rate off
    public static final byte CMD_ARG_HR_MODE_AUTO = (byte) 0x88;   // auto — continuous monitoring (VeryFit default)
    public static final byte CMD_ARG_HR_MODE_MANUAL = (byte) 0xAA; // manual — manual measurement only

    // CMD_ID_NOTIFY
    public static final byte CMD_KEY_NOTIFY_CALL = 0x01;
    public static final byte CMD_KEY_NOTIFY_STOP = 0x02;
    public static final byte CMD_KEY_NOTIFY_MSG = 0x03;

    // CMD_ID_APP_CONTROL (0x06)
    public static final byte CMD_KEY_APP_MUSIC = 0x01;
    public static final byte CMD_KEY_APP_PHOTO = 0x02;
    public static final byte CMD_KEY_APP_SINGLE_SPORT = 0x03;
    public static final byte CMD_KEY_APP_FIND_DEVICE = 0x04;
    public static final byte CMD_KEY_APP_OPEN_ANCS = 0x30;
    public static final byte CMD_KEY_APP_CLOSE_ANCS = 0x31;
    public static final byte CMD_ARG_APP_START = 0x00;
    public static final byte CMD_ARG_APP_STOP = 0x01;

    // CMD_ID_HEALTH_DATA
    public static final byte CMD_KEY_FETCH_ACTIVITY_TODAY = 0x03;

    // CMD_ID_DEVICE_RESTART
    public static final byte CMD_KEY_REBOOT = 0x01;

    // Display mode (for SET 0x2B) - keep for ID115
    public static final byte CMD_ARG_HORIZONTAL = 0x00;
    public static final byte CMD_ARG_VERTICAL = 0x02;

    public static byte getNotificationType(NotificationType type) {
        switch (type) {
//            case GENERIC_EMAIL:
//                return 2; // Icon is not supported
            case WECHAT:
                return 3;
//            case QQ:
//                return 4;
            case FACEBOOK:
                return 6;
            case TWITTER:
                return 7;
            case WHATSAPP:
                return 8;
            case FACEBOOK_MESSENGER:
                return 9;
            case INSTAGRAM:
                return 10;
            case LINKEDIN:
                return 11;
//            case GENERIC_CALENDAR:
//                return 12; // Icon is not supported
//            case SKYPE:
//                return 13; // Icon is not supported
//            case LINE:
//                return 17; // Icon is not supported
//            case VIBER:
//                return 18; // Icon is not supported
//            case KAKAO_TALK:
//                return 19; // Icon is not supported
//            case VK:
//                return 16; // Icon is not supported
//            case GMAIL:
//                return 20; // Icon is not supported
//            case OUTLOOK:
//                return 21; // Icon is not supported
//            case SNAPCHAT:
//                return 22; // Icon is not supported
        }
        return 1;
    }
}
