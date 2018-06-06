package vstore.android_filebox.utils;

import vstore.android_filebox.R;
import vstore.framework.context.types.activity.ActivityType;
import vstore.framework.context.types.network.cellular.CellularNetwork;

/**
 * Class providing convenience methods needed for doing context related things.
 */
public class ContextUtils {

    /**
     * Returns a string that is a readable name for the activity.
     * @param activity The id of the user activity.
     * @return The corresponding string.
     */
    public static String getStringForActivity(ActivityType activity) {
        switch(activity) {
            case STILL:
                return "Still";

            case WALKING:
                return "Walking";

            case IN_VEHICLE:
                return "Driving";

            case UNKNOWN:
            default:
                return "Unknown";
        }
    }

    /**
     * Returns a drawable resource that contains the image corresponding to the given activity id.
     * @param activity The id of the user activity.
     * @return The corresponding drawable.
     */
    public static int getIconForActivity(ActivityType activity) {
        switch(activity) {
            case STILL:
                return R.drawable.ic_sitting_grey_24dp;

            case WALKING:
                return R.drawable.ic_walk_grey_24dp;

            case IN_VEHICLE:
                return R.drawable.ic_car_grey_24dp;

            case UNKNOWN:
            default:
                return R.drawable.ic_wait_grey_24dp;
        }
    }

    public static String getStringForNetwork(CellularNetwork.MobileType type) {
        switch(type) {
            case NET_2G:
                return "2G";
            case NET_3G:
                return "3G";
            case NET_3_5_G:
                return "3.5G";
            case NET_4G:
                return "4G";
            default:
                return "2G";
        }
    }


    /**
     * @return An array of supported mobile network types.
     */
    public static String[] getMobileStrings() {
        CellularNetwork.MobileType[] types = vstore.framework.utils.ContextUtils.getSupportedMobileTypes();
        String[] supTypes = new String[types.length];
        for (int i = 0; i < types.length; i++)
        {
            supTypes[i] = types[i].name();
        }
        return supTypes;
    }
}