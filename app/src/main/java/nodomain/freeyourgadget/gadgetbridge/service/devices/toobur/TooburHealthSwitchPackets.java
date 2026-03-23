/*  Copyright (C) 2025 idowatch / TOOBUR — SET 0x03 0x44 SpO₂, 0x03 0x45 pressure (see packetdumps/logcat/set_stress_cont_*.txt) */
package nodomain.freeyourgadget.gadgetbridge.service.devices.toobur;

import nodomain.freeyourgadget.gadgetbridge.devices.id115.ID115Constants;

/**
 * Fixed-layout SET packets for continuous SpO₂ and stress (pressure) switches on VeryFit / IDO.
 * On/off uses {@code 0xAA} / {@code 0x55} like other ID115 switches (byte index 2 after cmd key).
 */
final class TooburHealthSwitchPackets {

    /** SET_PRESSURE capture: on — {@code 03 45 AA 09 00 12 00 55 3F 3C 00 50 00 01 00 1E} */
    private static final byte[] PRESSURE_TEMPLATE_ON = new byte[]{
            ID115Constants.CMD_ID_SETTINGS, 0x45,
            (byte) 0xAA, 0x09, 0x00, 0x12, 0x00, 0x55, 0x3F, 0x3C, 0x00, 0x50, 0x00, 0x01, 0x00, 0x1E
    };
    /** SET_PRESSURE capture: off — only byte[2] differs */
    private static final byte[] PRESSURE_TEMPLATE_OFF = new byte[]{
            ID115Constants.CMD_ID_SETTINGS, 0x45,
            0x55, 0x09, 0x00, 0x12, 0x00, 0x55, 0x3F, 0x3C, 0x00, 0x50, 0x00, 0x01, 0x00, 0x1E
    };

    /** SET SpO₂ — user doc: {@code 03 44 AA 09 00 12 00 01 00 00 01} */
    private static final byte[] SPO2_TEMPLATE_ON = new byte[]{
            ID115Constants.CMD_ID_SETTINGS, 0x44,
            (byte) 0xAA, 0x09, 0x00, 0x12, 0x00, 0x01, 0x00, 0x00, 0x01
    };
    private static final byte[] SPO2_TEMPLATE_OFF = new byte[]{
            ID115Constants.CMD_ID_SETTINGS, 0x44,
            0x55, 0x09, 0x00, 0x12, 0x00, 0x01, 0x00, 0x00, 0x01
    };

    private TooburHealthSwitchPackets() {
    }

    static byte[] buildPressureSwitch(boolean on) {
        return on ? copyOf(PRESSURE_TEMPLATE_ON) : copyOf(PRESSURE_TEMPLATE_OFF);
    }

    static byte[] buildSpo2Switch(boolean on) {
        return on ? copyOf(SPO2_TEMPLATE_ON) : copyOf(SPO2_TEMPLATE_OFF);
    }

    private static byte[] copyOf(byte[] src) {
        byte[] out = new byte[src.length];
        System.arraycopy(src, 0, out, 0, src.length);
        return out;
    }
}
