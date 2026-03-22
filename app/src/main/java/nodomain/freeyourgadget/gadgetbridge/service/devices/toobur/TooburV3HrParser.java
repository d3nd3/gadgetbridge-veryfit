/*  Copyright (C) 2025 idowatch / TOOBUR device support — see htmlapp/toobur-hr-csv.html */
package nodomain.freeyourgadget.gadgetbridge.service.devices.toobur;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses v3 health sync cmd {@code 0x04} <strong>dataType 3</strong> (HR) payloads using the same
 * heuristics as {@code htmlapp/toobur-hr-csv.html} {@code parseHr7003Header} / {@code parseHrV3}
 * (JNI callback 7003 / sub_16DE1C layout).
 */
final class TooburV3HrParser {
    static final class HrPoint {
        final int year;
        final int month;
        final int day;
        /** Seconds from local midnight (may exceed 86400 when using delta-accumulation model). */
        final int secondOfDay;
        final int bpm;

        HrPoint(int year, int month, int day, int secondOfDay, int bpm) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.secondOfDay = secondOfDay;
            this.bpm = bpm;
        }
    }

    static final class ParsedHr {
        final List<HrPoint> points;
        final String parsingModel;
        final String note;

        ParsedHr(List<HrPoint> points, String parsingModel, String note) {
            this.points = points;
            this.parsingModel = parsingModel;
            this.note = note;
        }
    }

    private TooburV3HrParser() {
    }

    @Nullable
    static ParsedHr parse(int itemCount, @NonNull byte[] headerBytes, @NonNull byte[] dataBytes) {
        Hr7003Head head = parseHr7003Header(headerBytes);
        List<HrPoint> rows = new ArrayList<>();
        String note;
        String model;
        int n = itemCount;
        byte[] d = dataBytes;
        byte[] h = headerBytes;

        if (head != null) {
            model = "native_7003";
            if (n > 0 && d.length >= 2 * n) {
                int sec = head.startTime;
                for (int j = 0; j < n; j++) {
                    int off = d[j * 2] & 0xFF;
                    int bpm = d[j * 2 + 1] & 0xFF;
                    sec += off;
                    if (bpm >= 1 && bpm <= 250) {
                        rows.add(new HrPoint(head.year, head.month, head.day, sec, bpm));
                    }
                }
                note = "JNI-7003; u8 delta sec + u8 BPM pairs";
            } else if (n > 0 && d.length >= 3 * n) {
                for (int j = 0; j < n; j++) {
                    int o = j * 3;
                    int minuteOff = u16le(d, o);
                    int bpm = d[o + 2] & 0xFF;
                    if (bpm >= 1 && bpm <= 250) {
                        int sod = head.startTime + minuteOff * 60;
                        rows.add(new HrPoint(head.year, head.month, head.day, sod, bpm));
                    }
                }
                note = "JNI-7003; u16 minute + u8 BPM";
            } else if (n > 0 && d.length >= n) {
                int perItem = d.length / n;
                if (perItem == 1) {
                    for (int i = 0; i < n; i++) {
                        int bpm = d[i] & 0xFF;
                        if (bpm >= 1 && bpm <= 250) {
                            rows.add(new HrPoint(head.year, head.month, head.day, head.startTime + i * 60, bpm));
                        }
                    }
                    note = "JNI-7003; 1 B BPM / min index";
                } else if (perItem >= 2) {
                    for (int i = 0; i < n; i++) {
                        int o = i * perItem;
                        int minuteOff = u16le(d, o);
                        int bpm = d[o + 2] & 0xFF;
                        if (bpm >= 1 && bpm <= 250) {
                            rows.add(new HrPoint(head.year, head.month, head.day, head.startTime + minuteOff * 60, bpm));
                        }
                    }
                    note = "JNI-7003; packed u16 minute + BPM";
                } else {
                    note = "JNI-7003; unknown perItem";
                }
            } else if (d.length > 0 && n == 0) {
                for (int i = 0; i < d.length; i++) {
                    int bpm = d[i] & 0xFF;
                    if (bpm >= 30 && bpm <= 220) {
                        rows.add(new HrPoint(head.year, head.month, head.day, head.startTime + i * 60, bpm));
                    }
                }
                note = "JNI-7003; itemCount=0 raw BPM";
            } else {
                note = rows.isEmpty() && d.length == 0 ? "no HR payload" : "JNI-7003 length mismatch";
            }
            return new ParsedHr(rows, model, note);
        }

        /* legacy heuristic */
        model = "legacy_heuristic";
        int year = 0;
        int month = 0;
        int day = 0;
        int secondOffset = 0;
        if (h.length >= 5) {
            year = u16le(h, 1);
            month = h[3] & 0xFF;
            day = h[4] & 0xFF;
        }
        if (h.length >= 9) {
            secondOffset = u32le(h, 5);
        }
        if (n > 0 && d.length >= n) {
            int perItem = d.length / n;
            if (perItem == 1) {
                for (int i = 0; i < n; i++) {
                    int bpm = d[i] & 0xFF;
                    if (bpm >= 1 && bpm <= 250) {
                        rows.add(new HrPoint(year, month, day, secondOffset + i * 60, bpm));
                    }
                }
                note = "legacy 1B/min";
            } else if (perItem >= 2) {
                for (int i = 0; i < n; i++) {
                    int o = i * perItem;
                    int minuteOff = u16le(d, o);
                    int bpm = d[o + 2] & 0xFF;
                    if (bpm >= 1 && bpm <= 250) {
                        rows.add(new HrPoint(year, month, day, secondOffset + minuteOff * 60, bpm));
                    }
                }
                note = "legacy u16 min + BPM";
            } else {
                note = "legacy unknown perItem";
            }
        } else if (d.length > 0 && n == 0) {
            for (int i = 0; i < d.length; i++) {
                int bpm = d[i] & 0xFF;
                if (bpm >= 30 && bpm <= 220) {
                    rows.add(new HrPoint(year, month, day, secondOffset + i * 60, bpm));
                }
            }
            note = "legacy itemCount=0";
        } else {
            note = "no HR samples";
        }
        return new ParsedHr(rows, model, note);
    }

    private static final class Hr7003Head {
        final int year;
        final int month;
        final int day;
        final int startTime;
        final int dataType;
        final int silentHr;

        Hr7003Head(int year, int month, int day, int startTime, int dataType, int silentHr) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.startTime = startTime;
            this.dataType = dataType;
            this.silentHr = silentHr;
        }
    }

    @Nullable
    private static Hr7003Head parseHr7003Header(byte[] h) {
        for (int base = 0; base <= 1; base++) {
            if (h.length < base + 25) {
                continue;
            }
            int year = u16le(h, base);
            if (!plausibleHrYear(year)) {
                continue;
            }
            int month = h[base + 2] & 0xFF;
            int day = h[base + 3] & 0xFF;
            if (month < 1 || month > 12 || day < 1 || day > 31) {
                continue;
            }
            int startTime = u32le(h, base + 4);
            int dataType = h[base + 8] & 0xFF;
            int silentHR = h[base + 9] & 0xFF;
            return new Hr7003Head(year, month, day, startTime, dataType, silentHR);
        }
        return null;
    }

    private static boolean plausibleHrYear(int y) {
        return y >= 2018 && y <= 2036;
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

    @NonNull
    static String formatLogSummary(@NonNull ParsedHr p) {
        return String.format(Locale.US, "model=%s points=%d | %s", p.parsingModel, p.points.size(), p.note);
    }
}
