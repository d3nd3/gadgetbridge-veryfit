package nodomain.freeyourgadget.gadgetbridge.prefs.migrators;

import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.prefs.AbstractPreferenceMigrator;

public class PreferenceMigrator57 extends AbstractPreferenceMigrator {
    private static final Logger LOG = LoggerFactory.getLogger(PreferenceMigrator57.class);

    @Override
    public void migrate(final int oldVersion, final SharedPreferences sharedPrefs, final SharedPreferences.Editor editor) {
        try {
            editor.putString("chart_sleep_range_hour", sharedPrefs.getBoolean("chart_sleep_range_24h", false) ? "12" : "0");
        } catch (final Exception e) {
            LOG.error("Failed to migrate prefs to version 57", e);
        }
    }
}
