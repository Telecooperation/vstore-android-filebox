package vstore.android_filebox.context_monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import vstore.android_filebox.Application;
import vstore.android_filebox.services.PlacesBackgroundService;
import vstore.android_filebox.utils.Connectivity;
import vstore.framework.VStore;
import vstore.framework.context.ContextDescription;
import vstore.framework.context.types.activity.VActivity;
import vstore.framework.context.types.location.VLatLng;
import vstore.framework.context.types.location.VLocation;
import vstore.framework.context.types.network.VNetwork;
import vstore.framework.context.types.network.cellular.CellularNetwork;
import vstore.framework.context.types.network.wifi.WiFi;
import vstore.framework.context.types.noise.VNoise;
import vstore.framework.exceptions.VStoreException;

/**
 * This class saves new context information from broadcasts received from the AWARE framework as
 * well as from own services.
 */
public class ContextAggregator extends BroadcastReceiver {

    public void onReceive(Context c, Intent intent) {
        if(intent == null) return;

        //Get context receive time
        Long gmtTime = System.currentTimeMillis();
        //Check what context is updated
        String action = intent.getAction();
        if (action == null || action.equals("")) { return; }

        if(VStore.getInstance() == null) {
            try {
                VStore.initialize(c.getApplicationContext().getExternalFilesDir(null),
                        Application.vstore_master_uri);
            } catch (VStoreException e) {
                e.printStackTrace();
                return;
            }
        }
        ContextDescription currentCtx = VStore.getInstance().getCurrentContext();

        switch (action)
        {
            // Handle broadcasts coming from the AWARE Activity plugin
            case "ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION":
                if (intent.getExtras() == null) { break; }
                VActivity activity
                        = new VActivity(ActivityParser.parse(intent.getExtras().getInt("activity")),
                        intent.getExtras().getInt("confidence"),
                        gmtTime);
                    currentCtx.setActivityContext(activity);
                break;

            // Handle broadcasts coming from the AWARE Fused Location plugin when a new location is
            // available. New location will be saved in the Key/Value file for context data.
            case "ACTION_AWARE_LOCATIONS":
                if (intent.getExtras() == null) { break; }

                //Got new location, so let's fetch some new places that are close
                Intent placesIntent = new Intent(c, PlacesBackgroundService.class);
                c.startService(placesIntent);

                Object o = intent.getExtras().get("data");
                VLocation locCtx = null;
                if (o != null)
                {
                    //Get the location object extra from the intent
                    Location newLoc = (Location) o;
                    locCtx = new VLocation(
                            new VLatLng(newLoc.getLatitude(), newLoc.getLongitude()),
                            newLoc.getAccuracy(),
                            gmtTime,
                            "");
                    locCtx.setDescription(getLocationGeoText(c, locCtx));

                }
                currentCtx.setLocationContext(locCtx);
                break;

            // Handle broadcasts coming from the AWARE Noise plugin. New noise data will
            // be saved in the Key/Value file for context data.
            case "ACTION_AWARE_PLUGIN_AMBIENT_NOISE":
                if (intent.getExtras() == null) { break; }
                double soundDB = -1.0*intent.getExtras().getDouble("sound_db", Double.MIN_VALUE);
                double soundRMS = intent.getExtras().getDouble("sound_rms", Double.MIN_VALUE);
                VNoise noiseCtx = null;
                if (soundDB != Double.MIN_VALUE && soundRMS != Double.MIN_VALUE)
                {
                    noiseCtx = new VNoise((float)soundDB, (float)soundRMS, 0, 0, gmtTime);
                }
                currentCtx.setNoiseContext(noiseCtx);
                break;

            case "android.net.conn.CONNECTIVITY_CHANGE":
                boolean isWifi = Connectivity.isConnectedWifi(c);
                String wifiSsid = Connectivity.getWifiName(c);
                WiFi wifiCtx = new WiFi(isWifi, wifiSsid);

                boolean isMobile = Connectivity.isConnectedMobile(c);
                boolean isMobileFast = Connectivity.isConnectedFast(c);
                CellularNetwork.MobileType mobileNetType = Connectivity.getMobileNetworkClass(c);
                CellularNetwork mobileCtx = new CellularNetwork(isMobile, isMobileFast, mobileNetType);

                currentCtx.setNetworkContext(
                        new VNetwork(wifiCtx, mobileCtx));
                break;
        }
        VStore.getInstance().provideContext(currentCtx).persistContext(true);
    }

    private String getLocationGeoText(Context c, VLocation location) {
        //Get the address string for the received location (stolen from AWARE plugin
        //"Google Fused Location")
        StringBuilder geo_text = new StringBuilder();
        ConnectivityManager connectivityManager
                = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        //Check if network is available
        if (networkInfo != null && networkInfo.isConnectedOrConnecting())
        {
            try {
                Geocoder geo = new Geocoder(c, Locale.getDefault());
                List<Address> addressList = geo.getFromLocation(location.getLatLng().getLatitude(),
                        location.getLatLng().getLongitude(), 1);
                for (int i = 0; i < addressList.size(); i++)
                {
                    Address address1 = addressList.get(i);
                    for (int j = 0; j < address1.getMaxAddressLineIndex(); j++)
                    {
                        if (address1.getAddressLine(j).length() > 0)
                        {
                            geo_text.append(address1.getAddressLine(j)).append("\n");
                        }
                    }
                    geo_text.append(address1.getCountryName());
                }
            } catch (IOException e) {
            }
        }
        return geo_text.toString();
    }
}

