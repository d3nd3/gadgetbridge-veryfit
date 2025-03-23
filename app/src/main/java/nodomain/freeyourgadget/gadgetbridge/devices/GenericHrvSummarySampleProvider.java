/*  Copyright (C) 2025 José Rebelo

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

package nodomain.freeyourgadget.gadgetbridge.devices;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.HrvSummarySample;
import nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class GenericHrvSummarySampleProvider implements TimeSampleProvider<HrvSummarySample> {
    private final TimeSampleProvider<? extends HrvValueSample> valueSampleProvider;

    public GenericHrvSummarySampleProvider(final TimeSampleProvider<? extends HrvValueSample> valueSampleProvider) {
        this.valueSampleProvider = valueSampleProvider;
    }

    @NonNull
    @Override
    public List<HrvSummarySample> getAllSamples(final long timestampFrom, final long timestampTo) {
        final List<HrvSummarySample> ret = new ArrayList<>();
        // Go backwards day by day and compute the rolling weekly average
        // FIXME: This is inefficient
        for (long ts = timestampTo; ts > timestampFrom; ts -= 24 * 60 * 60 * 1000L) {
            ret.add(computeSummary(ts));
        }
        return ret;
    }

    @Override
    public void addSample(final HrvSummarySample timeSample) {
        throw new UnsupportedOperationException("This sample provider is read-only!");
    }

    @Override
    public void addSamples(final List<HrvSummarySample> timeSamples) {
        throw new UnsupportedOperationException("This sample provider is read-only!");
    }

    @Override
    public HrvSummarySample createSample() {
        throw new UnsupportedOperationException("This sample provider is read-only!");
    }

    @Nullable
    @Override
    public HrvSummarySample getLatestSample() {
        final HrvValueSample latestValue = valueSampleProvider.getLatestSample();
        if (latestValue == null) {
            return null;
        }
        return computeSummary(latestValue.getTimestamp());
    }

    @Nullable
    @Override
    public HrvSummarySample getLatestSample(final long until) {
        final HrvValueSample latestValue = valueSampleProvider.getLatestSample(until);
        if (latestValue == null) {
            return null;
        }
        return computeSummary(latestValue.getTimestamp());
    }

    @Nullable
    @Override
    public HrvSummarySample getFirstSample() {
        final HrvValueSample sample = valueSampleProvider.getFirstSample();
        if (sample == null) {
            return null;
        }
        return new GenericHrvSummarySample(sample.getTimestamp(), sample.getValue());
    }

    private HrvSummarySample computeSummary(final long timestampTo) {
        // We need to fetch all value samples up to 1w back and compute the weekly average
        final long weekStart = DateTimeUtils.dayStart(new Date(timestampTo - 7 * 24 * 60 * 60 * 1000L)).getTime();
        final List<? extends HrvValueSample> valueSamples = valueSampleProvider.getAllSamples(weekStart, timestampTo);

        final int average = (int) valueSamples.stream()
                .mapToInt(HrvValueSample::getValue)
                .average()
                .orElse(0);

        return new GenericHrvSummarySample(timestampTo, average);
    }

    public static class GenericHrvSummarySample implements HrvSummarySample {
        private final long timestamp;
        private final int weeklyAverage;

        public GenericHrvSummarySample(final long timestamp, final int weeklyAverage) {
            this.timestamp = timestamp;
            this.weeklyAverage = weeklyAverage;
        }

        @Override
        public Integer getWeeklyAverage() {
            return weeklyAverage;
        }

        @Override
        public Integer getLastNightAverage() {
            return 0;
        }

        @Override
        public Integer getLastNight5MinHigh() {
            return 0;
        }

        @Override
        public Integer getBaselineLowUpper() {
            return 0;
        }

        @Override
        public Integer getBaselineBalancedLower() {
            return 0;
        }

        @Override
        public Integer getBaselineBalancedUpper() {
            return 0;
        }

        @Override
        public Status getStatus() {
            return Status.NONE;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }
    }
}
