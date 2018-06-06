package vstore.android_filebox.files_fragment;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
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
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.mikepenz.actionitembadge.library.ActionItemBadge;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;
import vstore.android_filebox.ItemClickSupport;
import vstore.android_filebox.R;
import vstore.android_filebox.utils.ContactParser;
import vstore.android_filebox.utils.FileUtils;
import vstore.android_filebox.utils.StringUtils;
import vstore.framework.VStore;
import vstore.framework.communication.upload.events.AllUploadsDoneEvent;
import vstore.framework.communication.upload.events.UploadDoneEvent;
import vstore.framework.communication.upload.events.UploadFailedEvent;
import vstore.framework.communication.upload.events.UploadFailedPermanentlyEvent;
import vstore.framework.db.DBResultOrdering;
import vstore.framework.error.ErrorCode;
import vstore.framework.exceptions.StoreException;
import vstore.framework.file.VFileType;
import vstore.framework.file.VStoreFile;
import vstore.framework.file.events.FileDeletedEvent;
import vstore.framework.file.events.FilesReadyEvent;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeManager;
import vstore.framework.utils.IdentifierUtils;

import static android.app.Activity.RESULT_OK;

public class MyFilesFragment extends Fragment {
    /*private LinearLayout mProgressLayout;
    private TextView mTxtUploadTitle;
    private ProgressBar mUploadProgressBar;
    private TextView mTxtPercentage;
    Runnable mHideRunnable;
    Handler mHandler;*/
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    private TextView mTxtNoFilesSaved;
    private RecyclerView mRvMyFiles;

    //Buttons for public sharing
    private FloatingActionMenu mButtonMenuPublic;
    private FloatingActionButton mBtnContactPublic;
    private FloatingActionButton mBtnDocumentPublic;
    private FloatingActionButton mBtnVideoPublic;
    private FloatingActionButton mBtnPhotoPublic;
    private FloatingActionButton mBtnSelectPublic;

    //Buttons for saving in private mode
    private FloatingActionMenu mButtonMenuPrivate;
    private FloatingActionButton mBtnContactPrivate;
    private FloatingActionButton mBtnDocumentPrivate;
    private FloatingActionButton mBtnVideoPrivate;
    private FloatingActionButton mBtnPhotoPrivate;
    private FloatingActionButton mBtnSelectPrivate;

    private SwipeRefreshLayout mLayoutSwipeRefreshMyFiles;

    private File mCurrentTmpFile;
    static final int REQUEST_TAKE_PHOTO = 1;
    private Bitmap mCurrentThumbnail;
    private boolean mCurrentPrivate = false;

    static final int REQUEST_SELECT_FILE = 2;
    static final int REQUEST_TAKE_VIDEO = 3;
    static final int REQUEST_SELECT_CONTACT = 4;
    static final int REQUEST_SELECT_DOCUMENT = 5;

    private ListWithDateHeaders<Object> mFiles;
    private FilesRecyclerAdapter mAdapter;
    private boolean mOnlyPrivate = false;
    private boolean mNewestFirst = true;
    private boolean mShowingOnlyPending = false;
    private int mPendingUploadCount = 0;

    private boolean mLongClicked = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_my_files, container, false);
        setHasOptionsMenu(true);
        /*mProgressLayout = (LinearLayout) v.findViewById(R.id.progressLayout);
        mTxtUploadTitle = (TextView) v.findViewById(R.id.txtUploadTitle);
        mUploadProgressBar = (ProgressBar) v.findViewById(R.id.uploadProgressBar);
        mTxtPercentage = (TextView) v.findViewById(R.id.txtPercentage);*/
        mTxtNoFilesSaved = (TextView) v.findViewById(R.id.txtNoFilesSaved);
        mRvMyFiles = (RecyclerView) v.findViewById(R.id.rvMyFiles);

        mButtonMenuPublic = (FloatingActionMenu) v.findViewById(R.id.fabMenuPublic);
        mBtnContactPublic = (FloatingActionButton) v.findViewById(R.id.btnContactPublic);
        mBtnDocumentPublic = (FloatingActionButton) v.findViewById(R.id.btnDocumentPublic);
        mBtnVideoPublic = (FloatingActionButton) v.findViewById(R.id.btnVideoPublic);
        mBtnPhotoPublic = (FloatingActionButton) v.findViewById(R.id.btnPhotoPublic);
        mBtnSelectPublic = (FloatingActionButton) v.findViewById(R.id.btnSelectPublic);
        mButtonMenuPrivate = (FloatingActionMenu) v.findViewById(R.id.fabMenuPrivate);
        mBtnContactPrivate = (FloatingActionButton) v.findViewById(R.id.btnContactPrivate);
        mBtnDocumentPrivate = (FloatingActionButton) v.findViewById(R.id.btnDocumentPrivate);
        mBtnVideoPrivate = (FloatingActionButton) v.findViewById(R.id.btnVideoPrivate);
        mBtnPhotoPrivate = (FloatingActionButton) v.findViewById(R.id.btnPhotoPrivate);
        mBtnSelectPrivate = (FloatingActionButton) v.findViewById(R.id.btnSelectPrivate);

        mFiles = new ListWithDateHeaders<>(true);
        mAdapter = new FilesRecyclerAdapter(getActivity(), mFiles);
        mRvMyFiles.setAdapter(mAdapter);
        mRvMyFiles.setHasFixedSize(true);
        mRvMyFiles.setItemViewCacheSize(20);
        mRvMyFiles.setDrawingCacheEnabled(true);
        mRvMyFiles.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        GridLayoutManager manager = new GridLayoutManager(getActivity(), 3);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch(mAdapter.getItemViewType(position)){
                    case FilesRecyclerAdapter.DATE:
                        return 3;
                    case FilesRecyclerAdapter.FILE:
                        return 1;
                    default:
                        return -1;
                }
            }
        });
        mRvMyFiles.setLayoutManager(manager);
        mRvMyFiles.setItemAnimator(new SlideInUpAnimator());

        addClickListeners();
        addRecyclerViewClickListeners();
        mButtonMenuPublic.setIconAnimated(false);
        mButtonMenuPrivate.setIconAnimated(false);

        setButtonImages();

        mLayoutSwipeRefreshMyFiles = (SwipeRefreshLayout) v.findViewById(R.id.layoutSwipeRefreshMyFiles);
        mLayoutSwipeRefreshMyFiles.setColorSchemeColors(getResources().getColor(R.color.colorAccent));
        mLayoutSwipeRefreshMyFiles.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestFiles();
            }
        });

        return v;
    }
    public static MyFilesFragment newInstance() {
        return new MyFilesFragment();
    }

    private void setButtonImages() {
        Drawable dShare, dPrivate, dSelect, dPhoto, dVideo, dDocument, dContact;

        Resources ctw = getActivity().getResources();
        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            //Get the drawables on all SDKs greater than API Level 23 (Android M, 6.0)
            dShare = ctw.getDrawable(R.drawable.ic_share_white_24dp, getActivity().getTheme());
            dPrivate = ctw.getDrawable(R.drawable.ic_security_white_24dp, getActivity().getTheme());
            dSelect = ctw.getDrawable(R.drawable.ic_more_horiz_black_24dp, getActivity().getTheme());
            dPhoto = ctw.getDrawable(R.drawable.ic_cam_black_24dp, getActivity().getTheme());
            dVideo = ctw.getDrawable(R.drawable.ic_videocam_black_24dp, getActivity().getTheme());
            dDocument = ctw.getDrawable(R.drawable.ic_doc_black_24dp, getActivity().getTheme());
            dContact = ctw.getDrawable(R.drawable.ic_contact_black_24dp, getActivity().getTheme());
        } else {
            //Get the drawables on all SDKs lower than API Level 23
            dShare = VectorDrawableCompat.create(ctw, R.drawable.ic_share_white_24dp, getActivity().getTheme());
            dPrivate = VectorDrawableCompat.create(ctw, R.drawable.ic_security_white_24dp, getActivity().getTheme());
            dSelect = VectorDrawableCompat.create(ctw, R.drawable.ic_more_horiz_black_24dp, getActivity().getTheme());
            dPhoto = VectorDrawableCompat.create(ctw, R.drawable.ic_cam_black_24dp, getActivity().getTheme());
            dVideo = VectorDrawableCompat.create(ctw, R.drawable.ic_videocam_black_24dp, getActivity().getTheme());
            dDocument = VectorDrawableCompat.create(ctw, R.drawable.ic_doc_black_24dp, getActivity().getTheme());
            dContact = VectorDrawableCompat.create(ctw, R.drawable.ic_contact_black_24dp, getActivity().getTheme());
        }
        //Set the drawables for the buttons
        mButtonMenuPrivate.getMenuIconView().setImageDrawable(dPrivate);
        mButtonMenuPublic.getMenuIconView().setImageDrawable(dShare);
        mBtnSelectPrivate.setImageDrawable(dSelect);
        mBtnSelectPublic.setImageDrawable(dSelect);
        mBtnPhotoPrivate.setImageDrawable(dPhoto);
        mBtnPhotoPublic.setImageDrawable(dPhoto);
        mBtnVideoPrivate.setImageDrawable(dVideo);
        mBtnVideoPublic.setImageDrawable(dVideo);
        mBtnDocumentPrivate.setImageDrawable(dDocument);
        mBtnDocumentPublic.setImageDrawable(dDocument);
        mBtnContactPrivate.setImageDrawable(dContact);
        mBtnContactPublic.setImageDrawable(dContact);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.my_files_top_menu, menu);
        super.onCreateOptionsMenu(menu,inflater);

        if (mPendingUploadCount > 0) {
            ActionItemBadge.update(getActivity(), menu.findItem(R.id.item_pending),
                    GoogleMaterial.Icon.gmd_file_upload,
                    ActionItemBadge.BadgeStyles.RED,
                    mPendingUploadCount);
        } else {
            ActionItemBadge.hide(menu.findItem(R.id.item_pending));
        }
        if(mNewestFirst) {
            menu.findItem(R.id.action_descending).setChecked(true);
        } else {
            menu.findItem(R.id.action_descending).setChecked(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_filter:
                return true;

            case R.id.action_ascending:
                item.setChecked(true);
                mShowingOnlyPending = false;
                mNewestFirst = false;
                requestFiles();
                return true;

            case R.id.action_descending:
                item.setChecked(true);
                mShowingOnlyPending = false;
                mNewestFirst = true;
                requestFiles();
                return true;

            case R.id.action_only_private:
                item.setChecked(true);
                mOnlyPrivate = true;
                mShowingOnlyPending = false;
                requestFiles();
                return true;

            case R.id.action_only_public:
                item.setChecked(true);
                mOnlyPrivate = false;
                mShowingOnlyPending = false;
                requestFiles();
                return true;

            case R.id.action_only_pending:
                mShowingOnlyPending = true;
                mOnlyPrivate = false;
                requestFiles();
                break;
        }
        return false;
    }

    private void addClickListeners() {
        //Public buttons listeners
        mBtnContactPublic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurrentPrivate = false;
                dispatchSelectContactIntent();
                closeButtonMenu();
            }
        });
        mBtnDocumentPublic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurrentPrivate = false;
                dispatchSelectDocumentIntent();
                closeButtonMenu();
            }
        });
        mBtnVideoPublic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurrentPrivate = false;
                dispatchTakeVideoIntent();
                closeButtonMenu();
            }
        });
        mBtnPhotoPublic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurrentPrivate = false;
                dispatchTakePictureIntent();
                closeButtonMenu();
            }
        });
        mBtnSelectPublic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurrentPrivate = false;
                dispatchSelectFileIntent();
                closeButtonMenu();
            }
        });
        mBtnContactPrivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurrentPrivate = true;
                dispatchSelectContactIntent();
                closeButtonMenu();
            }
        });
        mBtnDocumentPrivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurrentPrivate = true;
                dispatchSelectDocumentIntent();
                closeButtonMenu();
            }
        });
        //Private buttons listeners
        mBtnVideoPrivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurrentPrivate = true;
                dispatchTakeVideoIntent();
                closeButtonMenu();
            }
        });
        //Private buttons listeners
        mBtnPhotoPrivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurrentPrivate = true;
                dispatchTakePictureIntent();
                closeButtonMenu();
            }
        });
        mBtnSelectPrivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurrentPrivate = true;
                dispatchSelectFileIntent();
                closeButtonMenu();
            }
        });
    }

    private void addRecyclerViewClickListeners() {
        ItemClickSupport.addTo(mRvMyFiles).setOnItemClickListener(
                new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                        if(!mLongClicked) {
                            if (position < mFiles.size() && mFiles.get(position) instanceof VStoreFile) {
                                VStoreFile f = (VStoreFile) mFiles.get(position);
                                FileUtils.openFile(
                                        getContext(),
                                        FileUtils.getContentUriForFile(getContext(), f),
                                        f.getFileType());
                            }
                        } else {
                            mLongClicked = false;
                        }
                    }
                }
        );
        ItemClickSupport.addTo(mRvMyFiles).setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, final int position, View v) {
                //Set a lock that the normal click does not trigger after release
                mLongClicked = true;
                //Display dialog to ask for deletion
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(R.string.delete_file_ask)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(mFiles.get(position) instanceof VStoreFile) {
                                    String uuidToDelete = ((VStoreFile)mFiles.get(position)).getUuid();
                                    VStore.getInstance().getFileManager().deleteFile(uuidToDelete);
                                    mFiles.remove(position);
                                    requestFiles();
                                }
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                return false;
            }
        });
    }
    private void closeButtonMenu() {
        mButtonMenuPublic.close(true);
        mButtonMenuPrivate.close(true);
    }

    private void requestFiles() {
        DBResultOrdering ordering;
        if(mNewestFirst) {
            ordering = DBResultOrdering.NEWEST_FIRST;
        } else {
            ordering = DBResultOrdering.OLDEST_FIRST;
        }
        VStore.getInstance().getFilesUploadedByThisDevice(ordering, mShowingOnlyPending, mOnlyPrivate);
    }

    /**
     * This method creates an intent and starts the camera app to take a photo.
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Check if a camera app is available
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File photoFile = createTmpFile("jpg");
            if (photoFile != null) {
                //Use "content://" uri because "file://" will crash on newer Android when
                //sharing it with other apps
                Uri photoURI = FileProvider.getUriForFile(getActivity(),
                        vstore.android_filebox.FileProvider.FILE_PROVIDER_AUTHORITY,
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                grantPermissionsToFileProvider(takePictureIntent, photoURI);

                try {
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                } catch(ActivityNotFoundException e) {
                    Toast.makeText(getActivity(), "Error: No camera app available.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            // Error occurred: No camera app available
            Toast.makeText(getActivity(), "Error: No camera app available.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * This method creates an intent and starts the camera app to take a video.
     */
    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        // Check if a video app is available
        if (takeVideoIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            try {
                startActivityForResult(takeVideoIntent, REQUEST_TAKE_VIDEO);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getActivity(), "Error: No video camera app available.",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Error occurred: No app available that can take videos
            Toast.makeText(getActivity(), "Error: No video camera app available.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchSelectContactIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && getActivity().checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
            //After this point: Waiting for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        }
        else {
            // Android version < 6.0 or Permission is already granted.
            Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                    ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(contactPickerIntent, REQUEST_SELECT_CONTACT);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                dispatchSelectContactIntent();
            } else {
                Toast.makeText(getActivity(),
                        "Permission necessary to read contacts!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void dispatchSelectDocumentIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, VFileType.DOC_TYPES.toArray());
        Intent chooserIntent = Intent.createChooser(intent, "Select an app you want to import a document from:");
        startActivityForResult(chooserIntent, REQUEST_SELECT_DOCUMENT);
    }

    /**
     * This methods creates an intent to open a file chooser.
     */
    private void dispatchSelectFileIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        //String[] mimetypes = {IMAGE_JPG, VIDEO_MP4, DOC_PDF};
        //intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        Intent chooserIntent = Intent.createChooser(intent, "Select an app you want to import from:");
        startActivityForResult(chooserIntent, REQUEST_SELECT_FILE);
    }

    /**
     * Creates a temporary file with the given file extension (e.g. "jpg")
     * @param extension The file extension to append to the temporary file
     * @return A File object containing the path to the temp file on disk
     */
    private File createTmpFile(String extension) {
        if(extension.equals("")) {
            extension = "vtmp";
        }
        try {
            // Create collision-resistant tmp file name
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMAN).format(new Date());
            String tmpFileName = "VSTOR_" + timestamp + "_";
            //Create tmp_files directory if necessary
            File storageDir = new File(getActivity().getExternalCacheDir(), "tmp_files");
            storageDir.mkdirs();
            //Create the temporary file
            mCurrentTmpFile = File.createTempFile(tmpFileName, "."+extension, storageDir);
        } catch (IOException ex) {
            // Error occurred while creating the tmp file
            Toast.makeText(getActivity(), "Error: Temporary file could not be created.",
                    Toast.LENGTH_SHORT).show();
        }
        return mCurrentTmpFile;
    }

    /**
     * Needed for older Android versions: There you have to explicitly give permissions to the
     * called app to access the file provider.
     */
    private void grantPermissionsToFileProvider(Intent intent, Uri fileUri) {
        //For older Android devices:
        //Loop through supported apps and grant them permission to access the file provider
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            List<ResolveInfo> resInfoList = getActivity().getPackageManager()
                    .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                getActivity().grantUriPermission(
                        packageName,
                        fileUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            VStore v = VStore.getInstance();
            VStoreFile insertedFile = null;
            try {
                //Code for each case is pretty similar, but is left separately to enable
                //later changes to the different "pre-processing" per file type before saving.
                String path = "";
                switch(requestCode)
                {
                    case REQUEST_TAKE_PHOTO:
                        //Camera saved photo in the temporary file, so get it and check if it is available
                        if (!mCurrentTmpFile.exists()) { break; }
                        Uri imgUri = Uri.fromFile(mCurrentTmpFile);
                        if(imgUri.getScheme().contains("content"))
                            path = FileUtils.getPathFromContentUri(getActivity(), imgUri,
                                    FileUtils.getNameFromContentProvider(getActivity(), imgUri, imgUri.getLastPathSegment()));
                        else
                            path = imgUri.getPath();
                        insertedFile = v.store(path, mCurrentPrivate);
                        break;

                    case REQUEST_TAKE_VIDEO:
                        Uri videoUri = intent.getData();
                        if(videoUri == null) { break; }
                        if(videoUri.getScheme().contains("content")) {
                            path = FileUtils.getPathFromContentUri(getActivity(), videoUri,
                                    FileUtils.getNameFromContentProvider(getActivity(), videoUri, videoUri.getLastPathSegment()));
                        } else {
                            path = videoUri.getPath();
                        }
                        insertedFile = v.store(path, mCurrentPrivate);
                        break;

                    case REQUEST_SELECT_FILE:
                        Uri fileUri = intent.getData();
                        if(fileUri == null) { break; }
                        if(fileUri.getScheme().contains("content")) {
                            path = FileUtils.getPathFromContentUri(getActivity(), fileUri,
                                    FileUtils.getNameFromContentProvider(getActivity(), fileUri, fileUri.getLastPathSegment()));
                        } else {
                            path = fileUri.getPath();
                        }
                        insertedFile = v.store(path, mCurrentPrivate);
                        break;

                    case REQUEST_SELECT_CONTACT:
                        Uri contactUri = intent.getData();
                        if(contactUri == null) { break; }
                        //Create and get contact data file
                        ContactParser cParser = new ContactParser();
                        File fCopied = cParser.getAndWriteContactData(
                                getActivity(),
                                contactUri,
                                VStore.getInstance().getFileManager().getStoredFilesDir(),
                                "tmpContact.vcf");

                        insertedFile = v.store(fCopied.getAbsolutePath(), mCurrentPrivate);
                        break;

                    case REQUEST_SELECT_DOCUMENT:
                        Uri docUri = intent.getData();
                        if(docUri == null) { break; }
                        if(docUri.getScheme().contains("content")) {
                            path = FileUtils.getPathFromContentUri(getActivity(), docUri,
                                    FileUtils.getNameFromContentProvider(getActivity(), docUri, docUri.getLastPathSegment()));
                        } else {
                            path = docUri.getPath();
                        }
                        insertedFile = v.store(path, mCurrentPrivate);
                        break;
                }
                String nodeid = insertedFile.getMainNodeId();
                NodeManager mgr = VStore.getInstance().getNodeManager();
                NodeInfo n = mgr.getNode(nodeid);
                String str;
                if(n == null)
                {
                    str = "No matching storage node found.";
                }
                else
                {
                    if(insertedFile.getStoredNodeIds().size() > 1)
                    {
                        str = "Stored on multiple nodes.";
                    } else {
                        str = "Stored on " + StringUtils.capitalizeOnlyFirstLetter(n.getNodeType().name());
                    }
                }
                Toast.makeText(getContext(), str, Toast.LENGTH_LONG).show();
            }
            catch (StoreException e)
            {
                if (e.getErrCode().equals(ErrorCode.FILE_ALREADY_EXISTS))
                {
                    //Display a message to tell the user that the file type is not supported
                    Toast.makeText(getActivity(),
                            "Error: File already exists in the framework!",
                            Toast.LENGTH_LONG)
                            .show();
                }
                else
                {
                    e.printStackTrace();
                    Toast.makeText(
                            this.getActivity(),
                            "Store failed: " + e.getMessage(),
                            Toast.LENGTH_LONG)
                            .show();
                }
            }
            //If storing in framework was successful, reflect this in UI by updating all
            //necessary stuff
            if(insertedFile != null)
            {
                mPendingUploadCount++;
                mFiles.addElement(insertedFile);
                mAdapter.notifyItemRangeInserted(0, 1);
                mRvMyFiles.setVisibility(View.VISIBLE);
                mTxtNoFilesSaved.setVisibility(View.GONE);
                getActivity().invalidateOptionsMenu();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        //Register the event receivers in this class
        EventBus.getDefault().register(this);
        requestFiles();
        mPendingUploadCount = VStore.getInstance().getCommunicationManager().getPendingUploadCount();
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        //Unregister the event receivers in this class
        EventBus.getDefault().unregister(this);
    }

    /* ******* EVENTS RECEIVED FROM THE FRAMEWORK ******* */
    /**
     * Subscriber for the AllUploadsDoneEvent. Will be called once all uploads are done.
     * @param event The published event
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AllUploadsDoneEvent event) {
        AllUploadsDoneEvent stickyEvent = EventBus.getDefault().getStickyEvent(AllUploadsDoneEvent.class);
        if(stickyEvent != null) {
            EventBus.getDefault().removeStickyEvent(stickyEvent);
            //mHandler.postDelayed(mHideRunnable, 2*1000);
            //mTxtUploadTitle.setText(getString(R.string.all_uploads_done));
            mPendingUploadCount = 0;
            getActivity().invalidateOptionsMenu();
        }
    }

    /**
     * Subscriber for the UploadDoneEvent. Will be called once the UploadManagerJob has
     * finished uploading a file.
     * @param event The published event
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UploadDoneEvent event) {
        UploadDoneEvent theEvt = EventBus.getDefault().removeStickyEvent(UploadDoneEvent.class);
        if(theEvt == null) {
            if(event == null) return;
            theEvt = event;
        }
        for(int i = 0; i<mFiles.size(); i++)
        {
            if(mFiles.get(i) instanceof VStoreFile)
            {
                VStoreFile f = ((VStoreFile)mFiles.get(i));
                //Find the correct item in the model data
                if(f.getUuid().equals(theEvt.getFileId())) {
                    f.setUploadPending(false);
                    //Notify adapter that layout for this item has changed
                    mAdapter.notifyItemChanged(i);
                    mRvMyFiles.scrollToPosition(0);
                    break;
                }
            }
        }
        mPendingUploadCount--;
        if(mPendingUploadCount < 0) mPendingUploadCount = 0;
        getActivity().invalidateOptionsMenu();
    }

    /**
     * Subscriber for the UploadFailedEvent. Will be called if an upload failed.
     * @param event The published event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UploadFailedEvent event) {
        Toast.makeText(getActivity(), "Upload failed. Retry in " + event.getRetryTime() + "s.",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Subscriber for the FilesReadyEvent. Will be called once the framework has finished
     * retrieving the files.
     * @param event The published event
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(FilesReadyEvent event) {
        if(event.isOnlyPendingFiles()) {
            mPendingUploadCount = event.getFiles().size();
            getActivity().invalidateOptionsMenu();
        }
        if(!mShowingOnlyPending || event.isOnlyPendingFiles() && mShowingOnlyPending) {
            if (event.getFiles() != null && event.getFiles().size() > 0) {
                mFiles.clear();
                mFiles.setSortDirection(mNewestFirst);
                mFiles.addElements(event.getFiles());
                mRvMyFiles.setVisibility(View.VISIBLE);
                mTxtNoFilesSaved.setVisibility(View.GONE);
            } else {
                mRvMyFiles.setVisibility(View.GONE);
                mTxtNoFilesSaved.setVisibility(View.VISIBLE);
                if(mShowingOnlyPending) {
                    mTxtNoFilesSaved.setText(R.string.no_uploads_pending);
                } else {
                    mTxtNoFilesSaved.setText(R.string.no_files_available);
                }
            }
            mAdapter.notifyDataSetChanged();
        }
        FilesReadyEvent stickyEvent = EventBus.getDefault().getStickyEvent(FilesReadyEvent.class);
        if (stickyEvent != null) {
            EventBus.getDefault().removeStickyEvent(stickyEvent);
        }
        mLayoutSwipeRefreshMyFiles.setRefreshing(false);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UploadFailedPermanentlyEvent event) {
        UploadFailedPermanentlyEvent stickyEvent
                = EventBus.getDefault().getStickyEvent(UploadFailedPermanentlyEvent.class);
        if (stickyEvent == null) { return; }
        EventBus.getDefault().removeStickyEvent(stickyEvent);
        Toast.makeText(getActivity(), event.getErrorMsg(), Toast.LENGTH_LONG).show();
        mPendingUploadCount--;
        if(mPendingUploadCount < 0) mPendingUploadCount = 0;
        getActivity().invalidateOptionsMenu();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(FileDeletedEvent evt) {
        requestFiles();
    }
}
