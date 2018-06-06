package vstore.android_filebox.config_activity;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import vstore.android_filebox.R;

/**
 * This Dialog displays a map and an input field to configure the location context for
 * the rule.
 */
public class NodeLocationDialog extends DialogFragment {

    private CustomMapView mMapView;
    private LatLng mSetMarker;

    /**
     * Create a new instance of the ContextLocation dialog fragment.
     */
    static NodeLocationDialog newInstance(LatLng location) {
        NodeLocationDialog f = new NodeLocationDialog();

        //Set marker if one is given
        if(location != null && location.latitude != 0 && location.longitude != 0) {
            f.mSetMarker = location;
        }
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View dialogLayout = getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_node_location, null);

        mMapView = (CustomMapView) dialogLayout.findViewById(R.id.mapView_selectNodeLocation);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {
                //Set "my location enabled" so that the user can see his location.
                //Boiler plate permission code necessary
                if (ActivityCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                } else {
                    googleMap.setMyLocationEnabled(true);
                }

                //Set initial marker if one was given at the beginning.
                //Else, use some hardcoded default values near Darmstadt.
                googleMap.clear();
                if(mSetMarker == null) {
                    mSetMarker = new LatLng(49.872751, 8.650677);
                }
                googleMap.addMarker(new MarkerOptions()
                        .position(mSetMarker)
                        .icon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                // Update the location and zoom of the MapView
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                        mSetMarker, 14);
                googleMap.animateCamera(cameraUpdate);

                //Set long click listener on map to set a marker
                googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(LatLng latLng) {
                        googleMap.clear();
                        mSetMarker = latLng;
                        googleMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .icon(BitmapDescriptorFactory
                                        .defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    }
                });
            }
        });

        builder.setView(dialogLayout)
                .setTitle("Please choose a location!")
                .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent();
                        //Give configured information back to EditNodeDialog
                        if(mSetMarker != null) {
                            intent.putExtra("lat", mSetMarker.latitude);
                            intent.putExtra("lng", mSetMarker.longitude);
                            getTargetFragment().onActivityResult(
                                    getTargetRequestCode(),
                                    EditNodeDialog.OK_REQUEST,
                                    intent);
                        } else {
                            //No marker set, so do not return anything and set "cancel" result code
                            getTargetFragment().onActivityResult(
                                    getTargetRequestCode(),
                                    EditNodeDialog.CANCEL_REQUEST,
                                    null);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                    }
                });

        return builder.create();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }
}
