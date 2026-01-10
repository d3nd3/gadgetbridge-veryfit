package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.prg

class PrgSectionContainer(
    val magic: PrgSectionMagic,
    val length: UInt,
    val content: PrgSection,
)

sealed class PrgSection

object PrgSectionEnd : PrgSection()

data class PrgSectionUnknown(val bytes: ByteArray) : PrgSection() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PrgSectionUnknown

        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

data class PrgSectionHead(
    val headerVersion: UByte,
    val connectIqVersionMajor: UByte,
    val connectIqVersionMinor: UByte,
    val connectIqVersionHotfix: UByte,
    val backgroundOffset: PrgOffset,
    // Optional
    val appLockIndicator: UByte?,
    val unused1: UInt?,
    val unused2: UInt?,
    val glanceOffsets: PrgOffset?,
    val flags: UInt?
) : PrgSection() {
    fun appLock(): Boolean {
        return appLockIndicator != null && appLockIndicator != 0.toUByte()
    }

    fun glanceSupport(): Boolean {
        return flags != null && (flags and 0x01.toUInt()) != 0.toUInt()
    }

    fun profilingEnabled(): Boolean {
        return flags != null && (flags and 0x02.toUInt()) != 0.toUInt()
    }
}

data class PrgOffset(val dataOffset: UInt, val codeOffset: UInt)
