/*  Copyright (C) 2025 idowatch / TOOBUR device support */
package nodomain.freeyourgadget.gadgetbridge.devices.toobur;

import android.os.Parcel;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.toobur.TooburSupport;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/**
 * Registers preference handlers for TOOBUR device-specific settings and manual bind/unbind actions.
 */
public class TooburDeviceSpecificSettingsCustomizer implements DeviceSpecificSettingsCustomizer {

    @Override
    public void customizeSettings(DeviceSpecificSettingsHandler handler, Prefs prefs, String rootKey) {
        handler.addPreferenceHandlerFor(TooburSupport.PREF_TOOBUR_MUSIC_ENABLED);
        handler.addPreferenceHandlerFor(TooburSupport.PREF_TOOBUR_CALL_ALERT_ENABLED);
        handler.addPreferenceHandlerFor(TooburSupport.PREF_TOOBUR_DND_ENABLED);
        handler.addPreferenceHandlerFor(TooburSupport.PREF_TOOBUR_RAISE_TO_WAKE);
        handler.addPreferenceHandlerFor(TooburSupport.PREF_TOOBUR_HR_CONTINUOUS_ENABLED);
        handler.addPreferenceHandlerFor(TooburSupport.PREF_TOOBUR_HR_INTERVAL_SECONDS);
        handler.addPreferenceHandlerFor(TooburSupport.PREF_TOOBUR_SPO2_CONTINUOUS_ENABLED);
        handler.addPreferenceHandlerFor(TooburSupport.PREF_TOOBUR_PRESSURE_CONTINUOUS_ENABLED);
        handler.addPreferenceHandlerFor(TooburSupport.PREF_TOOBUR_AUTO_ACTIVITY_PRESET);
        handler.addPreferenceHandlerFor(TooburSupport.PREF_TOOBUR_WEATHER_ENABLED);

        Preference pBind = handler.findPreference(TooburSupport.PREF_TOOBUR_ACTION_SEND_BIND);
        if (pBind != null) {
            pBind.setOnPreferenceClickListener(pref -> {
                GBApplication.deviceService(handler.getDevice()).onSendConfiguration(TooburSupport.PREF_TOOBUR_ACTION_SEND_BIND);
                return true;
            });
        }
        Preference pUnbind = handler.findPreference(TooburSupport.PREF_TOOBUR_ACTION_SEND_UNBIND);
        if (pUnbind != null) {
            pUnbind.setOnPreferenceClickListener(pref -> {
                GBApplication.deviceService(handler.getDevice()).onSendConfiguration(TooburSupport.PREF_TOOBUR_ACTION_SEND_UNBIND);
                return true;
            });
        }

        Preference pInterval = handler.findPreference(TooburSupport.PREF_TOOBUR_AUTO_FETCH_INTERVAL_MINUTES);
        if (pInterval instanceof EditTextPreference) {
            ((EditTextPreference) pInterval).setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.selectAll();
            });
        }
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
