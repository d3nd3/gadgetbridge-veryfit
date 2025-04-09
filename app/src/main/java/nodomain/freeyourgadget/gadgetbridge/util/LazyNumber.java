package nodomain.freeyourgadget.gadgetbridge.util;

import java.io.Serializable;
import java.util.function.Supplier;

public class LazyNumber implements Serializable {
    private Number value = 0;

    public LazyNumber() {
    }

    public synchronized Number compute(final Supplier<Number> supplier) {
        if (value == null || value.intValue() == 0) {
            value = supplier.get();
        }
        return value;
    }

    public synchronized boolean isComputed() {
        return value.intValue() != 0;
    }

    public synchronized void reset() {
        value = 0;
    }
}
