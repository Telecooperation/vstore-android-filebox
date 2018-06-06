package vstore.android_filebox.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.EventBusException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import vstore.android_filebox.Application;
import vstore.android_filebox.MainActivity;
import vstore.android_filebox.R;
import vstore.framework.VStore;
import vstore.framework.communication.ApiConstants;
import vstore.framework.context.ContextDescription;
import vstore.framework.context.ContextFilter;
import vstore.framework.context.SearchContextDescription;
import vstore.framework.context.events.NewContextEvent;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeManager;
import vstore.framework.utils.ContextUtils;

import static android.app.Notification.DEFAULT_ALL;

/**
 * If new context is received, this service fetches files that match this context and notifies
 * the user if new files are available.
 */
public class MatchingFilesNotifierService extends Service {

    public static final String MATCHING_FILES_SERVICE_PREF = "MatchingFilesService";

    /**
     * Only notify the user at a frequency of 10 minutes (converted to milliseconds).
     */
    private int MIN_INTERVAL = 1 *60*1000;
    private long mLastTime = 0;

    private Set<String> mLastUUIDs;
    private Set<String> mNewUUIDs;
    private int mNewCount = 0;

    private ContextDescription mUsageContext;
    private SearchContextDescription mSearchContext;

    private NodeManager mNodeManager;

    private boolean mIsTimerScheduled;
    private Timer mTimer;
    private Runnable mRunnable;
    private Thread mThread;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        if(VStore.getInstance() == null) {
            try {
                VStore.initialize(this.getExternalFilesDir(null), Application.vstore_master_uri);
            } catch(Exception ignored) {}
        }

        mNodeManager = NodeManager.get();
        mLastUUIDs = readFromSharedPref();
        mNewUUIDs = new HashSet<>();
        mLastTime = readLongFromSharedPref("last_time");

        if(!acquireContext()) {
            return;
        }

        //Code for the thread that actually sends requests on a different thread
        mRunnable = new Runnable() {
            @Override
            public void run() {
                Map<String, NodeInfo> nodes = mNodeManager.getNodeList();
                mNewUUIDs.clear();
                //Send request to each node. Block while waiting for reply from a node.
                for(NodeInfo n : nodes.values())
                {
                    final String address = n.getAddress() + ":" + n.getPort() + ApiConstants.StorageNode.ROUTE_FILES_MATCHING_CONTEXT;
                    try {
                        Ion.with(getApplicationContext())
                                .load("POST", address)
                                .setMultipartParameter("context", mSearchContext.getJson().toString())
                                .setMultipartParameter("phoneID", VStore.getDeviceIdentifier())
                                .asJsonObject()
                                .setCallback(mResultCallback)
                                .get(10, TimeUnit.SECONDS); //Block and timeout after 10 seconds
                    } catch(InterruptedException e) {
                        return;
                    } catch(ExecutionException | TimeoutException e) { }
                }

                if(mNewCount > 0 || (mNewCount == 0 && (mLastUUIDs.size() != mNewUUIDs.size()))) {
                    showNotification(mNewCount);
                }
                mIsTimerScheduled = false;
                return;
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        try {
            EventBus.getDefault().register(this);
        } catch(EventBusException e) {}
        return START_STICKY;
    }

    FutureCallback<JsonObject> mResultCallback = new FutureCallback<JsonObject>() {
        @Override
        public void onCompleted(Exception e, JsonObject result) {
            if (e == null) {
                //Take the response and put the file addresses and UUIDs into a list
                //Post an event with this list.
                //The VStorage Framework provides methods for downloading a thumbnail
                //or the whole file, or just metadata. Apps can use these methods.
                if (result.get("error") != null && !result.get("error").getAsBoolean()) {
                    if (result.has("reply") && result.getAsJsonObject("reply").has("files")) {
                        JsonArray files = result.getAsJsonObject("reply").getAsJsonArray("files");
                        if (files.size() > 0) {
                            for(JsonElement o : files) {
                                JsonObject row = (JsonObject) o;
                                if (row != null && row.has("uuid")) {
                                    String uuid = row.get("uuid").getAsString();
                                    mNewUUIDs.add(uuid);
                                    //Check if this file id is new and not created by myself
                                    if(!mLastUUIDs.contains(uuid) && !VStore.getInstance().isMyFile(uuid))
                                    {
                                        mNewCount++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    };

    private boolean acquireContext() {
        //Get the configured filter
        SharedPreferences p = getApplicationContext()
                .getSharedPreferences("searchfilterconfig", Context.MODE_PRIVATE);
        ContextFilter f;
        if(p != null) {
            f = new ContextFilter(p.getString("filterconfig", ContextFilter.getDefaultFilter().getJson().toString()));
        } else {
            f = ContextFilter.getDefaultFilter();
        }
        //Get current usage context
        try {
            mUsageContext = VStore.getInstance().getCurrentContext();
        } catch(NullPointerException e) {
            return false;
        }

        //Apply the filter to the context and create
        //a new SearchContextDescription
        mSearchContext = ContextUtils.applyFilter(mUsageContext, f);

        return true;
    }

    /**
     * Called when new files are available. The user will then be notified, if the last notification
     * was already MIN_INTERVAL minutes ago.
     */
    private void showNotification(final int number) {
        long diff = System.currentTimeMillis() - mLastTime;

        //Ignore until we are allowed to notify again
        if(number > 0 && diff >= MIN_INTERVAL && !readFromSharedPref(MainActivity.KEY_ACTIVITY_OPEN)) {

            //Replace list of last UUIDs with the UUIDs from the new list
            mLastUUIDs.clear();
            mLastUUIDs.addAll(mNewUUIDs);
            putToSharedPref(mLastUUIDs);

            //Create the notification
            String message;
            if (number == 1) {
                message = "1 new file is matching your context!";
            } else {
                message = number + " new files are matching your context!";
            }
            PackageManager pm = getApplicationContext().getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
            intent.putExtra(MainActivity.STARTED_FOR_FILES_VIEW, true);
            PendingIntent pendInt = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("VStorage")
                            .setContentText(message)
                            .setDefaults(DEFAULT_ALL)
                            .setAutoCancel(true)
                            .setContentIntent(pendInt);

            NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.notify(1, mBuilder.build());
            //Save the new "last time we notified"
            mLastTime = System.currentTimeMillis();
            putToSharedPref("last_time", mLastTime);
        }
        mNewUUIDs.clear();
        mNewCount = 0;
    }

    private void putToSharedPref(Set<String> uuids) {
        SharedPreferences p = getSharedPreferences(MATCHING_FILES_SERVICE_PREF, MODE_PRIVATE);
        SharedPreferences.Editor edit = p.edit();
        edit.putStringSet("lastUUIDs", uuids);
        edit.commit();
    }

    private void putToSharedPref(String key, long value) {
        SharedPreferences p = getSharedPreferences(MATCHING_FILES_SERVICE_PREF, MODE_PRIVATE);
        SharedPreferences.Editor edit = p.edit();
        edit.putLong(key, value);
        edit.commit();
    }

    private Set<String> readFromSharedPref() {
        SharedPreferences p = getSharedPreferences(MATCHING_FILES_SERVICE_PREF, MODE_PRIVATE);
        return p.getStringSet("lastUUIDs", new HashSet<String>());
    }

    private long readLongFromSharedPref(String key) {
        SharedPreferences p = getSharedPreferences(MATCHING_FILES_SERVICE_PREF, MODE_PRIVATE);
        return p.getLong(key, 0);
    }
    private boolean readFromSharedPref(String key) {
        SharedPreferences p = getSharedPreferences(MATCHING_FILES_SERVICE_PREF, MODE_PRIVATE);
        return p.getBoolean(key, false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(NewContextEvent evt) {
        if(!acquireContext()) { return; }

        //Delay fetching of files for 10 seconds in case that more context arrives
        if (!mIsTimerScheduled) {
            mIsTimerScheduled = true;
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(mThread == null || !mThread.isAlive()) {
                        mThread = new Thread(mRunnable);
                        mThread.start();
                    }
                }
            }, 5000);
        }

    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        if(mThread != null && mThread.isAlive()) {
            mThread.interrupt();
        }
    }
}
