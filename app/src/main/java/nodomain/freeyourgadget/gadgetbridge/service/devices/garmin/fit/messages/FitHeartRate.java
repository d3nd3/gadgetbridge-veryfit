package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitHeartRate extends RecordData {
    public FitHeartRate(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 132) {
            throw new IllegalArgumentException("FitHeartRate expects global messages of " + 132 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getFractionalTimestamp() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Number[] getFilteredBpm() {
        final Object[] objectsArray = (Object[]) getFieldByNumber(6);
        if (objectsArray == null)
            return null;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Long getEventTimestamp() {
        return (Long) getFieldByNumber(9);
    }

    @Nullable
    public Number[] getEventTimestamp12() {
        final Object[] objectsArray = (Object[]) getFieldByNumber(10);
        if (objectsArray == null)
            return null;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }
}
