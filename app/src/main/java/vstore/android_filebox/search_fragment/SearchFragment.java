package vstore.android_filebox.search_fragment;


import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionMenu;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import vstore.android_filebox.ItemClickSupport;
import vstore.android_filebox.MetadataFragment;
import vstore.android_filebox.R;
import vstore.android_filebox.utils.Connectivity;
import vstore.framework.VStore;
import vstore.framework.communication.download.events.DownloadFailedEvent;
import vstore.framework.communication.download.events.DownloadProgressEvent;
import vstore.framework.communication.download.events.DownloadedFileReadyEvent;
import vstore.framework.communication.download.events.MetadataEvent;
import vstore.framework.communication.download.events.NewThumbnailEvent;
import vstore.framework.communication.download.events.ThumbnailDownloadFailedEvent;
import vstore.framework.context.ContextFilter;
import vstore.framework.context.types.place.VSinglePlace;
import vstore.framework.file.MatchingResultRow;
import vstore.framework.file.events.FileDeletedEvent;
import vstore.framework.file.events.NewFilesMatchingContextEvent;

public class SearchFragment extends Fragment {
    public static final int REQUEST_EDIT_FILTER = 0;
    public static final int OK_REQUEST = 1;

    private RecyclerView mRecyclerMatchedFiles;
    private List<MatchingResultRow> mResults;
    private MatchedFilesRecyclerAdapter mAdapter;

    private List<String> mWaitingForMetaData;

    private String mLastRequestId;
    private boolean mReceivedFilesForRequest;
    private RelativeLayout mLayoutSearchInfo;
    private RelativeLayout mLayoutLoadingFiles;
    private LinearLayout mLayoutNoFilesFound;
    private SwipeRefreshLayout mLayoutRefresh;
    private FloatingActionMenu mFabFilterContext;

    private boolean mLongClicked = false;

    private ContextFilter mContextFilter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mReceivedFilesForRequest = false;
        mWaitingForMetaData = new ArrayList<>();
        mContextFilter = getFilterConfigFromSharedPref();

        View v = inflater.inflate(R.layout.fragment_search, container, false);
        setHasOptionsMenu(true);

        mLayoutSearchInfo = (RelativeLayout) v.findViewById(R.id.layoutSearchInfo);
        mLayoutLoadingFiles = (RelativeLayout) v.findViewById(R.id.layoutLoadingFiles);
        mLayoutNoFilesFound = (LinearLayout) v.findViewById(R.id.layoutNoFilesFound);

        mResults = new ArrayList<>();
        mRecyclerMatchedFiles = (RecyclerView) v.findViewById(R.id.recyclerMatchedFiles);
        mAdapter = new MatchedFilesRecyclerAdapter(getActivity(), mResults, this);
        mAdapter.setHasStableIds(true);
        mRecyclerMatchedFiles.setAdapter(mAdapter);
        mRecyclerMatchedFiles.setHasFixedSize(true);
        mRecyclerMatchedFiles.setItemViewCacheSize(20);
        mRecyclerMatchedFiles.setDrawingCacheEnabled(true);
        RecyclerView.LayoutManager layman = new GridLayoutManager(getActivity(), 3);
        mRecyclerMatchedFiles.setLayoutManager(layman);
        addRecyclerViewClickListener();
        mLayoutRefresh = (SwipeRefreshLayout) v.findViewById(R.id.layoutSwipeRefreshFiles);
        mLayoutRefresh.setColorSchemeColors(getResources().getColor(R.color.colorAccent));
        mLayoutRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                startFetchingFiles();
            }
        });

        mFabFilterContext = (FloatingActionMenu) v.findViewById(R.id.fabFilterContext);
        Drawable dFilter;
        Resources ctw = getActivity().getResources();
        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            //Get the drawables on all SDKs greater than API Level 23 (Android M, 6.0)
            dFilter = ctw.getDrawable(R.drawable.ic_filter_white_24dp, getActivity().getTheme());
        } else {
            //Get the drawables on all SDKs lower than API Level 23
            dFilter = VectorDrawableCompat.create(ctw, R.drawable.ic_filter_white_24dp, getActivity().getTheme());
        }
        mFabFilterContext.getMenuIconView().setImageDrawable(dFilter);
        mFabFilterContext.setOnMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFilterDialog();
            }
        });

        return v;
    }
    public static SearchFragment newInstance() {
        return new SearchFragment();
    }

    private void addRecyclerViewClickListener() {
        ItemClickSupport.addTo(mRecyclerMatchedFiles).setOnItemClickListener(
                new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                        if(!mLongClicked) {
                            //Show the loader
                            View loader = v.findViewById(R.id.loadingPanel);
                            if (loader != null) {
                                loader.setVisibility(View.VISIBLE);
                            }
                            ProgressBar bar = (ProgressBar) v.findViewById(R.id.progressCircular);
                            if (bar != null) {
                                bar.setProgress(0);
                            }
                            TextView percent = (TextView) v.findViewById(R.id.txtPercentDownload);
                            if (percent != null) {
                                percent.setText("0%");
                            }

                            //Start the download
                            VStore.getInstance().getFile(mResults.get(position).getUUID(), null);
                        } else {
                            mLongClicked = false;
                        }
                    }
                }
        );
        ItemClickSupport.addTo(mRecyclerMatchedFiles).setOnItemLongClickListener(
                new ItemClickSupport.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClicked(RecyclerView recyclerView, final int position, final View v) {
                        //Set a lock that the normal click does not trigger after release
                        mLongClicked = true;
                        final VStore vstor = VStore.getInstance();
                        //Check if this is a file I have shared
                        if(vstor.isMyFile(mResults.get(position).getUUID()))
                        {
                            //Display dialog to ask for deletion or meta information
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setMessage(R.string.what_to_do)
                                    .setPositiveButton(R.string.metadata, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            postNewMetadataEvent(mResults.get(position));
                                        }
                                    })
                                    .setNegativeButton(R.string.delete_file, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            View loader = v.findViewById(R.id.loadingPanel);
                                            if (loader != null) {
                                                loader.setVisibility(View.VISIBLE);
                                            }
                                            vstor.getFileManager().deleteFile(mResults.get(position).getUUID());
                                        }
                                    })
                                    .setNeutralButton(R.string.cancel, null)
                                    .show();
                        } else {
                            postNewMetadataEvent(mResults.get(position));
                        }
                        return false;
                    }
                }
        );
    }

    public void onStart(){
        super.onStart();
        //Register the event receivers in this fragment
        EventBus.getDefault().register(this);

        //Automatically fetch contextual files if WiFi is connected.
        //If the user is in mobile network, just show a button.
        if(Connectivity.isConnectedWifi(getContext())) {
            startFetchingFiles();
        } else {

        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        //Unregister the event receivers in this fragment
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.search_by_context_top_menu, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_refresh_search:
                startFetchingFiles();
                return true;
        }
        return false;
    }

    private void startFetchingFiles() {
        mReceivedFilesForRequest = false;
        mLastRequestId = UUID.randomUUID().toString();
        mResults.clear();
        mAdapter.notifyDataSetChanged();
        mRecyclerMatchedFiles.setVisibility(View.GONE);
        mLayoutSearchInfo.setVisibility(View.GONE);
        mLayoutNoFilesFound.setVisibility(View.GONE);
        mLayoutLoadingFiles.setVisibility(View.VISIBLE);

        VStore vstor = VStore.getInstance();
        boolean startedRequest = vstor.getFilesMatchingContext(
                vstor.getCurrentContext(),
                mContextFilter,
                mLastRequestId);
        if(!startedRequest) {
            Toast.makeText(getContext(), R.string.request_not_started, Toast.LENGTH_SHORT).show();
        }
    }

    private void showNoFilesInfo() {
        mLayoutNoFilesFound.setVisibility(View.VISIBLE);
        mRecyclerMatchedFiles.setVisibility(View.VISIBLE);
        mLayoutLoadingFiles.setVisibility(View.GONE);
        mLayoutSearchInfo.setVisibility(View.GONE);
    }

    private void showFilterDialog() {
        android.support.v4.app.FragmentManager fragmentManager = getFragmentManager();
        VSinglePlace mlp;
        //Only enable checkbox for most likely place if we have one.
        try
        {
            mlp = VStore.getInstance().getCurrentContext().getMostLikelyPlace();
        }
        catch(NullPointerException e)
        {
            mlp = null;
        }
        FilterDialog d = FilterDialog.newInstance(mContextFilter, mlp != null);
        d.show(fragmentManager, "filter_dialog");
        d.setTargetFragment(SearchFragment.this, REQUEST_EDIT_FILTER);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_EDIT_FILTER) {
            if (resultCode == OK_REQUEST) {
                if(data != null && data.hasExtra("filterconfig")) {
                    String filterStr = data.getStringExtra("filterconfig");
                    ContextFilter newFilterConf = new ContextFilter(filterStr);
                    SharedPreferences p = getActivity().getSharedPreferences("searchfilterconfig", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = p.edit();
                    editor.putString("filterconfig", filterStr);
                    editor.commit();
                    mContextFilter = newFilterConf;
                    startFetchingFiles();
                }
            }
        }
    }

    public ContextFilter getFilterConfigFromSharedPref() {
        SharedPreferences p = getActivity().getSharedPreferences("searchfilterconfig", Context.MODE_PRIVATE);
        if(p != null) {
            return new ContextFilter(p.getString("filterconfig", ContextFilter.getDefaultFilter().getJson().toString()));
        }
        return null;
    }

    private void postNewMetadataEvent(MatchingResultRow row) {
        mWaitingForMetaData.add(row.getUUID());
        MetadataEvent e = new MetadataEvent(row.getUUID(), row.getMetaData());
        EventBus.getDefault().postSticky(e);
    }


    /******** EVENTS **********/
    /**
     * Subscriber for the NewFilesMatchingContextEvent. Will be called once the framework received
     * a reply from a node containing new images matching the current usage context.
     * @param event The published event
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(NewFilesMatchingContextEvent event) {
        NewFilesMatchingContextEvent stickyEvent
                = EventBus.getDefault().getStickyEvent(NewFilesMatchingContextEvent.class);
        if (stickyEvent != null)
        {
            EventBus.getDefault().removeStickyEvent(stickyEvent);
            if(event.getCount() > 0)
            {
                mReceivedFilesForRequest = true;
                VStore vstor = VStore.getInstance();
                for (int i = 0; i<event.getCount(); i++)
                {
                    MatchingResultRow row = event.getResultRow(i);
                    int pos;
                    for (pos = 0; pos < mResults.size(); pos++) {
                        long dateList = mResults.get(pos).getMetaData().getCreationDate().getTime();
                        long dateNew = row.getMetaData().getCreationDate().getTime();
                        if (dateList <= dateNew) {
                            break;
                        }
                    }
                    mResults.add(pos, row);
                }
                mAdapter.notifyDataSetChanged();
                mRecyclerMatchedFiles.setVisibility(View.VISIBLE);
                mLayoutSearchInfo.setVisibility(View.GONE);
                mLayoutLoadingFiles.setVisibility(View.GONE);
                mLayoutNoFilesFound.setVisibility(View.GONE);
                /*Toast.makeText(
                        getContext(),
                        "Received " + event.getCount() + " files!",
                        Toast.LENGTH_SHORT).show();*/
            } else {
                if(!mReceivedFilesForRequest) {
                    showNoFilesInfo();
                }
            }
        }
        mLayoutRefresh.setRefreshing(false);
    }

    /**
     * Subscriber for the DownloadProgressEvent.
     * Will be called regularly while the download progresses.
     *
     * @param event The published event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DownloadProgressEvent event) {
        if (event == null) { return; }

        int pos = -1;
        for(int i = 0; i<mResults.size(); i++) {
            if(mResults.get(i).getUUID().equals(event.getFileUUID())) {
                pos = i;
                break;
            }
        }
        if(pos != -1)
        {
            if (event.getProgress() != 100)
            {
                try {
                    View loader = mRecyclerMatchedFiles.getChildAt(pos).findViewById(R.id.loadingPanel);
                    if (loader != null) {
                        loader.setVisibility(View.VISIBLE);
                        ProgressBar bar = (ProgressBar) loader.findViewById(R.id.progressCircular);
                        if (bar != null) {
                            bar.setProgress(event.getProgress());
                        }
                        TextView percent = (TextView) loader.findViewById(R.id.txtPercentDownload);
                        if (percent != null) {
                            percent.setText(event.getProgress() + "%");
                        }
                    }
                } catch (NullPointerException ignored) {
                }
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DownloadedFileReadyEvent event) {
        DownloadedFileReadyEvent stickyEvent
                = EventBus.getDefault().getStickyEvent(DownloadedFileReadyEvent.class);
        if (stickyEvent == null) return;

        //Find position
        int pos = -1;
        for(int i = 0; i<mResults.size(); i++) {
            if(mResults.get(i).getUUID().equals(event.file.getUuid()))
            {
                pos = i;
                break;
            }
        }
        if(pos == -1) return;

        //Hide the loader
        try
        {
            View loader = mRecyclerMatchedFiles.getChildAt(pos).findViewById(R.id.loadingPanel);
            if (loader != null) {
                loader.setVisibility(View.GONE);
            }
        }
        catch (NullPointerException ignored) { }
    }

    /**
     * Subscriber for the DownloadFailedEvent. Will be called if a file download failed.
     * @param event The published event
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DownloadFailedEvent event) {
        DownloadFailedEvent stickyEvent
                = EventBus.getDefault().getStickyEvent(DownloadFailedEvent.class);
        if (stickyEvent != null)
        {
            EventBus.getDefault().removeStickyEvent(stickyEvent);
            try
            {
                int pos = -1;
                for(int i = 0; i<mResults.size(); i++)
                {
                    if(mResults.get(i).getUUID().equals(event.getFileUUID()))
                    {
                        pos = i;
                        break;
                    }
                }
                if(pos != -1)
                {
                    View loader = mRecyclerMatchedFiles.getChildAt(pos).findViewById(R.id.loadingPanel);
                    if (loader != null)
                    {
                        loader.setVisibility(View.GONE);
                    }
                }
            } catch(NullPointerException ignored) {}
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(FileDeletedEvent evt) {
        //Find position of the element
        int index = -1;
        for(int i = 0; i<mResults.size(); i++) {
            if(mResults.get(i).getUUID().equals(evt.getFileUUID()))
            {
                index = i;
                break;
            }
        }
        if(index != -1)
        {
            View child = mRecyclerMatchedFiles.getChildAt(index);
            if(child != null)
            {
                View loader = child.findViewById(R.id.loadingPanel);
                if(loader != null)
                {
                    loader.setVisibility(View.GONE);
                }
            }
            mResults.remove(index);
            mAdapter.notifyDataSetChanged();
        }
        if(mResults.size() == 0)
        {
            showNoFilesInfo();
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MetadataEvent evt) {
        MetadataEvent stickyEvent
                = EventBus.getDefault().getStickyEvent(MetadataEvent.class);
        if (stickyEvent != null)
        {
            EventBus.getDefault().removeStickyEvent(stickyEvent);
            if(!mWaitingForMetaData.contains(evt.getFileUUID())) { return; }
            mWaitingForMetaData.remove(evt.getFileUUID());
            //Find position of the element
            int index = -1;
            for(int i = 0; i<mResults.size(); i++)
            {
                if(mResults.get(i).getUUID().equals(evt.getFileUUID()))
                {
                    index = i;
                    break;
                }
            }
            if(index != -1) {
                View loader = mRecyclerMatchedFiles
                        .getChildAt(index)
                        .findViewById(R.id.loadingPanel);
                if (loader != null)
                {
                    loader.setVisibility(View.GONE);
                }
                FragmentManager fm = getActivity().getFragmentManager();
                MetadataFragment d = MetadataFragment.newInstance(evt.getMetadata());
                d.show(fm, "metadata_dialog");
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ThumbnailDownloadFailedEvent evt) {
        EventBus.getDefault().removeStickyEvent(evt);
        Toast.makeText(getActivity(), "Failed to download thumbnail", Toast.LENGTH_SHORT).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(NewThumbnailEvent evt) {
        EventBus.getDefault().removeStickyEvent(evt);
        //Find position of the element
        int index = -1;
        for(int i = 0; i<mResults.size(); i++)
        {
            if(mResults.get(i).getUUID().equals(evt.getFileId()))
            {
                index = i;
                break;
            }
        }
        if(index != -1) {
            ImageView thumb = mRecyclerMatchedFiles
                    .getChildAt(index)
                    .findViewById(R.id.thumbnailView);

            Bitmap bitmap = BitmapFactory.decodeFile(evt.getImageFile().getAbsolutePath());
            thumb.setImageBitmap(bitmap);
        }
    }
}
