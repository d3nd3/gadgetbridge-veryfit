/*  Copyright (C) 2025 idowatch / TOOBUR — stress samples in {@link nodomain.freeyourgadget.gadgetbridge.entities.ID115ActivitySample#stress} */
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
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;

/**
 * Reads stress (pressure) time series written by {@link nodomain.freeyourgadget.gadgetbridge.service.devices.toobur.TooburV3FetchHealthOperation}.
 */
public class TooburStressSampleProvider implements TimeSampleProvider<StressSample> {

    private final GBDevice device;
    private final DaoSession session;

    public TooburStressSampleProvider(@NonNull GBDevice device, @NonNull DaoSession session) {
        this.device = device;
        this.session = session;
    }

    private long getDeviceId() {
        return DBHelper.getDevice(device, session).getId();
    }

    @NonNull
    @Override
    public List<StressSample> getAllSamples(long timestampFrom, long timestampTo) {
        int fromSec = (int) (timestampFrom / 1000L);
        int toSec = (int) (timestampTo / 1000L);
        QueryBuilder<ID115ActivitySample> qb = session.getID115ActivitySampleDao().queryBuilder();
        List<ID115ActivitySample> rows = qb.where(ID115ActivitySampleDao.Properties.DeviceId.eq(getDeviceId()))
                .where(ID115ActivitySampleDao.Properties.Stress.isNotNull())
                .where(ID115ActivitySampleDao.Properties.Timestamp.ge(fromSec))
                .where(ID115ActivitySampleDao.Properties.Timestamp.le(toSec))
                .orderAsc(ID115ActivitySampleDao.Properties.Timestamp)
                .list();
        List<StressSample> out = new ArrayList<>(rows.size());
        for (ID115ActivitySample r : rows) {
            out.add(new StressRow(r));
        }
        return out;
    }

    @Override
    public void addSample(StressSample timeSample) {
        throw new UnsupportedOperationException("TOOBUR stress samples are synced from the band");
    }

    @Override
    public void addSamples(List<StressSample> timeSamples) {
        throw new UnsupportedOperationException("TOOBUR stress samples are synced from the band");
    }

    @NonNull
    @Override
    public StressSample createSample() {
        return new StressRow(new ID115ActivitySample());
    }

    @Nullable
    @Override
    public StressSample getLatestSample() {
        List<ID115ActivitySample> rows = session.getID115ActivitySampleDao().queryBuilder()
                .where(ID115ActivitySampleDao.Properties.DeviceId.eq(getDeviceId()))
                .where(ID115ActivitySampleDao.Properties.Stress.isNotNull())
                .orderDesc(ID115ActivitySampleDao.Properties.Timestamp)
                .limit(1)
                .list();
        return rows.isEmpty() ? null : new StressRow(rows.get(0));
    }

    @Nullable
    @Override
    public StressSample getLatestSample(long until) {
        int untilSec = (int) (until / 1000L);
        List<ID115ActivitySample> rows = session.getID115ActivitySampleDao().queryBuilder()
                .where(ID115ActivitySampleDao.Properties.DeviceId.eq(getDeviceId()))
                .where(ID115ActivitySampleDao.Properties.Stress.isNotNull())
                .where(ID115ActivitySampleDao.Properties.Timestamp.le(untilSec))
                .orderDesc(ID115ActivitySampleDao.Properties.Timestamp)
                .limit(1)
                .list();
        return rows.isEmpty() ? null : new StressRow(rows.get(0));
    }

    @Nullable
    @Override
    public StressSample getFirstSample() {
        List<ID115ActivitySample> rows = session.getID115ActivitySampleDao().queryBuilder()
                .where(ID115ActivitySampleDao.Properties.DeviceId.eq(getDeviceId()))
                .where(ID115ActivitySampleDao.Properties.Stress.isNotNull())
                .orderAsc(ID115ActivitySampleDao.Properties.Timestamp)
                .limit(1)
                .list();
        return rows.isEmpty() ? null : new StressRow(rows.get(0));
    }

    private static final class StressRow implements StressSample {
        private final ID115ActivitySample row;

        StressRow(ID115ActivitySample row) {
            this.row = row;
        }

        @Override
        public Type getType() {
            return Type.AUTOMATIC;
        }

        @Override
        public int getStress() {
            Integer v = row.getStress();
            return v != null ? v : 0;
        }

        @Override
        public long getTimestamp() {
            return row.getTimestamp() * 1000L;
        }
    }
}
