package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitRecordDataBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionCoursePoint.CoursePoint;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitCoursePoint extends RecordData {
    public FitCoursePoint(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 32) {
            throw new IllegalArgumentException("FitCoursePoint expects global messages of " + 32 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(1);
    }

    @Nullable
    public Double getLatitude() {
        return (Double) getFieldByNumber(2);
    }

    @Nullable
    public Double getLongitude() {
        return (Double) getFieldByNumber(3);
    }

    @Nullable
    public Double getDistance() {
        return (Double) getFieldByNumber(4);
    }

    @Nullable
    public CoursePoint getType() {
        return (CoursePoint) getFieldByNumber(5);
    }

    @Nullable
    public String getName() {
        return (String) getFieldByNumber(6);
    }

    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(32);
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setLatitude(final Double value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setLongitude(final Double value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setDistance(final Double value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setType(final CoursePoint value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setName(final String value) {
            setFieldByNumber(6, value);
            return this;
        }

        @Override
        public FitCoursePoint build() {
            return (FitCoursePoint) super.build();
        }
    }
}
