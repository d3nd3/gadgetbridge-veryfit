/*  Copyright (C) 2018-2025 Daniele Gobbetti, José Rebelo, Martin

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
package nodomain.freeyourgadget.gadgetbridge.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.service.devices.toobur.TooburSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;


public class GBAutoFetchReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(GBAutoFetchReceiver.class);

    /** Last auto-fetch for non-TOOBUR devices (global interval from preferences). */
    private Date lastNonTooburSync = new Date();
    /** Debounce rapid duplicate broadcasts (#4165) after any auto-fetch dispatch. */
    private Date lastDispatchAt = new Date();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (!GBApplication.getPrefs().getBoolean(GBPrefs.PREF_AUTO_FETCH_ENABLED, false)) {
            return;
        }
        synchronized (this) {
            final Date now = new Date();
            final long sinceDispatch = now.getTime() - lastDispatchAt.getTime();
            if (sinceDispatch < 2500L) {
                LOG.warn("Throttling auto fetch by {}, last one was {}ms ago", intent.getAction(), sinceDispatch);
                return;
            }

            final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
            boolean dispatched = false;

            for (GBDevice device : devices) {
                if (!device.isInitialized()) {
                    continue;
                }
                if (device.getType() == DeviceType.TOOBUR) {
                    if (shouldRunTooburAutoFetch(device, now)) {
                        LOG.info("Trigger auto fetch (TOOBUR) for {} by {}", device.getAddress(), intent.getAction());
                        GBApplication.deviceService(device).onFetchRecordedData(RecordedDataTypes.TYPE_SYNC, true);
                        markTooburAutoFetch(device, now.getTime());
                        dispatched = true;
                    }
                }
            }

            final Date nextNonToobur = DateUtils.addMinutes(lastNonTooburSync,
                    GBApplication.getPrefs().getInt(GBPrefs.PREF_AUTO_FETCH_INTERVAL_LIMIT, 0));
            if (nextNonToobur.before(now)) {
                for (GBDevice device : devices) {
                    if (!device.isInitialized()) {
                        continue;
                    }
                    if (device.getType() == DeviceType.TOOBUR) {
                        continue;
                    }
                    LOG.info("Trigger auto fetch for {} by {}", device.getAddress(), intent.getAction());
                    GBApplication.deviceService(device).onFetchRecordedData(RecordedDataTypes.TYPE_SYNC, false);
                    dispatched = true;
                }
                lastNonTooburSync = now;
            }

            if (dispatched) {
                lastDispatchAt = now;
            }
        }
    }

    private static boolean shouldRunTooburAutoFetch(GBDevice device, Date now) {
        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());
        if (!prefs.getBoolean(TooburSupport.PREF_TOOBUR_AUTO_FETCH_ENABLED, true)) {
            return false;
        }
        int intervalMin = parsePositiveInt(prefs.getString(TooburSupport.PREF_TOOBUR_AUTO_FETCH_INTERVAL_MINUTES, "0"), 0);
        if (intervalMin <= 0) {
            intervalMin = GBApplication.getPrefs().getInt(GBPrefs.PREF_AUTO_FETCH_INTERVAL_LIMIT, 0);
        }
        long lastMs = prefs.getLong(TooburSupport.PREF_TOOBUR_AUTO_FETCH_LAST_MS, 0L);
        if (lastMs == 0L) {
            return true;
        }
        Date next = DateUtils.addMinutes(new Date(lastMs), intervalMin);
        return next.before(now);
    }

    private static void markTooburAutoFetch(GBDevice device, long wallMs) {
        GBApplication.getDeviceSpecificSharedPrefs(device.getAddress())
                .edit()
                .putLong(TooburSupport.PREF_TOOBUR_AUTO_FETCH_LAST_MS, wallMs)
                .apply();
    }

    private static int parsePositiveInt(String raw, int def) {
        if (raw == null || raw.trim().isEmpty()) {
            return def;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
