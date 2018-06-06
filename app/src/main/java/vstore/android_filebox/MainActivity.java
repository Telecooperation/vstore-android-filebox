package vstore.android_filebox;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import vstore.android_filebox.aware.AWAREConstants;
import vstore.android_filebox.aware.AwareController;
import vstore.android_filebox.config_activity.ConfigActivity;
import vstore.android_filebox.context_fragment.CurrentContextFragment;
import vstore.android_filebox.events.PermissionsDeniedEvent;
import vstore.android_filebox.events.PermissionsGrantedEvent;
import vstore.android_filebox.files_fragment.MyFilesFragment;
import vstore.android_filebox.rules_elements.RulesActivity;
import vstore.android_filebox.search_fragment.SearchFragment;
import vstore.android_filebox.services.MatchingFilesNotifierService;
import vstore.framework.VStore;
import vstore.framework.config.ConfigManager;
import vstore.framework.exceptions.VStoreException;

@SuppressLint("ApplySharedPref")
public class MainActivity extends AppCompatActivity {
    public static final String KEY_ACTIVITY_OPEN = "main_activity_open";
    public static final String STARTED_FOR_FILES_VIEW = "started_for_files";

    private ViewPager mViewPager;
    private BottomBar mBottomBar;

    private File vstore_base_dir;

    private void initializeVStore() {
        vstore_base_dir = this.getApplicationContext().getExternalFilesDir(null);
        vstore_base_dir.mkdirs();
        Log.d("vStore", "Initializing vstore at " + vstore_base_dir.getAbsolutePath());
        try
        {
            VStore.initialize(vstore_base_dir, Application.vstore_master_uri);
        }
        catch(VStoreException e)
        {
            e.printStackTrace();
            Log.d("VStore", "ErrorCode: " + e.getErrorCode().toString());
            Log.d("VStore", "ErrorMessage: " + e.getMessage());
            System.exit(0);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!AwareController.isAwareInstalled(this) || !AwareController.allPluginsInstalled(this))
        {
            setContentView(R.layout.layout_need_aware);
            showAwareScreen();
            return;
        }
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitleTextColor(0xFFFFFFFF);
        setSupportActionBar(myToolbar);


        initializeVStore();
        ConfigManager confMgr = VStore.getInstance().getConfigManager();
        confMgr.download(false);

        mBottomBar = findViewById(R.id.bottomBar);
        mViewPager = findViewById(R.id.viewPager);
        mViewPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));
        initBottomBar();
        initViewPager();

        //Start background notifier service
        startService(new Intent(getApplicationContext(), MatchingFilesNotifierService.class));

        Intent intent = getIntent();
        if(intent.hasExtra(STARTED_FOR_FILES_VIEW)) {
            mViewPager.setCurrentItem(2, true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.default_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                startConfigActivity();
                return true;
            case R.id.action_rules:
                startActivity(new Intent(this, RulesActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initBottomBar() {
        mBottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(@IdRes int tabId) {
                switch (tabId) {
                    case R.id.tab_current_context:
                        mViewPager.setCurrentItem(0, true);
                        break;

                    case R.id.tab_my_files:
                        mViewPager.setCurrentItem(1, true);
                        break;

                    case R.id.tab_filter:
                        mViewPager.setCurrentItem(2, true);
                        break;

                    default:
                        mViewPager.setCurrentItem(0, true);
                        break;
                }
            }
        });
    }

    private void initViewPager() {
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int pos) {
                switch (pos) {
                    case 0:
                        mBottomBar.selectTabAtPosition(0, true);
                        setTitle(R.string.menu_context);
                        break;
                    case 1:
                        mBottomBar.selectTabAtPosition(1, true);
                        setTitle(R.string.menu_my_files);
                        break;
                    case 2:
                        mBottomBar.selectTabAtPosition(2, true);
                        setTitle(R.string.menu_search);
                        break;
                    default:
                        mBottomBar.selectTabAtPosition(0, true);
                        setTitle(R.string.menu_context);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Register the event receivers in this class
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!AwareController.isAwareInstalled(this) || !AwareController.allPluginsInstalled(this))
        {
            if(findViewById(R.id.imgAwareAct) != null) {
                refreshPluginIcons();
            }
            return;
        }

        initializeVStore();
        //Write to shared pref that activity is open
        SharedPreferences p = getSharedPreferences(MatchingFilesNotifierService.MATCHING_FILES_SERVICE_PREF, MODE_PRIVATE);
        SharedPreferences.Editor edit = p.edit();
        edit.putBoolean(KEY_ACTIVITY_OPEN, true);
        edit.commit();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Unregister the event receivers in this class
        EventBus.getDefault().unregister(this);
        //Write to shared pref that activity is closed
        SharedPreferences p = getSharedPreferences(MatchingFilesNotifierService.MATCHING_FILES_SERVICE_PREF, MODE_PRIVATE);
        SharedPreferences.Editor edit = p.edit();
        edit.putBoolean(KEY_ACTIVITY_OPEN, false);
        edit.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VStore.getInstance().clean();
    }

    private void refreshPluginIcons() {
        int count = 0;
        //Show green ticks for installed plugins
        ImageView img = ((ImageView)findViewById(R.id.imgAwareTick));
        if(AwareController.isAwareInstalled(this)) {
            img.setVisibility(View.VISIBLE);
            count++;
        } else {
            img.setVisibility(View.GONE);
        }
        img = ((ImageView)findViewById(R.id.imgAwareLocTick));
        if(AwareController.isLocationPluginInstalled(this)) {
            img.setVisibility(View.VISIBLE);
            count++;
        } else {
            img.setVisibility(View.GONE);
        }
        img = ((ImageView)findViewById(R.id.imgAwareActTick));
        if(AwareController.isActivityPluginInstalled(this)) {
            img.setVisibility(View.VISIBLE);
            count++;
        } else {
            img.setVisibility(View.GONE);
        }
        img = ((ImageView)findViewById(R.id.imgAwareNoiseTick));
        if(AwareController.isNoisePluginInstalled(this)) {
            img.setVisibility(View.VISIBLE);
            count++;
        } else {
            img.setVisibility(View.GONE);
        }
        //If all are installed, reload activity
        if(count == 4) {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
    }

    private void startConfigActivity() {
        Intent intent = new Intent(this, ConfigActivity.class);
        startActivity(intent);
    }

    private void showAwareScreen() {
        //Add click listeners to go directly to play store page
        ImageView img;
        img = (ImageView) findViewById(R.id.imgAware);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id="+ AWAREConstants.AWARE));
                startActivity(intent);
            }
        });
        img = (ImageView) findViewById(R.id.imgAwareLoc);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id="+ AWAREConstants.LOCATION_PLUGIN));
                startActivity(intent);
            }
        });
        img = (ImageView) findViewById(R.id.imgAwareAct);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id="+ AWAREConstants.ACTIVITY_PLUGIN));
                startActivity(intent);
            }
        });
        img = (ImageView) findViewById(R.id.imgAwareNoise);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id="+ AWAREConstants.NOISE_PLUGIN));
                startActivity(intent);
            }
        });

        refreshPluginIcons();

        //Set click listener to try to re-request permissions on click.
        /*((Button)findViewById(R.id.btnRequestPermissions)).setOnClickListener(
                new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VStorage.requestPermissions(MainActivity.this);
            }
        });

        //Hide the AWARE message if it is installed
        if(AppUtils.isAwareInstalled(this)) {
            ((TextView) findViewById(R.id.txtInfo2)).setVisibility(View.GONE);
        }*/
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(PermissionsGrantedEvent evt) {
        PermissionsGrantedEvent stickyEvent
                = EventBus.getDefault().getStickyEvent(PermissionsGrantedEvent.class);
        if (stickyEvent != null) {
            EventBus.getDefault().removeStickyEvent(stickyEvent);
            //Permission is granted, so let's restart the app
            Toast.makeText(getApplicationContext(), "Restarting app...", Toast.LENGTH_SHORT).show();
            Intent mStartActivity = new Intent(this, MainActivity.class);
            int mPendingIntentId = 123456;
            PendingIntent mPendingIntent = PendingIntent
                    .getActivity(this,
                            mPendingIntentId,
                            mStartActivity,
                            PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            if(mgr != null) {
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
            }
            finish();
        }

    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(PermissionsDeniedEvent evt) {
        PermissionsDeniedEvent stickyEvent
                = EventBus.getDefault().getStickyEvent(PermissionsDeniedEvent.class);
        if (stickyEvent != null) {
            EventBus.getDefault().removeStickyEvent(stickyEvent);
        }
    }

    /**
     * This inner class provides the adapter for the view pager.
     */
    private class MyPagerAdapter extends FragmentStatePagerAdapter {

        private FragmentManager fm;

        MyPagerAdapter(FragmentManager fm) {
            super(fm);
            this.fm = fm;
        }

        @Override
        public Fragment getItem(int pos) {
            //Try to find fragment in Fragment manager
            Fragment fragment = fm.findFragmentByTag("android:switcher:"
                    + mViewPager.getId() + ":" + pos);
            //Return fragment if it is already in memory (e.g. in fragment manager)
            if (fragment != null) {
                return fragment;
            }

            //If not, get a new instance
            switch (pos) {
                case 0:
                    return CurrentContextFragment.newInstance();
                case 1:
                    return MyFilesFragment.newInstance();
                case 2:
                    return SearchFragment.newInstance();
                default:
                    return CurrentContextFragment.newInstance();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    }
}
