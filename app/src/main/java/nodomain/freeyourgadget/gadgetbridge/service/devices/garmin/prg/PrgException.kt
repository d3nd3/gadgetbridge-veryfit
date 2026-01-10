package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.prg

class PrgException(
    override val message: String,
    override val cause: Throwable,
): Exception(message, cause)
