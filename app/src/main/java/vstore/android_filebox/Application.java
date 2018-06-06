package vstore.android_filebox;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import vstore.android_filebox.events.ExportFileEvent;
import vstore.android_filebox.utils.FileUtils;
import vstore.framework.VStore;
import vstore.framework.communication.download.events.DownloadFailedEvent;
import vstore.framework.communication.download.events.DownloadProgressEvent;
import vstore.framework.communication.download.events.DownloadStartEvent;
import vstore.framework.communication.download.events.DownloadedFileReadyEvent;
import vstore.framework.communication.upload.events.UploadBeginEvent;
import vstore.framework.communication.upload.events.UploadDoneCompletelyEvent;
import vstore.framework.communication.upload.events.UploadFailedEvent;
import vstore.framework.communication.upload.events.UploadFailedPermanentlyEvent;
import vstore.framework.communication.upload.events.UploadStateEvent;
import vstore.framework.config.events.ConfigDownloadFailedEvent;
import vstore.framework.config.events.ConfigDownloadSucceededEvent;
import vstore.framework.file.events.FileDeletedEvent;

/**
 * Global application state.
 *
 * Subscribes to globally important events
 */
public class Application extends android.app.Application {

    public static URL vstore_master_uri;
    static {
        try {
            vstore_master_uri = new URL("http://<address_of_master>:<port>");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        createNotificationChannel();
    }

    public void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.Communication_State);
            String description = getString(R.string.notify_channel_descr);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(getString(R.string.vstore_commstate_channel), name, importance);
            channel.setDescription(description);
            channel.setVibrationPattern(null);
            channel.enableLights(false);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /********************** Events regarding file handling ***********************/
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(FileDeletedEvent evt) {
        Toast.makeText(getApplicationContext(), R.string.file_deleted,
                Toast.LENGTH_SHORT).show();
    }

    Integer dl_notify = 4;
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DownloadedFileReadyEvent event) {
        DownloadedFileReadyEvent stickyEvent
                = EventBus.getDefault().getStickyEvent(DownloadedFileReadyEvent.class);
        if (stickyEvent == null) return;
        EventBus.getDefault().removeStickyEvent(event);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(dl_notify);

        //Open the file
        FileUtils.openFile(
                getApplicationContext(),
                FileProvider.getUriForFile(getApplicationContext(),
                        FileProvider.FILE_PROVIDER_AUTHORITY, new File(event.file.getFullPath())),
                event.file.getFileType());

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ExportFileEvent evt) {
        if(evt != null && evt.mUUID != null) {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            VStore vstor = VStore.getInstance();
            vstor.getFile(evt.mUUID, dir);
            Toast.makeText(
                    getApplicationContext(),
                    "File will be exported to Downloads folder...",
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DownloadFailedEvent evt) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(dl_notify);
    }

    /********************** Receive configuration events *********************/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ConfigDownloadSucceededEvent evt) {
        //Toast.makeText(getApplicationContext(), "Configuration download finished!", Toast.LENGTH_LONG).show();
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ConfigDownloadFailedEvent evt) {
        Toast.makeText(getApplicationContext(), "Config download failed. Reason: " + evt.errorCode.toString(), Toast.LENGTH_LONG).show();
    }

    /************************* Events regarding the upload state *************************/
    Integer ul_notifyId_ongoing = 1;
    Integer ul_notifyId_done = 2;
    Integer ul_notifyId_failed = 3;
    //Upload begins
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UploadBeginEvent evt) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setOngoing(true)
                .setContentTitle("vStore Upload in progress (0%)...")
                .setContentText("")
                .setSmallIcon(R.drawable.ic_launcher)
                .setProgress(100, 0, false)
                .setVibrate(null);
        if(android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            builder.setChannelId(getString(R.string.vstore_commstate_channel));
        }
        Notification notification = builder.build();
        notificationManager.cancel(ul_notifyId_failed);
        notificationManager.cancel(ul_notifyId_done);
        notificationManager.notify(ul_notifyId_ongoing, notification);
    }
    //Upload progress
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UploadStateEvent evt) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setOngoing(true)
                .setContentTitle("vStore Upload in progress (" + evt.getProgress() + "%)...")
                .setContentText("")
                .setSmallIcon(R.drawable.ic_launcher)
                .setProgress(100, evt.getProgress(), false)
                .setVibrate(null);
        if(android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            builder.setChannelId(getString(R.string.vstore_commstate_channel));
        }
        Notification notification = builder.build();
        notificationManager.notify(ul_notifyId_ongoing, notification);
    }
    //Upload done
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UploadDoneCompletelyEvent evt) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //Build "finished" notification
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setOngoing(true)
                .setContentTitle("Upload done!")
                .setContentText("A file has been uploaded successfully.")
                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(false)
                .setVibrate(null);
        if(android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            builder.setChannelId(getString(R.string.vstore_commstate_channel));
        }
        Notification notification = builder.build();
        notificationManager.cancel(ul_notifyId_ongoing);
        notificationManager.notify(ul_notifyId_done, notification);
    }
    //Upload failed
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UploadFailedEvent evt) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(ul_notifyId_ongoing);

        //Build "failed" notification
        String text;
        if(evt.willRetry()) {
            String attemptText = "" + evt.getNumberOfAttempt();
            switch(evt.getNumberOfAttempt())
            {
                case 1:
                    attemptText += "st";
                    break;
                case 2:
                    attemptText += "nd";
                    break;
                case 3:
                    attemptText += "rd";
                    break;
                case 4:
                case 5:
                default:
                    attemptText += "th";
                    break;
            }
            attemptText += " attempt";
            text = "We will retry in " + evt.getRetryTime() + "s ("+attemptText+").";
        } else {
            text = "Permanent error. There is nothing we can do.";
        }

        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setOngoing(true)
                .setContentTitle("Upload failed.")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(false)
                .setVibrate(null);
        if(android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            builder.setChannelId(getString(R.string.vstore_commstate_channel));
        }
        Notification notification = builder.build();
        notificationManager.cancel(ul_notifyId_done);
        notificationManager.cancel(ul_notifyId_ongoing);
        notificationManager.notify(ul_notifyId_failed, notification);
    }
    //Upload failed permanently
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UploadFailedPermanentlyEvent evt) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(ul_notifyId_ongoing);

        //Build "failed" notification
        String text = "Permanent error. There is nothing we can do.";

        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setOngoing(true)
                .setContentTitle("Upload failed.")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(false)
                .setVibrate(null);
        if(android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            builder.setChannelId(getString(R.string.vstore_commstate_channel));
        }
        Notification notification = builder.build();
        notificationManager.cancel(ul_notifyId_done);
        notificationManager.cancel(ul_notifyId_ongoing);
        notificationManager.notify(ul_notifyId_failed, notification);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DownloadStartEvent event) {
        Toast.makeText(getApplicationContext(), "New file is downloading...", Toast.LENGTH_LONG).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DownloadProgressEvent event) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setOngoing(false)
                .setContentTitle("vStore Download in progress ("+event.getProgress()+"%)...")
                .setContentText("")
                .setSmallIcon(R.drawable.ic_launcher)
                .setProgress(100, event.getProgress(), false)
                .setVibrate(null);
        if(android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            builder.setChannelId(getString(R.string.vstore_commstate_channel));
        }
        Notification notification = builder.build();
        notificationManager.notify(dl_notify, notification);
    }

}
