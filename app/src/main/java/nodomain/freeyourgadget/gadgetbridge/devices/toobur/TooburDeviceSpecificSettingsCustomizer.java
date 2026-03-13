/*  Copyright (C) 2025 idowatch / TOOBUR device support

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
package nodomain.freeyourgadget.gadgetbridge.devices.toobur;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.toobur.TooburSupport;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/**
 * Registers preference handlers for TOOBUR device-specific settings so that
 * when the user toggles music, call alert, DND, raise-to-wake, or SOS,
 * onSendConfiguration is called and the device is updated.
 */
public class TooburDeviceSpecificSettingsCustomizer implements DeviceSpecificSettingsCustomizer {

    @Override
    public void customizeSettings(DeviceSpecificSettingsHandler handler, Prefs prefs, String rootKey) {
        handler.addPreferenceHandlerFor(TooburSupport.PREF_TOOBUR_MUSIC_ENABLED);
        handler.addPreferenceHandlerFor(TooburSupport.PREF_TOOBUR_CALL_ALERT_ENABLED);
        handler.addPreferenceHandlerFor(TooburSupport.PREF_TOOBUR_DND_ENABLED);
        handler.addPreferenceHandlerFor(TooburSupport.PREF_TOOBUR_RAISE_TO_WAKE);
        handler.addPreferenceHandlerFor(TooburSupport.PREF_TOOBUR_SOS_ENABLED);
        handler.addPreferenceHandlerFor(TooburSupport.PREF_TOOBUR_HR_MODE);
        handler.addPreferenceHandlerFor(TooburSupport.PREF_TOOBUR_REALTIME_HR_ENABLED);
        handler.addPreferenceHandlerFor(TooburSupport.PREF_TOOBUR_WEATHER_ENABLED);
    }

    @Override
    public void onPreferenceChange(Preference preference, DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public void onDeviceChanged(DeviceSpecificSettingsHandler handler) {
    }

    @NonNull
    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    public static final Creator<TooburDeviceSpecificSettingsCustomizer> CREATOR = new Creator<TooburDeviceSpecificSettingsCustomizer>() {
        @Override
        public TooburDeviceSpecificSettingsCustomizer createFromParcel(Parcel in) {
            return new TooburDeviceSpecificSettingsCustomizer();
        }

        @Override
        public TooburDeviceSpecificSettingsCustomizer[] newArray(int size) {
            return new TooburDeviceSpecificSettingsCustomizer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
}
