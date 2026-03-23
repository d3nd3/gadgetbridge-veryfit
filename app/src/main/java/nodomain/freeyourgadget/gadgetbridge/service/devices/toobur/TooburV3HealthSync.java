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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Locale;

/**
 * VeryFit v3 health sync: cmd {@code 0x0005} (get sizes by type/offset) and {@code 0x0004} (sync with
 * operate start/stop). Matches {@code packetdumps/logcat/sync_example.txt} and
 * {@code htmlapp/confirmed-only.html}.
 */
final class TooburV3HealthSync {
    private static final Logger LOG = LoggerFactory.getLogger(TooburV3HealthSync.class);

    static final int V3_CMD_HEALTH_SYNC = 0x0004;
    static final int V3_CMD_HEALTH_SIZES = 0x0005;
    private static final int V3_HEALTH_TYPE_SPO2 = 0x01;
    private static final int V3_HEALTH_TYPE_PRESSURE = 0x02;
    private static final int V3_HEALTH_TYPE_HR = 0x03;
    private static final int V3_HEALTH_TYPE_SPORT_SUMMARY = 0x08;
    private static final int TX_PACKET_LEN = 19;
    /** First v3 TX chunk: preamble + inner length 0x0088 + cmd 0x05 + seq + 35 B (7× type+offset) + pad + CRC. */
    private static final int V3_HEALTH_SIZES_TX_LEN = 137;

    /** Operate byte after seq in cmd 0x04 (see sync_example / protocol_v3_health_client). */
    static final byte V3_HEALTH_OPERATE_START = 0x00;
    static final byte V3_HEALTH_OPERATE_STOP = 0x01;

    /** Sync order: SpO2, pressure, HR, activity, swim, sleep, sport — matches VeryFit log. */
    static final int[] V3_HEALTH_SYNC_DATA_TYPES = {0x01, 0x02, 0x03, 0x04, 0x06, 0x07, 0x08};

    private TooburV3HealthSync() {
    }

    /**
     * v3 cmd {@code 0x0004}: operate + data type + byte14 + 16-bit save offset LE at bytes 15–16.
     */
    static byte[] buildHealthSync04(byte operate, int dataType, int seq, int byte14, int saveOffset16) {
        byte[] packet = new byte[TX_PACKET_LEN];
        packet[0] = 0x33;
        packet[1] = (byte) 0xDA;
        packet[2] = (byte) 0xAD;
        packet[3] = (byte) 0xDA;
        packet[4] = (byte) 0xAD;
        packet[5] = 0x01;
        packet[6] = 0x10;
        packet[7] = 0x00;
        packet[8] = 0x04;
        packet[9] = 0x00;
        packet[10] = (byte) (seq & 0xFF);
        packet[11] = (byte) ((seq >> 8) & 0xFF);
        packet[12] = operate;
        packet[13] = (byte) (dataType & 0xFF);
        packet[14] = (byte) (byte14 & 0xFF);
        packet[15] = (byte) (saveOffset16 & 0xFF);
        packet[16] = (byte) ((saveOffset16 >> 8) & 0xFF);
        TooburV3HrPackets.finalizeCrc(packet);
        return packet;
    }

    static int defaultByte14ForDataType(int dataType) {
        switch (dataType) {
            case 0x04:
            case 0x06:
            case 0x07:
                return 0x00;
            default:
                return 0x01;
        }
    }

    /**
     * v3 “get health sizes” cmd {@code 0x0005}: one chunk ({@code sync_example.txt} / get_sync_health_v3).
     * Payload after seq: for each entry, 1 byte type + 4-byte delta offset LE (0 = from start).
     *
     * @param offsets32 seven offsets (bytes into stored stream per type), same order as {@link #V3_HEALTH_SYNC_DATA_TYPES}
     */
    static byte[] buildV3HealthSizesRequest(int seq, @NonNull int[] offsets32) {
        if (offsets32.length != V3_HEALTH_SYNC_DATA_TYPES.length) {
            throw new IllegalArgumentException("offsets32 must have length " + V3_HEALTH_SYNC_DATA_TYPES.length);
        }
        byte[] packet = new byte[V3_HEALTH_SIZES_TX_LEN];
        int o = 0;
        packet[o++] = 0x33;
        packet[o++] = (byte) 0xDA;
        packet[o++] = (byte) 0xAD;
        packet[o++] = (byte) 0xDA;
        packet[o++] = (byte) 0xAD;
        packet[o++] = 0x01;
        packet[o++] = (byte) 0x88;
        packet[o++] = 0x00;
        packet[o++] = 0x05;
        packet[o++] = 0x00;
        packet[o++] = (byte) (seq & 0xFF);
        packet[o++] = (byte) ((seq >> 8) & 0xFF);
        for (int i = 0; i < V3_HEALTH_SYNC_DATA_TYPES.length; i++) {
            packet[o++] = (byte) (V3_HEALTH_SYNC_DATA_TYPES[i] & 0xFF);
            int off = offsets32[i];
            packet[o++] = (byte) (off & 0xFF);
            packet[o++] = (byte) ((off >> 8) & 0xFF);
            packet[o++] = (byte) ((off >> 16) & 0xFF);
            packet[o++] = (byte) ((off >> 24) & 0xFF);
        }
        while (o < packet.length - 2) {
            packet[o++] = 0x00;
        }
        TooburV3HrPackets.finalizeCrc(packet);
        return packet;
    }

    /** @return aggregate total bytes to sync (u32 LE at offset 11 after seq), or -1 if too short */
    static int parseV3HealthSizesReplyTotal(@NonNull byte[] frame) {
        if (frame.length < 15) {
            return -1;
        }
        return u32le(frame, 11);
    }

    public interface ReplyListener {
        /**
         * Cmd {@code 0x0005} reply: aggregate size (delta from requested offsets), e.g. sync all size in logcat.
         */
        void onV3HealthSizesTotal(int totalBytes);

        /**
         * Cmd {@code 0x0004} reply with {@code dataType == 3} (HR stream) — parsed like {@code toobur-hr-csv.html}.
         */
        default void onV3HrParsed(@NonNull TooburV3HrParser.ParsedHr parsed, int dataSizeBytes) {
        }

        /** Cmd {@code 0x0004} reply with {@code dataType == 1} (SpO₂ day stream). */
        default void onV3Spo2Parsed(@NonNull TooburV3DayMetricParser.ParsedDayMetric parsed, int dataSizeBytes) {
        }

        /** Cmd {@code 0x0004} reply with {@code dataType == 2} (pressure / stress day stream). */
        default void onV3PressureParsed(@NonNull TooburV3DayMetricParser.ParsedDayMetric parsed, int dataSizeBytes) {
        }

        /** Cmd {@code 0x0004} reply with {@code dataType == 8} (sport / daily summary) — steps, distance, etc. */
        default void onV3SportSummary(@NonNull V3SportSummary summary) {
        }
    }

    /**
     * @deprecated use {@link #buildHealthSync04(byte, int, int, int, int)}
     */
    @Deprecated
    static byte[] buildHealthSyncStart(byte operate, int dataType, int seq) {
        return buildHealthSync04(operate, dataType, seq, defaultByte14ForDataType(dataType), 0);
    }

    /**
     * Reassembled v3 frame (no leading {@code 0x33}) — first byte is {@code DA} like
     * {@code confirmed-only.html} {@code dispatchV3Reply} / {@code handleV3ReplyReassembly}.
     *
     * @return true if this notification was consumed as v3 (fragment or full); legacy 0x02… should not run.
     */
    static boolean consumeNotifyFragment(@NonNull V3ReassemblyBuffer rx, @NonNull byte[] data,
            @Nullable ReplyListener listener) {
        if (data.length == 0) {
            return false;
        }
        if ((data[0] & 0xFF) != 0x33 && !rx.isReceiving()) {
            return false;
        }
        if (!rx.feed(data)) {
            return false;
        }
        byte[] complete;
        while ((complete = rx.pollComplete()) != null) {
            dispatchV3Reply(complete, listener);
        }
        return true;
    }

    /**
     * Feed 0x0AF2 notify into the reassembly buffer without dispatching; caller polls
     * {@link V3ReassemblyBuffer#pollComplete()} then runs state machine and calls {@link #dispatchV3Reply}.
     */
    static boolean feedNotifyFragmentRx(@NonNull V3ReassemblyBuffer rx, @NonNull byte[] data) {
        if (data.length == 0) {
            return false;
        }
        if ((data[0] & 0xFF) != 0x33 && !rx.isReceiving()) {
            return false;
        }
        return rx.feed(data);
    }

    static void dispatchV3Reply(@NonNull byte[] frame, @Nullable ReplyListener listener) {
        if (frame.length < 12) {
            return;
        }
        int cmd = u16le(frame, 7);
        if (cmd == V3_CMD_HEALTH_SIZES) {
            int total = parseV3HealthSizesReplyTotal(frame);
            if (total >= 0) {
                LOG.debug("[V3 health] cmd 0x05 totalBytes={} (aggregate from offsets)", total);
                if (listener != null) {
                    listener.onV3HealthSizesTotal(total);
                }
            }
            return;
        }
        if (cmd != V3_CMD_HEALTH_SYNC) {
            return;
        }
        final byte[] payload = frame.length > 11 ? Arrays.copyOfRange(frame, 11, frame.length) : new byte[0];
        V3HealthCommon common = parseV3HealthCommon(payload);
        if (common == null) {
            return;
        }
        if (common.dataType == V3_HEALTH_TYPE_HR) {
            int headerStart = 14;
            int headerEnd = Math.min(payload.length, headerStart + common.headSize);
            int dataEnd = Math.min(payload.length, headerEnd + common.dataSize);
            byte[] headerBytes = Arrays.copyOfRange(payload, headerStart, headerEnd);
            byte[] dataBytes = headerEnd < dataEnd ? Arrays.copyOfRange(payload, headerEnd, dataEnd) : new byte[0];
            TooburV3HrParser.ParsedHr parsed = TooburV3HrParser.parse(common.itemCount, headerBytes, dataBytes);
            if (parsed != null) {
                LOG.info("[V3 HR] {} items={} head={} data={}",
                        TooburV3HrParser.formatLogSummary(parsed), common.itemCount, common.headSize, common.dataSize);
                if (listener != null) {
                    listener.onV3HrParsed(parsed, common.dataSize);
                }
            }
            return;
        }
        if (common.dataType == V3_HEALTH_TYPE_SPO2) {
            int headerStart = 14;
            int headerEnd = Math.min(payload.length, headerStart + common.headSize);
            int dataEnd = Math.min(payload.length, headerEnd + common.dataSize);
            byte[] headerBytes = Arrays.copyOfRange(payload, headerStart, headerEnd);
            byte[] dataBytes = headerEnd < dataEnd ? Arrays.copyOfRange(payload, headerEnd, dataEnd) : new byte[0];
            TooburV3DayMetricParser.ParsedDayMetric parsed = TooburV3DayMetricParser.parse(
                    common.itemCount, headerBytes, dataBytes, 50, 100);
            if (parsed != null) {
                LOG.info("[V3 SpO2] {} items={} head={} data={}",
                        TooburV3DayMetricParser.formatLogSummary(parsed), common.itemCount, common.headSize, common.dataSize);
                if (listener != null) {
                    listener.onV3Spo2Parsed(parsed, common.dataSize);
                }
            }
            return;
        }
        if (common.dataType == V3_HEALTH_TYPE_PRESSURE) {
            int headerStart = 14;
            int headerEnd = Math.min(payload.length, headerStart + common.headSize);
            int dataEnd = Math.min(payload.length, headerEnd + common.dataSize);
            byte[] headerBytes = Arrays.copyOfRange(payload, headerStart, headerEnd);
            byte[] dataBytes = headerEnd < dataEnd ? Arrays.copyOfRange(payload, headerEnd, dataEnd) : new byte[0];
            TooburV3DayMetricParser.ParsedDayMetric parsed = TooburV3DayMetricParser.parse(
                    common.itemCount, headerBytes, dataBytes, 0, 100);
            if (parsed != null) {
                LOG.info("[V3 stress] {} items={} head={} data={}",
                        TooburV3DayMetricParser.formatLogSummary(parsed), common.itemCount, common.headSize, common.dataSize);
                if (listener != null) {
                    listener.onV3PressureParsed(parsed, common.dataSize);
                }
            }
            return;
        }
        if (common.dataType != V3_HEALTH_TYPE_SPORT_SUMMARY) {
            LOG.debug("[V3 health] cmd 0x04 dataType=0x{} items={} (skip non-sport summary)", Integer.toHexString(common.dataType & 0xFF), common.itemCount);
            return;
        }
        int headerStart = 14;
        int headerEnd = Math.min(payload.length, headerStart + common.headSize);
        int dataEnd = Math.min(payload.length, headerEnd + common.dataSize);
        if (headerEnd < headerStart + 20) {
            LOG.warn("[V3 sport] header too short (headSize={})", common.headSize);
            return;
        }
        byte[] headerBytes = Arrays.copyOfRange(payload, headerStart, headerEnd);
        byte[] dataBytes = headerEnd < dataEnd ? Arrays.copyOfRange(payload, headerEnd, dataEnd) : new byte[0];

        V3SportSummary summary = parseV3SportSummary(headerBytes, dataBytes, common.itemCount);
        if (summary == null) {
            return;
        }
        LOG.info(formatSportLogLine(common, summary));
        if (listener != null) {
            listener.onV3SportSummary(summary);
        }
    }

    private static int u16le(byte[] b, int off) {
        if (b.length < off + 2) {
            return 0;
        }
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
    }

    private static int u32le(byte[] b, int off) {
        if (b.length < off + 4) {
            return 0;
        }
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8) | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
    }

    private static final class V3HealthCommon {
        final int oper;
        final int dataType;
        final int itemCount;
        final int headSize;
        final int dataSize;

        V3HealthCommon(int oper, int dataType, int itemCount, int headSize, int dataSize) {
            this.oper = oper;
            this.dataType = dataType;
            this.itemCount = itemCount;
            this.headSize = headSize;
            this.dataSize = dataSize;
        }
    }

    @Nullable
    private static V3HealthCommon parseV3HealthCommon(byte[] payload) {
        if (payload == null || payload.length < 14) {
            return null;
        }
        return new V3HealthCommon(
                payload[0] & 0xFF,
                payload[1] & 0xFF,
                u16le(payload, 5),
                u16le(payload, 7),
                u32le(payload, 9)
        );
    }

    /** Parsed v3 sport / daily summary (data type {@code 0x08}) — see {@code packetdumps/logcat/sync_example.txt} / TOOBUR.md. */
    public static final class V3SportSummary {
        public final int year;
        public final int month;
        public final int day;
        public final int minuteOffset;
        public final int intervalMinutes;
        public final int totalSteps;
        public final int totalCalories;
        public final int headerDisplayCalories;
        public final int itemDisplayCalories;
        public final int rawTotalCalories;
        public final int totalDistance;
        public final int coveredUntilMinutes;
        public final int totalActiveTime;

        V3SportSummary(int year, int month, int day, int minuteOffset, int intervalMinutes,
                int totalSteps, int totalCalories, int headerDisplayCalories, int itemDisplayCalories,
                int rawTotalCalories, int totalDistance, int coveredUntilMinutes, int totalActiveTime) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.minuteOffset = minuteOffset;
            this.intervalMinutes = intervalMinutes;
            this.totalSteps = totalSteps;
            this.totalCalories = totalCalories;
            this.headerDisplayCalories = headerDisplayCalories;
            this.itemDisplayCalories = itemDisplayCalories;
            this.rawTotalCalories = rawTotalCalories;
            this.totalDistance = totalDistance;
            this.coveredUntilMinutes = coveredUntilMinutes;
            this.totalActiveTime = totalActiveTime;
        }
    }

    @Nullable
    private static V3SportSummary parseV3SportSummary(byte[] headerBytes, byte[] dataBytes, int itemCount) {
        if (headerBytes == null || headerBytes.length < 20) {
            return null;
        }
        int headerSteps = u32le(headerBytes, 8);
        int rawTotalCalories = u32le(headerBytes, 12);
        int totalDistance = u32le(headerBytes, 16);
        int headerDisplayCalories = headerBytes.length >= 28 ? u16le(headerBytes, 26) : 0;

        int rawActiveTime = headerBytes.length >= 24 ? u32le(headerBytes, 20) : 0;
        int totalActiveTime = rawActiveTime > 86400 ? 0 : rawActiveTime;

        int itemTotalsDisplay = 0;
        if (dataBytes != null && itemCount > 0) {
            int itemSize = itemCount > 0 ? dataBytes.length / itemCount : 0;
            if (itemSize == 10 && dataBytes.length >= itemCount * itemSize) {
                for (int i = 0; i < itemCount; i++) {
                    int off = i * itemSize;
                    itemTotalsDisplay += dataBytes[off + 3] & 0xFF;
                }
            }
        }

        int totalCalories = headerDisplayCalories != 0
                ? headerDisplayCalories
                : (itemTotalsDisplay != 0 ? itemTotalsDisplay : rawTotalCalories);

        int coveredUntilMinutes = u16le(headerBytes, 5) + (itemCount * (headerBytes[7] & 0xFF));

        return new V3SportSummary(
                u16le(headerBytes, 1),
                headerBytes[3] & 0xFF,
                headerBytes[4] & 0xFF,
                u16le(headerBytes, 5),
                headerBytes[7] & 0xFF,
                headerSteps,
                totalCalories,
                headerDisplayCalories,
                itemTotalsDisplay,
                rawTotalCalories,
                totalDistance,
                coveredUntilMinutes,
                totalActiveTime
        );
    }

    private static String formatDateParts(int year, int month, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
    }

    private static String formatMinutesOfDay(int totalMinutes) {
        if (totalMinutes < 0) {
            return "—";
        }
        int h = (totalMinutes / 60) % 24;
        int m = totalMinutes % 60;
        return String.format(Locale.US, "%02d:%02d", h, m);
    }

    @NonNull
    private static String formatSportLogLine(
            @NonNull V3HealthCommon common,
            @NonNull V3SportSummary summary) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("[V3 sport] date=").append(formatDateParts(summary.year, summary.month, summary.day));
        sb.append(" interval=").append(summary.intervalMinutes).append("m");
        sb.append(" offset=").append(summary.minuteOffset);
        sb.append(" items=").append(common.itemCount);
        sb.append(" steps=").append(summary.totalSteps);
        sb.append(" kcal=").append(summary.totalCalories);
        sb.append(" headerKcal=").append(summary.headerDisplayCalories);
        sb.append(" itemKcal=").append(summary.itemDisplayCalories);
        sb.append(" rawKcal=").append(summary.rawTotalCalories);
        sb.append(" distance=").append(summary.totalDistance);
        sb.append(" coveredUntil=").append(formatMinutesOfDay(summary.coveredUntilMinutes));
        if (summary.totalActiveTime > 0) {
            sb.append(" active=").append(summary.totalActiveTime).append("s");
        }
        return sb.toString();
    }

    // —— Reassembly (same logic as confirmed-only.html handleV3ReplyReassembly) ——

    static final class V3ReassemblyBuffer {
        private static final int MAX_FRAME = 65536;
        byte[] buffer;
        int written;

        boolean feed(byte[] arr) {
            if (arr == null || arr.length == 0) {
                return false;
            }
            boolean inReassembly = buffer != null && written > 0 && written < buffer.length;

            // Continuation chunk: starts with 0x33 but not full magic header
            if (inReassembly && arr[0] == 0x33 && !isFullMagicHeader(arr)) {
                int skip = 1;
                int toCopy = Math.min(arr.length - skip, buffer.length - written);
                if (toCopy > 0) {
                    System.arraycopy(arr, skip, buffer, written, toCopy);
                    written += toCopy;
                }
                return true;
            }

            if (arr.length >= 10 && arr[0] == 0x33 && arr[1] == (byte) 0xDA && arr[2] == (byte) 0xAD && arr[3] == (byte) 0xDA && arr[4] == (byte) 0xAD) {
                int totalLen = (arr[6] & 0xFF) | ((arr[7] & 0xFF) << 8);
                if (totalLen <= 0 || totalLen > MAX_FRAME) {
                    reset();
                    return true;
                }
                buffer = new byte[totalLen];
                written = 0;
                int toCopy = Math.min(totalLen, arr.length - 1);
                if (toCopy > 0) {
                    System.arraycopy(arr, 1, buffer, 0, toCopy);
                    written = toCopy;
                }
                return true;
            }

            return false;
        }

        private static boolean isFullMagicHeader(byte[] arr) {
            return arr.length >= 5 && arr[1] == (byte) 0xDA && arr[2] == (byte) 0xAD && arr[3] == (byte) 0xDA && arr[4] == (byte) 0xAD;
        }

        @Nullable
        byte[] pollComplete() {
            if (buffer != null && written < buffer.length) {
                return null;
            }
            if (buffer != null && written == buffer.length && written > 0) {
                byte[] done = buffer;
                reset();
                return done;
            }
            return null;
        }

        void reset() {
            buffer = null;
            written = 0;
        }

        boolean isReceiving() {
            return buffer != null && written > 0 && written < buffer.length;
        }
    }
}
