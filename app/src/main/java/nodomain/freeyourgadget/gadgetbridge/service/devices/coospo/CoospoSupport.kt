package nodomain.freeyourgadget.gadgetbridge.service.devices.coospo

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder
import nodomain.freeyourgadget.gadgetbridge.service.devices.generic_hr.GenericHeartRateSupport
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CoospoSupport : GenericHeartRateSupport() {
    init {
        addSupportedService(GattService.UUID_FUTEK_ADVANCED_SENSOR_TECHNOLOGY)
    }

    override fun initializeDevice(builder: TransactionBuilder): TransactionBuilder {
        super.initializeDevice(builder)
        builder.write(GattCharacteristic.UUID_CHARACTERISTIC_LUMINOSTICS, *byteArrayOf(
            0xa2.toByte(),
            0x04,
            0x81.toByte(),
            0x27
        ))
        return builder
    }

    override fun onSendConfiguration(config: String) {
        when (config) {
            DeviceSettingsPreferenceConst.PREF_MAX_HEART_RATE -> {
                val maxHr = devicePrefs.getInt(config, 180)
                LOG.debug("Setting max hr to {}", maxHr)
                val builder = createTransactionBuilder("set max hr")
                builder.write(GattCharacteristic.UUID_CHARACTERISTIC_LUMINOSTICS, *encodeMaxHr(maxHr))
                builder.queue()
                return
            }
        }

        super.onSendConfiguration(config)
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(CoospoSupport::class.java)

        fun encodeMaxHr(value: Int): ByteArray {
            val cmd = byteArrayOf(
                0xb2.toByte(),
                0x05.toByte(),
                0x0c.toByte(),
                value.toByte(),
                0x00.toByte()
            )
            // crc
            cmd[4] = (cmd[0] + cmd[1] + cmd[2] + cmd[3]).toByte()
            return cmd
        }
    }
}
