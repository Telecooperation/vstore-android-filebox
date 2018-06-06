package vstore.android_filebox.aware;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import static vstore.android_filebox.aware.AWAREConstants.ACTIVITY_PLUGIN;
import static vstore.android_filebox.aware.AWAREConstants.AWARE;
import static vstore.android_filebox.aware.AWAREConstants.LOCATION_PLUGIN;
import static vstore.android_filebox.aware.AWAREConstants.NOISE_PLUGIN;

/**
 * Class for controlling AWARE middleware
 */
public class AwareController {

    static final int DEFAULT_REFRESH_INTERVAL_SECONDS = 300;

    private AwareController() {}

    /**
     * Checks if the necessary Context middleware and plugins are installed.
     * Plugins are:
     * AWARE Google Activity Recognition
     * AWARE Google Fused Location
     * AWARE Ambient Noise
     *
     * @param c The Android context
     * @return True, if all necessary plugins are installed. False, if not.
     */
    public static boolean isAwareInstalled(Context c) {
        try {
            c.getPackageManager().getApplicationInfo(AWARE, 0);
            return true;
        }
        catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isActivityPluginInstalled(Context c) {
        try {
            c.getPackageManager().getApplicationInfo(ACTIVITY_PLUGIN, 0);
            return true;
        } catch(PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isLocationPluginInstalled(Context c) {
        try {
            c.getPackageManager().getApplicationInfo(LOCATION_PLUGIN, 0);
            return true;
        } catch(PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isNoisePluginInstalled(Context c) {
        try {
            c.getPackageManager().getApplicationInfo(NOISE_PLUGIN, 0);
            return true;
        } catch(PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * @param c The Android context
     * @return Returns true, if all necessary AWARE plugins are installed.
     */
    public static boolean allPluginsInstalled(Context c) {
        return isActivityPluginInstalled(c) && isLocationPluginInstalled(c) && isNoisePluginInstalled(c);
    }

    /**
     * Sets the given settings key from AWARE to the given value (by accessing AWAREs content providers).
     * @param c The Android context
     * @param key The desired AWARE settings key
     * @param value The value for this setting.
     */
    private static void setAwareSetting(Context c, String key, String value) {
        ContentValues setting = new ContentValues();
        setting.put("key", key);
        setting.put("value", value);
        setting.put("package_name", "com.aware");

        try {
            //Check if similar setting is already set
            Cursor qry = c.getContentResolver().query(
                    AWAREConstants.Settings.URI,
                    (String[]) null,
                    "key LIKE \'" + key + "\' AND " + "package_name" + " LIKE " + "\'com.aware\'",
                    (String[]) null,
                    (String) null);

            if (qry != null && qry.moveToFirst()) {
                try {
                    if (!qry.getString(qry.getColumnIndex("value")).equals(value)) {
                        c.getContentResolver().update(
                                AWAREConstants.Settings.URI,
                                setting,
                                "_id=" + qry.getInt(qry.getColumnIndex("_id")),
                                (String[]) null);
                    }
                } catch (SQLiteException var10) {
                    Log.d("vStore", var10.getMessage());
                } catch (SQLException var11) {
                    Log.d("vStore", var11.getMessage());
                }
            } else {
                try {
                    c.getContentResolver().insert(AWAREConstants.Settings.URI, setting);
                    Log.d("vStore", "Added: " + key + "=" + value);
                } catch (SQLiteException var8) {
                    Log.d("vStore", var8.getMessage());
                } catch (SQLException var9) {
                    Log.d("vStore", var9.getMessage());
                }
                catch(IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
            if (qry != null) {
                qry.close();
            }
        } catch(SecurityException e) {
            //Ignore the fact that content provider could not be accessed.
            //Happens, if VStorage has been installed before AWARE.
        }
    }

    private static boolean enablePlugin(Context c, String pkgName) {
        ContentValues rowData = new ContentValues();
        rowData.put(AWAREConstants.Plugins.KEY_PLUGIN_STATUS, AWAREConstants.Plugins.STATUS_PLUGIN_ON);
        try {
            c.getContentResolver().update(
                    AWAREConstants.Plugins.URI,
                    rowData,
                    AWAREConstants.Plugins.KEY_PLUGIN_PACKAGE_NAME + " LIKE '" + pkgName + "'",
                    null);
            return true;
        } catch(SecurityException e) {
            //Ignore the fact that content provider could not be accessed.
            //Happens, if VStorage has been installed before AWARE.
            return false;
        }
    }

    public static void startPlugin(Context c, String pkgName) {
        ComponentName component = new ComponentName(pkgName, pkgName + ".Plugin");
        Intent pluginIntent = new Intent();
        pluginIntent.setComponent(component);
        c.stopService(pluginIntent);
        c.startService(pluginIntent);
    }

    /**
     * Starts the activity plugin with the given update frequency
     * @param c The Android context
     * @param secondsInterval Update interval for the activity detection
     */
    public static void startActivityRecognitionPlugin(Context c, int secondsInterval) {
        if(secondsInterval <= 0) {
            //Default: every 5 minutes
            secondsInterval = 300;
        }
        //Set update interval in seconds
        setAwareSetting(c, "frequency_plugin_google_activity_recognition", ""+secondsInterval);
        //Set Enable/disable flag for the plugin
        setAwareSetting(c, "status_plugin_google_activity_recognition", "true");
        //Tell AWARE to start the plugin
        if(enablePlugin(c, ACTIVITY_PLUGIN)) {
            startPlugin(c, ACTIVITY_PLUGIN);
        }
    }

    /**
     * Starts the location plugin with the given update frequency
     * @param c The Android context
     * @param secondsInterval Update interval for the location acquisition
     */
    public static void startLocationPlugin(Context c, int secondsInterval) {
        if(secondsInterval <= 0) {
            //Default: every 5 minutes
            secondsInterval = 300;
        }
        //Set update interval in seconds
        setAwareSetting(c, "frequency_google_fused_location", ""+secondsInterval);
        //Set how fast the framework should receive updates about the location
        setAwareSetting(c, "max_frequency_google_fused_location", ""+120);
        //Set accuracy to "balanced" (=GPS, Network and WiFi)
        setAwareSetting(c, "accuracy_google_fused_location", ""+102);
        //Set fix timeout
        setAwareSetting(c, "fallback_location_timeout", ""+10);
        //Set location sensitivity in meters (after how many meters of movement a new location
        //should be requested)
        setAwareSetting(c, "location_sensitivity", ""+5);
        //Set Enable/disable flag for the plugin
        setAwareSetting(c, "status_google_fused_location", "true");
        //Tell AWARE to start the plugin
        if(enablePlugin(c, LOCATION_PLUGIN)) {
            startPlugin(c, LOCATION_PLUGIN);
        }
    }

    /**
     * Starts the ambient noise plugin with the given update frequency
     * @param c The Android context
     * @param secondsInterval Update interval for the noise acquisition in seconds.
     */
    public static void startNoisePlugin(Context c, int secondsInterval) {
        if (secondsInterval < 60) {
            //Default: every 5 minutes
            secondsInterval = 300;
        }
        //Set update interval in minutes
        setAwareSetting(c, "frequency_plugin_ambient_noise", "" + (secondsInterval / 60));
        //Set the duration of a recording in seconds
        setAwareSetting(c, "plugin_ambient_noise_sample_size", "" + 5);
        //Set Enable/disable flag for the plugin
        setAwareSetting(c, "status_plugin_ambient_noise", "true");
        //Tell AWARE to start the plugin
        if (enablePlugin(c, NOISE_PLUGIN)) {
            startPlugin(c, NOISE_PLUGIN);
        }
    }

    public static void configureAwarePlugins(Context c) {
        AwareController.startActivityRecognitionPlugin(c, DEFAULT_REFRESH_INTERVAL_SECONDS);
        AwareController.startLocationPlugin(c, DEFAULT_REFRESH_INTERVAL_SECONDS);
        AwareController.startNoisePlugin(c, DEFAULT_REFRESH_INTERVAL_SECONDS);
    }
}
