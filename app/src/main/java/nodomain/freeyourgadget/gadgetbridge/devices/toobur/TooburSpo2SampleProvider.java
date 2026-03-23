/*  Copyright (C) 2025 idowatch / TOOBUR — SpO₂ samples in {@link nodomain.freeyourgadget.gadgetbridge.entities.ID115ActivitySample#spo2} */
package nodomain.freeyourgadget.gadgetbridge.devices.toobur;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.ID115ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.ID115ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;

/**
 * Reads SpO₂ time series written by {@link nodomain.freeyourgadget.gadgetbridge.service.devices.toobur.TooburV3FetchHealthOperation}.
 */
public class TooburSpo2SampleProvider implements TimeSampleProvider<Spo2Sample> {

    private final GBDevice device;
    private final DaoSession session;

    public TooburSpo2SampleProvider(@NonNull GBDevice device, @NonNull DaoSession session) {
        this.device = device;
        this.session = session;
    }

    private long getDeviceId() {
        return DBHelper.getDevice(device, session).getId();
    }

    @NonNull
    @Override
    public List<Spo2Sample> getAllSamples(long timestampFrom, long timestampTo) {
        int fromSec = (int) (timestampFrom / 1000L);
        int toSec = (int) (timestampTo / 1000L);
        QueryBuilder<ID115ActivitySample> qb = session.getID115ActivitySampleDao().queryBuilder();
        List<ID115ActivitySample> rows = qb.where(ID115ActivitySampleDao.Properties.DeviceId.eq(getDeviceId()))
                .where(ID115ActivitySampleDao.Properties.Spo2.isNotNull())
                .where(ID115ActivitySampleDao.Properties.Timestamp.ge(fromSec))
                .where(ID115ActivitySampleDao.Properties.Timestamp.le(toSec))
                .orderAsc(ID115ActivitySampleDao.Properties.Timestamp)
                .list();
        List<Spo2Sample> out = new ArrayList<>(rows.size());
        for (ID115ActivitySample r : rows) {
            out.add(new Spo2Row(r));
        }
        return out;
    }

    @Override
    public void addSample(Spo2Sample timeSample) {
        throw new UnsupportedOperationException("TOOBUR SpO2 samples are synced from the band");
    }

    @Override
    public void addSamples(List<Spo2Sample> timeSamples) {
        throw new UnsupportedOperationException("TOOBUR SpO2 samples are synced from the band");
    }

    @NonNull
    @Override
    public Spo2Sample createSample() {
        return new Spo2Row(new ID115ActivitySample());
    }

    @Nullable
    @Override
    public Spo2Sample getLatestSample() {
        List<ID115ActivitySample> rows = session.getID115ActivitySampleDao().queryBuilder()
                .where(ID115ActivitySampleDao.Properties.DeviceId.eq(getDeviceId()))
                .where(ID115ActivitySampleDao.Properties.Spo2.isNotNull())
                .orderDesc(ID115ActivitySampleDao.Properties.Timestamp)
                .limit(1)
                .list();
        return rows.isEmpty() ? null : new Spo2Row(rows.get(0));
    }

    @Nullable
    @Override
    public Spo2Sample getLatestSample(long until) {
        int untilSec = (int) (until / 1000L);
        List<ID115ActivitySample> rows = session.getID115ActivitySampleDao().queryBuilder()
                .where(ID115ActivitySampleDao.Properties.DeviceId.eq(getDeviceId()))
                .where(ID115ActivitySampleDao.Properties.Spo2.isNotNull())
                .where(ID115ActivitySampleDao.Properties.Timestamp.le(untilSec))
                .orderDesc(ID115ActivitySampleDao.Properties.Timestamp)
                .limit(1)
                .list();
        return rows.isEmpty() ? null : new Spo2Row(rows.get(0));
    }

    @Nullable
    @Override
    public Spo2Sample getFirstSample() {
        List<ID115ActivitySample> rows = session.getID115ActivitySampleDao().queryBuilder()
                .where(ID115ActivitySampleDao.Properties.DeviceId.eq(getDeviceId()))
                .where(ID115ActivitySampleDao.Properties.Spo2.isNotNull())
                .orderAsc(ID115ActivitySampleDao.Properties.Timestamp)
                .limit(1)
                .list();
        return rows.isEmpty() ? null : new Spo2Row(rows.get(0));
    }

    private static final class Spo2Row implements Spo2Sample {
        private final ID115ActivitySample row;

        Spo2Row(ID115ActivitySample row) {
            this.row = row;
        }

        @Override
        public Type getType() {
            return Type.AUTOMATIC;
        }

        @Override
        public int getSpo2() {
            Integer v = row.getSpo2();
            return v != null ? v : 0;
        }

        @Override
        public long getTimestamp() {
            return row.getTimestamp() * 1000L;
        }
    }
}
