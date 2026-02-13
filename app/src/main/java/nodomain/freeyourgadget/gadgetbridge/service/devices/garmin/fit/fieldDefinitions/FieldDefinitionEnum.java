package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import java.nio.ByteBuffer;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionEnum<E extends Enum<E>> extends FieldDefinition {
    private final Class<E> enumClass;

    public FieldDefinitionEnum(final int localNumber,
                               final String name,
                               final Class<E> enumClass) {
        super(localNumber, 1, BaseType.ENUM, name, 1, 0);
        this.enumClass = enumClass;
    }

    @Override
    public E decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj != null) {
            return Objects.requireNonNull(enumClass.getEnumConstants())[(int) rawObj];
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (enumClass.isInstance(o)) {
            baseType.encode(byteBuffer, Objects.requireNonNull(enumClass.cast(o)).ordinal(), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }
}
