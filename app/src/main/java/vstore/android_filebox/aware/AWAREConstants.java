package vstore.android_filebox.aware;
import android.net.Uri;

/**
 * Contains constants of AWARE specific properties.
 */
public class AWAREConstants {
    public static final int DEFAULT_REFRESH_INTERVAL_SECONDS = 120;

    public static final String AWARE = "com.aware.phone";
    public static final String ACTIVITY_PLUGIN = "com.aware.plugin.google.activity_recognition";
    public static final String LOCATION_PLUGIN = "com.aware.plugin.google.fused_location";
    public static final String NOISE_PLUGIN = "com.aware.plugin.ambient_noise";

    //Prevent instantiation
    private AWAREConstants() {}

    public static final class Settings {
        public static final Uri URI = Uri.parse("content://com.aware.phone.provider.aware/aware_settings");
    }

    public static final class Plugins {
        public static final Uri URI = Uri.parse("content://com.aware.phone.provider.aware/aware_plugins");
        public static final String KEY_PLUGIN_STATUS = "plugin_status";
        public static final String KEY_PLUGIN_PACKAGE_NAME = "package_name";
        public static final int STATUS_PLUGIN_OFF = 0;
        public static final int STATUS_PLUGIN_ON = 1;
    }

    public static final class Location {
        public static final Uri URI = Uri.parse("content://com.aware.phone.provider.locations/locations");

        public static final String ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String LATITUDE = "double_latitude";
        public static final String LONGITUDE = "double_longitude";
        public static final String ACCURACY = "accuracy";
    }

    public static final class Activity {
        public static final Uri URI = Uri.parse("content://com.aware.plugin.google.activity_recognition.provider.gar/plugin_google_activity_recognition");

        public static final String ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String NAME = "activity_name";
        public static final String TYPE = "activity_type";
        public static final String CONFIDENCE = "confidence";
        public static final String OTHER_ACTIVITIES = "activities";
    }

    public static final class Noise {
        public static final Uri URI = Uri.parse("content://com.aware.plugin.ambient_noise.provider.ambient_noise/plugin_ambient_noise");

        public static final String ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String FREQUENCY = "double_frequency";
        public static final String DECIBELS = "double_decibels";
        public static final String RMS = "double_rms";
        public static final String SILENCE_THRESH = "double_silence_threshold";
    }

}
