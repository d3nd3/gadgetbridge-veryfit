package nodomain.freeyourgadget.gadgetbridge.service.devices.coospo

import org.junit.Assert.*
import org.junit.Test

class CoospoSupportTest {
    @Test
    fun testEncodeMaxHr() {
        val examples: Map<Int, ByteArray> = mapOf(
            Pair(169, "b2050ca96c".hexToByteArray()),
            Pair(150, "b2050c9659".hexToByteArray()),
            Pair(175, "b2050caf72".hexToByteArray()),
            Pair(200, "b2050cc88b".hexToByteArray()),
            Pair(70, "b2050c4609".hexToByteArray()),
        )

        for ((value, expected) in examples) {
            assertEquals(
                expected.toHexString(),
                CoospoSupport.encodeMaxHr(value).toHexString()
            )
        }
    }
}
