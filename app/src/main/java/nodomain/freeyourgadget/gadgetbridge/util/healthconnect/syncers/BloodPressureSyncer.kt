/*  Copyright (C) 2026 José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.util.healthconnect.syncers

import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Pressure
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.BloodPressureSample
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset
import kotlin.reflect.KClass

internal object BloodPressureSyncer : AbstractTimeSampleSyncer<BloodPressureSample, BloodPressureRecord>() {
    override val logger: Logger = LoggerFactory.getLogger(BloodPressureSyncer::class.java)
    override val recordClass: KClass<BloodPressureRecord> = BloodPressureRecord::class

    override fun getSampleProvider(
        gbDevice: GBDevice,
        daoSession: DaoSession
    ): TimeSampleProvider<out BloodPressureSample> {
        @Suppress("UNCHECKED_CAST")
        return gbDevice.deviceCoordinator.getBloodPressureSampleProvider(gbDevice, daoSession) as TimeSampleProvider<out BloodPressureSample>
    }

    override fun convertSample(
        sample: BloodPressureSample,
        offset: ZoneOffset,
        metadata: Metadata,
        deviceName: String
    ): BloodPressureRecord {
        return BloodPressureRecord(
            time = Instant.ofEpochMilli(sample.timestamp),
            zoneOffset = offset,
            systolic = Pressure.millimetersOfMercury(sample.bpSystolic.toDouble()),
            diastolic = Pressure.millimetersOfMercury(sample.bpDiastolic.toDouble()),
            metadata = metadata
        )
    }
}
