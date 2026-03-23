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

import org.slf4j.Logger;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * VeryFit / IDO v3 heart-rate mode: cmd {@code 0x0009} (evt 5010) is written on
 * {@code 0x0AF6} (same as {@code htmlapp/toobur-hr-csv.html} TX); {@code 0x0AF1} is for v3 bulk sync.
 * Use a single unified 26-byte frame via {@link #buildHrUnified}.
 */
final class TooburV3HrPackets {
    /** v3 cmd LE at bytes 8–9 (raw 0x0AF2) / 7–8 (reassembled without leading {@code 0x33}). */
    static final int V3_CMD_HR_UNIFIED = 0x0009;

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
     * @param intervalSeconds one of {@code 5, 60, 180, 300, 600, 900, 1800} or {@code 255} (smart / intensity-based)
     */
    public static byte[] buildHrUnified(boolean continuousOn, int intervalSeconds, int seq) {
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

    /** Device-supported fixed intervals (seconds). {@code 255} is handled separately (smart HR). */
    private static final int[] HR_INTERVAL_FIXED_SEC = {5, 60, 180, 300, 600, 900, 1800};

    /**
     * Normalize to a supported wire value: {@code 5, 60, 180, 300, 600, 900, 1800} or {@code 255} (smart).
     * Unknown values snap to the nearest fixed interval (legacy prefs).
     */
    public static int clampInterval(int seconds) {
        if (seconds == 255) {
            return 255;
        }
        for (int v : HR_INTERVAL_FIXED_SEC) {
            if (seconds == v) {
                return v;
            }
        }
        int best = 300;
        int bestDist = Integer.MAX_VALUE;
        for (int v : HR_INTERVAL_FIXED_SEC) {
            int d = Math.abs(seconds - v);
            if (d < bestDist) {
                bestDist = d;
                best = v;
            }
        }
        return best;
    }

    private static int u16le(byte[] d, int off) {
        if (d == null || d.length < off + 2) {
            return 0;
        }
        return (d[off] & 0xFF) | ((d[off + 1] & 0xFF) << 8);
    }

    /**
     * If {@code data} is a raw 0x0AF2 notify with v3 magic {@code 0x33} and cmd {@link #V3_CMD_HR_UNIFIED},
     * logs INFO (matches VeryFit {@code protocol_receive_data} lines e.g. app_fresh_launch.txt:184).
     *
     * @return true if this was a 0x09 frame (caller may still forward to super)
     */
    public static boolean logV3Hr09RxIfPresent(byte[] data, Logger log) {
        if (data == null || data.length < 12 || log == null || !log.isInfoEnabled()) {
            return false;
        }
        if ((data[0] & 0xFF) != 0x33) {
            return false;
        }
        if (u16le(data, 8) != V3_CMD_HR_UNIFIED) {
            return false;
        }
        int innerLen = u16le(data, 6);
        int nseq = u16le(data, 10);
        int intervalEcho = data.length >= 24 ? u16le(data, 22) : -1;
        boolean crcOk = data.length >= 4 && crc16CcittFalse(data, 1, data.length - 3)
                == (u16le(data, data.length - 2));
        log.info("TOOBUR HR: v3 0x09 RX (0x0AF2) nseq=0x{} innerLen={} intervalEcho={}s crcOk={} hex={}",
                String.format(Locale.US, "%04X", nseq & 0xFFFF),
                innerLen,
                intervalEcho,
                crcOk,
                GB.hexdump(data));
        return true;
    }

    /**
     * Log reassembled v3 frame without leading {@code 0x33} (see {@link TooburV3HealthSync.V3ReassemblyBuffer}).
     */
    public static void logV3Hr09StrippedRx(byte[] frame, Logger log) {
        if (frame == null || frame.length < 12 || log == null || !log.isInfoEnabled()) {
            return;
        }
        if (u16le(frame, 7) != V3_CMD_HR_UNIFIED) {
            return;
        }
        int innerLen = u16le(frame, 5);
        int nseq = u16le(frame, 9);
        int intervalEcho = frame.length >= 23 ? u16le(frame, 21) : -1;
        log.info("TOOBUR HR: v3 0x09 RX (0x0AF2 reassembled) nseq=0x{} innerLen={} intervalEcho={}s hex={}",
                String.format(Locale.US, "%04X", nseq & 0xFFFF),
                innerLen,
                intervalEcho,
                GB.hexdump(frame));
    }
}
