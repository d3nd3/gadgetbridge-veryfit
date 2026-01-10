package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.prg

import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class PrgParserTest {
    @Test
    fun localTest() {
        val data = Files.readAllBytes(Paths.get("/storage/app.prg"))
        val prg = PrgParser.parse(data)
    }
}
