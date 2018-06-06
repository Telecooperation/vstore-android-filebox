package vstore.android_filebox.context_monitor;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import vstore.android_filebox.R;
import vstore.android_filebox.aware.AWAREConstants;
import vstore.android_filebox.services.PlacesBackgroundService;
import vstore.android_filebox.utils.Connectivity;
import vstore.framework.VStore;
import vstore.framework.context.ContextDescription;
import vstore.framework.context.events.NewContextEvent;
import vstore.framework.context.types.activity.VActivity;
import vstore.framework.context.types.location.VLatLng;
import vstore.framework.context.types.location.VLocation;
import vstore.framework.context.types.network.VNetwork;
import vstore.framework.context.types.network.cellular.CellularNetwork;
import vstore.framework.context.types.network.wifi.WiFi;
import vstore.framework.context.types.noise.VNoise;

/**
 * This class can be used to manually pull the newest context from the AWARE framework,
 * if necessary. It is useful if no broadcasts are received (e.g. after initial installation.
 * The latest data from the AWARE content providers is pulled and then passed to the framework.
 */
public class ManualContextUpdater extends Thread {

    //Android context
    private Context c;

    //Framework context data
    private ContextDescription currentCtx;

    public ManualContextUpdater(Context c) {
        this.c = c;
    }

    @Override
    public void run() {
        currentCtx = VStore.getInstance().getCurrentContext();

        fetchLocationFromProvider();
        fetchActivityFromProvider();
        getNetworkData();
        fetchNoiseFromProvider();

        VStore.getInstance().provideContext(currentCtx);
        VStore.getInstance().persistContext(true);

        fetchPlaces();

        NewContextEvent evt = new NewContextEvent(true);
        EventBus.getDefault().post(evt);
    }

    /**
     * Manually fetches the most recent location context data from the AWARE Fused Location plugin
     */
    private void fetchLocationFromProvider() {
        String[] tableColumns = new String[] {
                AWAREConstants.Location.LATITUDE,
                AWAREConstants.Location.LONGITUDE,
                AWAREConstants.Location.ACCURACY,
                AWAREConstants.Location.TIMESTAMP
        };
        //Order by timestamp descending and limit results to one row
        String orderBy = AWAREConstants.Location.TIMESTAMP + " DESC LIMIT 1";
        try {
            ContentResolver resolver = c.getApplicationContext().getContentResolver();
            Cursor cur = resolver.query(AWAREConstants.Location.URI, tableColumns, null, null, orderBy);
            if (cur != null && cur.moveToFirst()) {
                double latitude = cur.getDouble(cur.getColumnIndexOrThrow(AWAREConstants.Location.LATITUDE));
                double longitude = cur.getDouble(cur.getColumnIndexOrThrow(AWAREConstants.Location.LONGITUDE));
                float accuracy = cur.getFloat(cur.getColumnIndexOrThrow(AWAREConstants.Location.ACCURACY));
                long timestamp = cur.getLong(cur.getColumnIndexOrThrow(AWAREConstants.Location.TIMESTAMP));
                cur.close();
                VLocation l = new VLocation(new VLatLng(latitude, longitude), accuracy, timestamp, "");
                currentCtx.setLocationContext(l);
            }
        } catch(NullPointerException ex) {
            Toast.makeText(c.getApplicationContext(), R.string.make_sure_aware, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Manually fetches the most recent activity context data from the AWARE Activity Recognition plugin.
     */
    private void fetchActivityFromProvider() {
        String[] tableColumns = new String[] {
                AWAREConstants.Activity.TYPE,
                AWAREConstants.Activity.CONFIDENCE,
                AWAREConstants.Activity.TIMESTAMP
        };
        //Order by timestamp descending and limit results to one row
        String orderBy = AWAREConstants.Activity.TIMESTAMP + " DESC LIMIT 1";
        try {
            Cursor cur = c.getApplicationContext().getContentResolver()
                    .query(AWAREConstants.Activity.URI, tableColumns, null, null, orderBy);
            if (cur != null && cur.getCount() > 0) {
                cur.moveToFirst();
                int activity = cur.getInt(cur.getColumnIndexOrThrow(AWAREConstants.Activity.TYPE));
                int confidence = cur.getInt(cur.getColumnIndexOrThrow(AWAREConstants.Activity.CONFIDENCE));
                long timestamp = cur.getLong(cur.getColumnIndexOrThrow(AWAREConstants.Activity.TIMESTAMP));
                cur.close();
                VActivity a = new VActivity(ActivityParser.parse(activity), confidence, timestamp);
                currentCtx.setActivityContext(a);
            }
        } catch(NullPointerException ex) {
            //Same problem already handled by try/catch in fetchLocationFromProvider
        }
    }

    /**
     * Gets the current network context data manually.
     */
    private void getNetworkData() {
        Context appCtx = c.getApplicationContext();

        boolean isWifi = Connectivity.isConnectedWifi(appCtx);
        String wifiSsid = Connectivity.getWifiName(appCtx);
        WiFi wifiCtx = new WiFi(isWifi, wifiSsid);

        boolean isMobile = Connectivity.isConnectedMobile(appCtx);
        boolean isMobileFast = Connectivity.isConnectedFast(appCtx);
        CellularNetwork.MobileType mobileType = Connectivity.getMobileNetworkClass(appCtx);
        CellularNetwork mobCtx = new CellularNetwork(isMobile, isMobileFast, mobileType);

        VNetwork net = new VNetwork(wifiCtx, mobCtx);
        currentCtx.setNetworkContext(net);
    }

    /**
     * Manually fetches the most recent noise context data from the AWARE Ambient Noise plugin.
     */
    private void fetchNoiseFromProvider() {
        String[] tableColumns = new String[] {
                AWAREConstants.Noise.DECIBELS,
                AWAREConstants.Noise.RMS,
                AWAREConstants.Noise.TIMESTAMP
        };
        //Order by timestamp descending and limit results to one row
        String orderBy = AWAREConstants.Noise.TIMESTAMP + " DESC LIMIT 1";
        try {
            Cursor cur = c.getApplicationContext().getContentResolver()
                    .query(AWAREConstants.Noise.URI, tableColumns, null, null, orderBy);
            if (cur != null && cur.getCount() > 0)
            {
                cur.moveToFirst();
                double dB = -1.0 * cur.getDouble(cur.getColumnIndexOrThrow(AWAREConstants.Noise.DECIBELS));
                double rms = cur.getDouble(cur.getColumnIndexOrThrow(AWAREConstants.Noise.RMS));
                int rmsThreshold = 0;
                int dbThreshold = 0;
                long timestamp = cur.getLong(cur.getColumnIndexOrThrow(AWAREConstants.Noise.TIMESTAMP));
                cur.close();
                //Cast db and rms to float, because we do not need double precision for this
                VNoise noise = new VNoise((float)dB, (float)rms, rmsThreshold, dbThreshold, timestamp);
                currentCtx.setNoiseContext(noise);
            }
        } catch(NullPointerException ex) {
            //Same problem already handled by try/catch in fetchLocationFromProvider
        }
    }


    /**
     * Runs the PlacesBackgroundService to fetch nearby places
     */
    private void fetchPlaces() {
        Intent placesIntent = new Intent(c.getApplicationContext(), PlacesBackgroundService.class);
        c.getApplicationContext().startService(placesIntent);
    }
}
