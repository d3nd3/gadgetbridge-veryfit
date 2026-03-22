/*  Copyright (C) 2025 idowatch / TOOBUR device support

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.toobur;

/**
 * VeryFit / IDO v3 heart-rate mode on {@code 0x0AF6}: cmd {@code 0x0009} (evt 5010).
 * <p>
 * One 26-byte frame: {@code UPDATE_TIMESTAMP(4)} + {@code ON_OFF(2)} +
 * null {@code START-END(4)} + {@code INTERVAL(2 LE)} + CRC16 — same shape as
 * {@code htmlapp/toobur-hr-csv.html} {@code buildV3HrUnifiedPacket} / dual-step1-only captures
 * (e.g. OFF: {@code AA 00} + {@code 00 00 00 00} + interval; ON: {@code CC 00} + same).
 * </p>
 */
final class TooburV3HrPackets {
    static final int PACKET_LEN = 26;
    private static final byte[] MAGIC = {
            0x33, (byte) 0xDA, (byte) 0xAD, (byte) 0xDA, (byte) 0xAD,
            0x01, 0x17, 0x00, 0x09, 0x00
    };

    /** Continuous HR ON uses 0xCC; OFF uses 0xAA (see toobur-hr-csv.html). */
    private static final byte HR_ON_STATE = (byte) 0xCC;
    private static final byte HR_OFF_STATE = (byte) 0xAA;

    private TooburV3HrPackets() {
    }

    /**
     * CRC-16-CCITT-FALSE over {@code data[offset .. offset+length)}.
     * Same algorithm as {@code htmlapp/toobur-hr-csv.html} {@code crc16Ccitt}.
     */
    static int crc16CcittFalse(byte[] data, int offset, int length) {
        int crc = 0xFFFF;
        final int end = offset + length;
        for (int i = offset; i < end; i++) {
            crc ^= (data[i] & 0xFF) << 8;
            for (int k = 0; k < 8; k++) {
                crc = ((crc & 0x8000) != 0) ? ((crc << 1) ^ 0x1021) : (crc << 1);
                crc &= 0xFFFF;
            }
        }
        return crc;
    }

    static void finalizeCrc(byte[] packet) {
        int crc = crc16CcittFalse(packet, 1, packet.length - 3);
        packet[packet.length - 2] = (byte) (crc & 0xFF);
        packet[packet.length - 1] = (byte) ((crc >> 8) & 0xFF);
    }

    /**
     * Single v3 HR command: on/off and/or interval change (no legacy SET {@code 0x03 0x25}).
     *
     * @param continuousOn true = ON ({@code 0xCC}), false = OFF ({@code 0xAA})
     * @param intervalSeconds measurement interval in seconds; {@code 255} = smart/dynamic HR
     */
    static byte[] buildHrUnified(boolean continuousOn, int intervalSeconds, int seq) {
        byte[] p = new byte[PACKET_LEN];
        System.arraycopy(MAGIC, 0, p, 0, MAGIC.length);
        p[10] = (byte) (seq & 0xFF);
        p[11] = (byte) ((seq >> 8) & 0xFF);

        long nowSec = System.currentTimeMillis() / 1000L;
        p[12] = (byte) (nowSec & 0xFF);
        p[13] = (byte) ((nowSec >> 8) & 0xFF);
        p[14] = (byte) ((nowSec >> 16) & 0xFF);
        p[15] = (byte) ((nowSec >> 24) & 0xFF);

        p[16] = continuousOn ? HR_ON_STATE : HR_OFF_STATE;
        p[17] = 0x00;
        // bytes 18–21: null time-range (already zero)

        int iv = clampInterval(intervalSeconds);
        p[22] = (byte) (iv & 0xFF);
        p[23] = (byte) ((iv >> 8) & 0xFF);

        finalizeCrc(p);
        return p;
    }

    /**
     * Valid wire values: fixed seconds (5–3600) or 255 = smart / dynamic HR.
     */
    static int clampInterval(int seconds) {
        if (seconds == 255) {
            return 255;
        }
        if (seconds < 5) {
            return 5;
        }
        if (seconds > 3600) {
            return 3600;
        }
        return seconds;
    }
}
