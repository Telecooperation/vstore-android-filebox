package vstore.android_filebox.services;

import android.Manifest;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;

import java.util.ArrayList;
import java.util.List;

import vstore.android_filebox.R;
import vstore.android_filebox.config_activity.ConfigActivity;
import vstore.android_filebox.config_activity.Constants;
import vstore.android_filebox.context_monitor.PlaceTypeParser;
import vstore.android_filebox.utils.StringUtils;
import vstore.framework.VStore;
import vstore.framework.context.ContextDescription;
import vstore.framework.context.types.location.VLatLng;
import vstore.framework.context.types.location.VLocation;
import vstore.framework.context.types.place.PlaceType;
import vstore.framework.context.types.place.VPlaces;
import vstore.framework.context.types.place.VSinglePlace;
import vstore.framework.utils.ContextUtils;

import static vstore.framework.context.types.place.VSinglePlace.DISTANCE_THRESHOLD;
import static vstore.framework.context.types.place.VSinglePlace.LIKELIHOOD_THRESHOLD;
import static vstore.framework.context.types.place.VSinglePlace.LIKELIHOOD_THRESHOLD_2;

/**
 * This IntentService is responsible for fetching "Nearby places" data from the
 * Google Awareness API.
 *
 */
public class PlacesBackgroundService extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    public PlacesBackgroundService() {
        super("PlacesBackgroundService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        mGoogleApiClient.connect();

        //Check if Location permissions are granted
        int fineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if(fineLocationPermission == PackageManager.PERMISSION_GRANTED)
        {
            PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                    .getCurrentPlace(mGoogleApiClient, null);
            result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                @Override
                public void onResult(@NonNull PlaceLikelihoodBuffer likelyPlaces) {
                    VPlaces places = generatePlaces(likelyPlaces);
                    likelyPlaces.release();
                    ContextDescription curCtx = VStore.getInstance().getCurrentContext();
                    curCtx.setPlacesContext(places);
                    VStore.getInstance().provideContext(curCtx);
                    VStore.getInstance().persistContext(true);
                }
            });
        }
        else
        {
            //If permissions are not granted, create a notification and tell the user to
            //grant permissions.
            NotificationCompat.Builder notificBuilder =
                    new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_add_location_black_24dp)
                    .setContentTitle("Permission required.")
                    .setContentText("Please grant permission to use location!")
                    .setAutoCancel(true);
            NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            //Create an Intent and a PendingIntent to start the ConfigActivity for
            //showing a dialog to request for permission. Necessary because showing the dialog
            //is not possible from a Background Service.
            Intent resultIntent = new Intent(this, ConfigActivity.class);
            Bundle b = new Bundle();
            b.putBoolean(Constants.WAS_STARTED_FOR_PERMISSION_REQUESTS, true);
            resultIntent.putExtras(b);
            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(
                            this,
                            0,
                            resultIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            notificBuilder.setContentIntent(resultPendingIntent);
            mNotifyMgr.notify(1, notificBuilder.build());
        }
    }

    /**
     * This method generates the VPlaces object for the given PlaceLikelihoodBuffer.
     * @param l A PlaceLikelihoodBuffer coming from the Google API. Contains nearby places.
     * @return A VPlaces containing the places information for the current context.
     */
    private VPlaces generatePlaces(PlaceLikelihoodBuffer l) {
        ArrayList<VSinglePlace> places = new ArrayList<>();

        //Get current location context, if available
        VLocation locCtx = null;
        VStore vstor = VStore.getInstance();
        if(vstor != null) {
            ContextDescription usageContext = vstor.getCurrentContext();
            if(usageContext != null) {
                locCtx = usageContext.getLocationContext();
            }
        }

        //Create json for every single place, if it is supported
        for (PlaceLikelihood p : l)
        {
            Place place = p.getPlace();
            List<Integer> types = place.getPlaceTypes();
            if(types.size() == 0) { continue; }

            //Check if Google API place type number is supported
            if(PlaceTypeParser.isPlaceTypeKnown(types.get(0)))
            {
                //Place type is supported
                PlaceType category = PlaceTypeParser.getPlaceCategory(types.get(0));

                VSinglePlace singlePlace = new VSinglePlace(
                        place.getId(),
                        place.getName().toString(),
                        category,
                        place.getLatLng().latitude,
                        place.getLatLng().longitude,
                        p.getLikelihood());
                singlePlace.setPlaceTypeText(StringUtils.capitalizeOnlyFirstLetter(category.toString()));

                //Set a place as most likely if distance and likelihood meet the
                //requirements
                if(locCtx != null) {
                    VLatLng placeLatLng = new VLatLng(p.getPlace().getLatLng().latitude, p.getPlace().getLatLng().longitude);
                    float distToPlace = ContextUtils.distanceBetween(locCtx.getLatLng(), placeLatLng);

                    //Ensure that most likely place is only marked as such if
                    //certain thresholds are crossed for distance and likelihood
                    if((distToPlace <= DISTANCE_THRESHOLD && p.getLikelihood() >= LIKELIHOOD_THRESHOLD)
                            || (p.getLikelihood() > LIKELIHOOD_THRESHOLD_2)){
                        singlePlace.setMostLikely(true);
                    } else {
                        singlePlace.setMostLikely(false);
                    }
                } else {
                    singlePlace.setMostLikely(false);
                }
                places.add(singlePlace);
            }
        }
        return new VPlaces(places, System.currentTimeMillis());
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) { }
    @Override
    public void onConnectionSuspended(int i) { }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) { }
}

