package nodomain.freeyourgadget.gadgetbridge.database.schema;

import android.database.sqlite.SQLiteDatabase;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;
import nodomain.freeyourgadget.gadgetbridge.entities.ID115ActivitySampleDao;

/**
 * Adds {@code HEART_RATE} to {@link nodomain.freeyourgadget.gadgetbridge.entities.ID115ActivitySample}
 * for TOOBUR / ID115 heart-rate charts (live GET 0x02 0xA0 and sync).
 */
public class GadgetbridgeUpdate_129 implements DBUpdateScript {
    @Override
    public void upgradeSchema(final SQLiteDatabase db) {
        if (!DBHelper.existsColumn(ID115ActivitySampleDao.TABLENAME, ID115ActivitySampleDao.Properties.HeartRate.columnName, db)) {
            final String statement = "ALTER TABLE " + ID115ActivitySampleDao.TABLENAME + " ADD COLUMN \""
                    + ID115ActivitySampleDao.Properties.HeartRate.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
    }

    @Override
    public void downgradeSchema(SQLiteDatabase database) {
    }
}
