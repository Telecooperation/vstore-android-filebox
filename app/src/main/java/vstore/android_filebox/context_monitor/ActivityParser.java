package vstore.android_filebox.context_monitor;

import com.google.android.gms.location.DetectedActivity;

import vstore.framework.context.types.activity.ActivityType;

public class ActivityParser {

    public static ActivityType parse(int activity) {
        switch(activity)
        {
            case DetectedActivity.IN_VEHICLE:
                return ActivityType.IN_VEHICLE;

            case DetectedActivity.WALKING:
            case DetectedActivity.ON_FOOT:
                return ActivityType.WALKING;

            case DetectedActivity.STILL:
                return ActivityType.STILL;

            default:
                return ActivityType.UNKNOWN;
        }
    }
}
