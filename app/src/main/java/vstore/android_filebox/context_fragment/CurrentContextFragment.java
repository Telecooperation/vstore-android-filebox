package vstore.android_filebox.context_fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import vstore.android_filebox.R;
import vstore.android_filebox.aware.AwareController;
import vstore.android_filebox.context_monitor.ManualContextUpdater;
import vstore.android_filebox.utils.ContextUtils;
import vstore.framework.VStore;
import vstore.framework.context.ContextDescription;
import vstore.framework.context.events.NewContextEvent;
import vstore.framework.context.types.activity.VActivity;
import vstore.framework.context.types.location.VLatLng;
import vstore.framework.context.types.location.VLocation;
import vstore.framework.context.types.network.VNetwork;
import vstore.framework.context.types.noise.VNoise;
import vstore.framework.context.types.place.VSinglePlace;

public class CurrentContextFragment extends Fragment {
    private NestedScrollView mContext_mainScrollView;

    private LinearLayout mCardLocContent;
    private MapView mSmallMapView;
    private TextView mAddress;
    private TextView mTxtCurLatLong;
    private TextView mTxtTimeOfFix;

    private LinearLayout mCardPlacesContent;
    private LinearLayout mCardPlacesList;

    private LinearLayout mCardActivityContent;
    private LinearLayout mSmallActivitiesContainer;
    private TextView mTxtNoActivityContext;

    private RelativeLayout mCardNetworkContent;
    private ImageView mImgWifiVal;
    private TextView mTxtWifiSsid;
    private TextView mTxtMobileVal;
    private TextView mTxtMobileNet;

    private LinearLayout mCardNoiseContent;
    private TextView mTxtNoiseDbVal;
    //private TextView mTxtNoiseRMSVal;
    private TextView mTxtNoiseSilentVal;

    private SwipeRefreshLayout mLayoutSwipeRefreshContext;

    private ContextDescription mCurrentContext;
    private Long mLastUpdate;

    private boolean mWasPaused = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_current_context, container, false);
        setHasOptionsMenu(true);
        mContext_mainScrollView = (NestedScrollView) v.findViewById(R.id.context_mainScrollView);
        mLayoutSwipeRefreshContext = (SwipeRefreshLayout) v.findViewById(R.id.layoutSwipeRefreshContext);
        //Find location card views
        mCardLocContent = (LinearLayout) v.findViewById(R.id.cardLocContent);
        mSmallMapView = (MapView) v.findViewById(R.id.mapView_currentLocation);
        mAddress = (TextView) v.findViewById(R.id.txtAddress);
        mTxtCurLatLong = (TextView) v.findViewById(R.id.txtCurLatLong);
        mTxtTimeOfFix = (TextView) v.findViewById(R.id.txtTimeOfFix);
        //Find places card views
        mCardPlacesContent = (LinearLayout) v.findViewById(R.id.cardPlacesContent);
        ScrollView mPlacesListScrollView = (ScrollView) v.findViewById(R.id.placesListScrollView);
        mCardPlacesList = (LinearLayout) v.findViewById(R.id.cardPlacesList);
        //Find activity card views
        mCardActivityContent = (LinearLayout) v.findViewById(R.id.cardActivityContent);
        mSmallActivitiesContainer = (LinearLayout) v.findViewById(R.id.smallActivitiesContainer);
        mTxtNoActivityContext = (TextView)v.findViewById(R.id.txtNoActivityContext);
        //Find network card views
        mCardNetworkContent = (RelativeLayout) v.findViewById(R.id.cardNetworkContent);
        mImgWifiVal = (ImageView) v.findViewById(R.id.imgWifiVal);
        mTxtWifiSsid = (TextView) v.findViewById(R.id.txtWifiSsid);
        mTxtMobileVal = (TextView) v.findViewById(R.id.txtMobileVal);
        mTxtMobileNet = (TextView) v.findViewById(R.id.txtMobileNet);
        //Find noise card views
        mCardNoiseContent = (LinearLayout) v.findViewById(R.id.cardNoiseContent);
        mTxtNoiseDbVal = (TextView) v.findViewById(R.id.txtNoiseDbVal);
        //mTxtNoiseRMSVal = (TextView) v.findViewById(R.id.txtNoiseRMSVal);
        mTxtNoiseSilentVal = (TextView) v.findViewById(R.id.txtNoiseSilentVal);

        mSmallMapView.onCreate(savedInstanceState);

        setCardToggleListeners(v);

        //Make it possible to scroll scrollview in scrollview
        mContext_mainScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                getActivity().findViewById(R.id.placesListScrollView).getParent()
                        .requestDisallowInterceptTouchEvent(false);
                return false;
            }
        });
        mPlacesListScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Disallow the touch request for parent scroll on touch of child view
                mContext_mainScrollView.requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        mLayoutSwipeRefreshContext.setColorSchemeColors(getResources().getColor(R.color.colorAccent));
        mLayoutSwipeRefreshContext.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ManualContextUpdater ctxUpdater = new ManualContextUpdater(getActivity());
                ctxUpdater.run();
            }
        });

        mCurrentContext = VStore.getInstance().getCurrentContext();
        if(mLastUpdate == null) {
            mLastUpdate = System.currentTimeMillis();
            updateAllCards();
        }

        return v;
    }

    public static CurrentContextFragment newInstance() {
        return new CurrentContextFragment();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.current_context_top_menu, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_refresh:
                AwareController.configureAwarePlugins(getActivity());
                ManualContextUpdater ctxUpdater = new ManualContextUpdater(getActivity());
                ctxUpdater.run();
                return true;
        }

        return false;
    }

    private void setCardToggleListeners(View baseView) {
        LinearLayout locTitle = (LinearLayout) baseView.findViewById(R.id.cardLocationTitle);
        locTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TransitionManager.beginDelayedTransition(mCardLocation);
                if(mCardLocContent.getVisibility() == View.GONE) {
                    mCardLocContent.setVisibility(View.VISIBLE);
                } else {
                    mCardLocContent.setVisibility(View.GONE);
                }
            }
        });
        LinearLayout placesTitle = (LinearLayout) baseView.findViewById(R.id.cardPlacesTitle);
        placesTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TransitionManager.beginDelayedTransition(mCardActivity);
                if(mCardPlacesContent.getVisibility() == View.GONE) {
                    mCardPlacesContent.setVisibility(View.VISIBLE);
                } else {
                    mCardPlacesContent.setVisibility(View.GONE);
                }
            }
        });
        LinearLayout activityTitle = (LinearLayout) baseView.findViewById(R.id.cardActivityTitle);
        activityTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TransitionManager.beginDelayedTransition(mCardActivity);
                if(mCardActivityContent.getVisibility() == View.GONE) {
                    mCardActivityContent.setVisibility(View.VISIBLE);
                } else {
                    mCardActivityContent.setVisibility(View.GONE);
                }
            }
        });
        LinearLayout networkTitle = (LinearLayout) baseView.findViewById(R.id.cardNetworkTitle);
        networkTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TransitionManager.beginDelayedTransition(mCardActivity);
                if(mCardNetworkContent.getVisibility() == View.GONE) {
                    mCardNetworkContent.setVisibility(View.VISIBLE);
                } else {
                    mCardNetworkContent.setVisibility(View.GONE);
                }
            }
        });
        LinearLayout noiseTitle = (LinearLayout) baseView.findViewById(R.id.cardNoiseTitle);
        noiseTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TransitionManager.beginDelayedTransition(mCardActivity);
                if(mCardNoiseContent.getVisibility() == View.GONE) {
                    mCardNoiseContent.setVisibility(View.VISIBLE);
                } else {
                    mCardNoiseContent.setVisibility(View.GONE);
                }
            }
        });
    }

    private void updateLocationCard() {
        if(mCurrentContext != null) {
            //Only update map and show information if location data is available
            final VLocation loc = mCurrentContext.getLocationContext();
            if(loc != null && loc.getLatLng() != null) {
                mSmallMapView.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap map) {
                        if (map != null) {
                            map.getUiSettings().setMyLocationButtonEnabled(false);

                            VLatLng tmpLatLng = loc.getLatLng();
                            LatLng currentContextLatLong
                                    = new LatLng(tmpLatLng.getLatitude(), tmpLatLng.getLongitude());
                            map.clear();
                            // Update the location and zoom of the MapView
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                                    currentContextLatLong, 12);
                            map.animateCamera(cameraUpdate);
                            map.addMarker(new MarkerOptions()
                                    .position(currentContextLatLong));
                        }
                    }
                });

                mAddress.setText(loc.getDescription());
                mTxtCurLatLong.setText(String.valueOf(loc.getLatLng().getLatitude()) + "\n" +
                        String.valueOf(loc.getLatLng().getLongitude()));
                Date locFixDate = new java.util.Date(loc.getTimestamp());
                String vv = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN).format(locFixDate);
                mTxtTimeOfFix.setText(vv);
            } else {
                mAddress.setText("N/A");
                mTxtCurLatLong.setText("N/A");
                mTxtTimeOfFix.setText("N/A");
            }
        }
    }

    private void updatePlacesCard() {
        //Clear the currently displayed places
        mCardPlacesList.removeAllViews();
        //Check if places are available
        ArrayList<VSinglePlace> places = mCurrentContext.getPlacesList();
        if(places != null && places.size() != 0) {
            DecimalFormat df = new DecimalFormat("#.##");
            df.setRoundingMode(RoundingMode.CEILING);
            for(VSinglePlace p : places) {
                //Get all data we want to display
                String name = p.getName();
                String likelihood = df.format(p.getLikelihood()*100) + "%";
                String placeTypeText = p.getPlaceTypeText();
                String distanceString = "(" + df.format(p.getDistance()) + "km)";

                //Inflate the layout for a list row
                LinearLayout v = (LinearLayout) getActivity().getLayoutInflater()
                        .inflate(R.layout.place_row, null);
                //First text view of the layout is the name
                TextView txtName = (TextView)v.getChildAt(0);
                txtName.setText(name + " " + distanceString);
                //Second text view of the layout is the likelihood
                TextView txtLikeli = (TextView)v.getChildAt(1);
                txtLikeli.setText(getResources()
                        .getString(R.string.likelihood_colon) + " " + likelihood);
                //Third text view of the layout is the place type
                TextView txtType = (TextView) v.getChildAt(2);
                if(placeTypeText != null) {
                    txtType.setText(getResources().getString(R.string.type_colon) + " " + placeTypeText);
                }
                //Set color green if place is the one the user is at
                if(p.isLikely()) {
                    txtName.setTextColor(getResources().getColor(R.color.green));
                    txtLikeli.setTextColor(getResources().getColor(R.color.green));
                    txtType.setTextColor(getResources().getColor(R.color.green));
                }
                //Add to the card
                mCardPlacesList.addView(v);
            }
        }
        else
        {
            //No places available, so tell the user
            View v = getActivity().getLayoutInflater().inflate(R.layout.no_places_layout, null);
            mCardPlacesList.getLayoutParams().height = 30;
            mCardPlacesList.addView(v);
        }
    }

    private void updateActivityCard() {
        //Scroll Activity Scroll View to the right
        /*mScrollActivityHistory.post(new Runnable() {
            @Override
            public void run() {
                mScrollActivityHistory.fullScroll(View.FOCUS_RIGHT);
            }
        });*/
        mSmallActivitiesContainer.removeAllViews();
        VActivity a = mCurrentContext.getActivityContext();
        if(a != null) {
            //Inflate the layout for a new activity
            LinearLayout v = (LinearLayout) getActivity().getLayoutInflater()
                    .inflate(R.layout.one_activity_small, mSmallActivitiesContainer, false);
            //First view of the layout is the image
            AppCompatImageView imgView = ((AppCompatImageView)v.getChildAt(0));
            String description;
            imgView.setImageResource(ContextUtils.getIconForActivity(a.getType()));
            description = ContextUtils.getStringForActivity(a.getType());

            //Second view of the layout is the confidence
            ((TextView)v.getChildAt(1)).setText(description + " (" + a.getConfidence() + "%)");
            //Third view of the layout is the duration
            long diff = System.currentTimeMillis() - mCurrentContext.getActivityTimestamp();
            int hours   = (int) ((diff / (1000*60*60)) % 24);
            int minutes = (int) ((diff / (1000*60)) % 60);
            int seconds = (int) (diff / 1000) % 60 ;
            String duration = hours + "h, " + minutes + "m, " + seconds + "s";
            ((TextView)v.getChildAt(2)).setText("for  " + duration);

            mSmallActivitiesContainer.addView(v);
            mSmallActivitiesContainer.setVisibility(View.VISIBLE);
            mTxtNoActivityContext.setVisibility(View.GONE);
        } else {
            mSmallActivitiesContainer.setVisibility(View.GONE);
            mTxtNoActivityContext.setVisibility(View.VISIBLE);
        }
    }

    private void updateNetworkCard() {
        //Check if network data is available
        VNetwork net = mCurrentContext.getNetworkContext();
        if(net != null) {
            if(net.getWiFiContext().isWifiConnected()) {
                mImgWifiVal.setImageResource(R.drawable.ic_wifi_black_24dp);
                mTxtWifiSsid.setText(net.getWiFiContext().getWifiSSID());
            } else {
                mImgWifiVal.setImageResource(R.drawable.ic_no_wifi_black_24dp);
                mTxtWifiSsid.setText(getString(R.string.no_wifi));
            }
            String netString = ContextUtils.getStringForNetwork(net.getMobileContext().getMobileNetworkType());
            mTxtMobileVal.setText(netString);
            if(net.getMobileContext().isMobileConnected()) {
                mTxtMobileNet.setText(R.string.mobile_connected);
            } else {
                mTxtMobileNet.setText(R.string.mobile_not_connected);
            }
        } else {
            mImgWifiVal.setImageResource(R.drawable.ic_no_wifi_black_24dp);
            mTxtWifiSsid.setText(getString(R.string.no_data));
            mTxtMobileVal.setText("?");
            mTxtMobileNet.setText(R.string.no_data);
        }
    }

    private void updateNoiseCard() {
        //Check if places are available
        VNoise noise = mCurrentContext.getNoiseContext();
        if(noise != null) {
            DecimalFormat df = new DecimalFormat("#.##");
            df.setRoundingMode(RoundingMode.CEILING);
            mTxtNoiseDbVal.setText(df.format(noise.getDb()) + "dB");
            //mTxtNoiseRMSVal.setText(df.format(noise.getRMS()));
            if(noise.isSilent()) {
                mTxtNoiseSilentVal.setText(getString(R.string.yes));
            } else {
                mTxtNoiseSilentVal.setText(getString(R.string.no));
            }
        }
        else
        {
            //No noise values available
            mTxtNoiseDbVal.setText("N/A");
        }
    }

    private void updateAllCards() {
        updateLocationCard();
        updatePlacesCard();
        updateActivityCard();
        updateNoiseCard();
        updateNetworkCard();
        mLayoutSwipeRefreshContext.setRefreshing(false);
    }

    public void onStart(){
        super.onStart();
        //Register the event receivers in this class
        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSmallMapView.onResume();
        //Check if we need to refresh current usage context, if we resume the fragment
        if((Math.abs(System.currentTimeMillis() - mLastUpdate) > 1000 * 60) || mWasPaused)
        {
            mCurrentContext = VStore.getInstance().getCurrentContext();
            updateAllCards();
            mWasPaused = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mWasPaused = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        //Unregister the event receivers in this class
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSmallMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mSmallMapView.onLowMemory();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(NewContextEvent evt) {
        VStore vStor = VStore.getInstance();
        assert vStor != null;
        mCurrentContext = vStor.getCurrentContext();
        if (evt.isUpdated_places) {
            updatePlacesCard();
        }
        if (evt.isUpdated_activity) {
            updateActivityCard();
        }
        if (evt.isUpdated_location) {
            updateLocationCard();
        }
        if (evt.isUpdated_noise) {
            updateNoiseCard();
        }
        if (evt.isUpdated_network) {
            updateNetworkCard();
        }
        if (evt.isUpdated_all) {
            updateAllCards();
        }
    }
}
