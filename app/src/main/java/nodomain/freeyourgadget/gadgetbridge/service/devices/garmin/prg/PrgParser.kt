package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.prg

import nodomain.freeyourgadget.gadgetbridge.util.kotlin.getUInt
import java.nio.ByteBuffer
import kotlin.jvm.Throws

object PrgParser {
    @Throws(PrgException::class)
    fun parse(data: ByteArray): List<PrgSectionContainer> {
        val buf = ByteBuffer.wrap(data)

        val sections: MutableList<PrgSectionContainer> = mutableListOf()

        while (buf.position() < buf.remaining()) {
            sections.add(readSection(buf))
        }

        return sections
    }

    private fun readSection(buf: ByteBuffer): PrgSectionContainer {
        val magicValue = buf.getUInt()
        val length = buf.getUInt()
        val magic = PrgSectionMagic.fromMagic(magicValue)

        val content = when (magic) {
            else -> {
                val contentBytes = ByteArray(length.toInt())
                PrgSectionUnknown(contentBytes)
            }
        }

        return PrgSectionContainer(magic, length, content)
    }
}
