/*  Copyright (C) 2025 idowatch / TOOBUR device support — same layout as {@link TooburV3HrParser} */
package nodomain.freeyourgadget.gadgetbridge.service.devices.toobur;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/**
 * Parses v3 health sync cmd {@code 0x04} for <strong>dataType 1</strong> (SpO₂) and
 * <strong>dataType 2</strong> (pressure/stress) using the same heuristics as HR, with a value range filter.
 */
final class TooburV3DayMetricParser {
    static final class MetricPoint {
        final int year;
        final int month;
        final int day;
        final int secondOfDay;
        final int value;

        MetricPoint(int year, int month, int day, int secondOfDay, int value) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.secondOfDay = secondOfDay;
            this.value = value;
        }
    }

    public static final class ParsedDayMetric {
        public final List<MetricPoint> points;
        public final String parsingModel;
        public final String note;

        ParsedDayMetric(List<MetricPoint> points, String parsingModel, String note) {
            this.points = points;
            this.parsingModel = parsingModel;
            this.note = note;
        }
    }

    private TooburV3DayMetricParser() {
    }

    static long toEpochMillis(int year, int month, int day, int secondOfDay) {
        Calendar c = new GregorianCalendar(year, month - 1, day);
        c.add(Calendar.SECOND, secondOfDay);
        return c.getTimeInMillis();
    }

    @Nullable
    static ParsedDayMetric parse(int itemCount, @NonNull byte[] headerBytes, @NonNull byte[] dataBytes,
            int minValue, int maxValue) {
        Head7003 head = parse7003Header(headerBytes);
        List<MetricPoint> rows = new ArrayList<>();
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
                    int v = d[j * 2 + 1] & 0xFF;
                    sec += off;
                    if (v >= minValue && v <= maxValue) {
                        rows.add(new MetricPoint(head.year, head.month, head.day, sec, v));
                    }
                }
                note = "JNI-7003; u8 delta sec + u8 value";
            } else if (n > 0 && d.length >= 3 * n) {
                for (int j = 0; j < n; j++) {
                    int o = j * 3;
                    int minuteOff = u16le(d, o);
                    int v = d[o + 2] & 0xFF;
                    if (v >= minValue && v <= maxValue) {
                        int sod = head.startTime + minuteOff * 60;
                        rows.add(new MetricPoint(head.year, head.month, head.day, sod, v));
                    }
                }
                note = "JNI-7003; u16 minute + u8 value";
            } else if (n > 0 && d.length >= n) {
                int perItem = d.length / n;
                if (perItem == 1) {
                    for (int i = 0; i < n; i++) {
                        int v = d[i] & 0xFF;
                        if (v >= minValue && v <= maxValue) {
                            rows.add(new MetricPoint(head.year, head.month, head.day, head.startTime + i * 60, v));
                        }
                    }
                    note = "JNI-7003; 1 B / min index";
                } else if (perItem >= 2) {
                    for (int i = 0; i < n; i++) {
                        int o = i * perItem;
                        int minuteOff = u16le(d, o);
                        int v = d[o + 2] & 0xFF;
                        if (v >= minValue && v <= maxValue) {
                            rows.add(new MetricPoint(head.year, head.month, head.day, head.startTime + minuteOff * 60, v));
                        }
                    }
                    note = "JNI-7003; packed u16 minute + value";
                } else {
                    note = "JNI-7003; unknown perItem";
                }
            } else if (d.length > 0 && n == 0) {
                for (int i = 0; i < d.length; i++) {
                    int v = d[i] & 0xFF;
                    if (v >= minValue && v <= maxValue) {
                        rows.add(new MetricPoint(head.year, head.month, head.day, head.startTime + i * 60, v));
                    }
                }
                note = "JNI-7003; itemCount=0 raw";
            } else {
                note = rows.isEmpty() && d.length == 0 ? "no payload" : "JNI-7003 length mismatch";
            }
            return new ParsedDayMetric(rows, model, note);
        }

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
                    int v = d[i] & 0xFF;
                    if (v >= minValue && v <= maxValue) {
                        rows.add(new MetricPoint(year, month, day, secondOffset + i * 60, v));
                    }
                }
                note = "legacy 1B/min";
            } else if (perItem >= 2) {
                for (int i = 0; i < n; i++) {
                    int o = i * perItem;
                    int minuteOff = u16le(d, o);
                    int v = d[o + 2] & 0xFF;
                    if (v >= minValue && v <= maxValue) {
                        rows.add(new MetricPoint(year, month, day, secondOffset + minuteOff * 60, v));
                    }
                }
                note = "legacy u16 min + value";
            } else {
                note = "legacy unknown perItem";
            }
        } else if (d.length > 0 && n == 0) {
            for (int i = 0; i < d.length; i++) {
                int v = d[i] & 0xFF;
                if (v >= minValue && v <= maxValue) {
                    rows.add(new MetricPoint(year, month, day, secondOffset + i * 60, v));
                }
            }
            note = "legacy itemCount=0";
        } else {
            note = "no samples";
        }
        return new ParsedDayMetric(rows, model, note);
    }

    private static final class Head7003 {
        final int year;
        final int month;
        final int day;
        final int startTime;
        final int dataType;
        final int silent;

        Head7003(int year, int month, int day, int startTime, int dataType, int silent) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.startTime = startTime;
            this.dataType = dataType;
            this.silent = silent;
        }
    }

    @Nullable
    private static Head7003 parse7003Header(byte[] h) {
        for (int base = 0; base <= 1; base++) {
            if (h.length < base + 25) {
                continue;
            }
            int year = u16le(h, base);
            if (year < 2018 || year > 2036) {
                continue;
            }
            int month = h[base + 2] & 0xFF;
            int day = h[base + 3] & 0xFF;
            if (month < 1 || month > 12 || day < 1 || day > 31) {
                continue;
            }
            int startTime = u32le(h, base + 4);
            int dataType = h[base + 8] & 0xFF;
            int silent = h[base + 9] & 0xFF;
            return new Head7003(year, month, day, startTime, dataType, silent);
        }
        return null;
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
    static String formatLogSummary(@NonNull ParsedDayMetric p) {
        return String.format(Locale.US, "model=%s points=%d | %s", p.parsingModel, p.points.size(), p.note);
    }
}
