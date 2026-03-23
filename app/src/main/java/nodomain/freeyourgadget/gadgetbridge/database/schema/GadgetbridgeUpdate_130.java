package nodomain.freeyourgadget.gadgetbridge.database.schema;

import android.database.sqlite.SQLiteDatabase;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;
import nodomain.freeyourgadget.gadgetbridge.entities.ID115ActivitySampleDao;

/**
 * Adds optional {@code spo2} and {@code stress} columns to {@link nodomain.freeyourgadget.gadgetbridge.entities.ID115ActivitySample}
 * for TOOBUR v3 health sync charts.
 */
public class GadgetbridgeUpdate_130 implements DBUpdateScript {
    @Override
    public void upgradeSchema(final SQLiteDatabase db) {
        if (!DBHelper.existsColumn(ID115ActivitySampleDao.TABLENAME, ID115ActivitySampleDao.Properties.Spo2.columnName, db)) {
            db.execSQL("ALTER TABLE " + ID115ActivitySampleDao.TABLENAME + " ADD COLUMN \""
                    + ID115ActivitySampleDao.Properties.Spo2.columnName + "\" INTEGER;");
        }
        if (!DBHelper.existsColumn(ID115ActivitySampleDao.TABLENAME, ID115ActivitySampleDao.Properties.Stress.columnName, db)) {
            db.execSQL("ALTER TABLE " + ID115ActivitySampleDao.TABLENAME + " ADD COLUMN \""
                    + ID115ActivitySampleDao.Properties.Stress.columnName + "\" INTEGER;");
        }
    }

    @Override
    public void downgradeSchema(SQLiteDatabase database) {
    }
}
