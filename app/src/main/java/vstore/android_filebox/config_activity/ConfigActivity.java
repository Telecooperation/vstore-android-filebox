package vstore.android_filebox.config_activity;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import vstore.android_filebox.MainActivity;
import vstore.android_filebox.R;
import vstore.android_filebox.aware.AwareController;
import vstore.android_filebox.events.PermissionsDeniedEvent;
import vstore.android_filebox.events.PermissionsGrantedEvent;
import vstore.android_filebox.qrcode.IntentIntegrator;
import vstore.android_filebox.qrcode.IntentResult;
import vstore.android_filebox.qrcode.QRCodeHandler;
import vstore.framework.VStore;
import vstore.framework.config.events.ConfigDownloadSucceededEvent;
import vstore.framework.context.types.location.VLatLng;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeManager;
import vstore.framework.node.NodeType;

public class ConfigActivity extends AppCompatActivity implements EditNodeDialog.EditNodeResult {
    public static final int REQUEST_EDIT_NODE = 0;
    public static final int REQUEST_ADD_NODE = 1;
    public static final int MY_PERMISSIONS_REQUEST = 1234;

    private ListView mListviewNodeList;
    ListAdapter mNodeListAdapter;

    private List<NodeInfo> mNodeInfoItems;
    private int mPositionClicked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Check if activity was opened just for permission requests
        Bundle b = getIntent().getExtras();
        if(b != null && b.getBoolean(Constants.WAS_STARTED_FOR_PERMISSION_REQUESTS)) {
            checkPermissions();
        } else {
            setContentView(R.layout.activity_config);
            Toolbar myToolbar = (Toolbar) findViewById(R.id.config_toolbar);
            myToolbar.setTitleTextColor(0xFFFFFFFF);
            setSupportActionBar(myToolbar);
            setTitle("vStore Configuration");

            ((TextView) findViewById(R.id.txtDeviceIdentifier)).setText(VStore.getDeviceIdentifier());

            Button btnScanQR = (Button) findViewById(R.id.btnScanQR);
            btnScanQR.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    QRCodeHandler.scanQRCode(ConfigActivity.this);
                }
            });

            Button btnReloadConfig = (Button) findViewById(R.id.btnReloadConfig);
            btnReloadConfig.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(
                            getApplicationContext(),
                            "Downloading configuration file...",
                            Toast.LENGTH_SHORT)
                            .show();
                    try {
                        VStore.getInstance().getConfigManager().download(false);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });

            Button btnAddStorageNode = (Button) findViewById(R.id.btnAddStorageNode);
            btnAddStorageNode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showNodeDialog(REQUEST_ADD_NODE, false, null);
                }
            });

            Button btnConfigureNodes = (Button) findViewById(R.id.btnConfigureNodes);
            btnConfigureNodes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mNodeInfoItems.size() > 0) {
                        if (mListviewNodeList.getVisibility() == View.VISIBLE) {
                            mListviewNodeList.setVisibility(View.GONE);
                        } else {
                            mListviewNodeList.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(ConfigActivity.this,
                                R.string.no_nodes_available,
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            });

            mListviewNodeList = (ListView) findViewById(R.id.listviewNodeList);
            mNodeInfoItems = loadNodeList();
            mNodeListAdapter = new NodeListAdapter(this, R.layout.nodeinfo_list_row, mNodeInfoItems);
            mListviewNodeList.setAdapter(mNodeListAdapter);
            mListviewNodeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                    long viewId = view.getId();
                    if (viewId == R.id.btnDeleteNode) {
                        //Display dialog to ask for deletion
                        AlertDialog.Builder builder = new AlertDialog.Builder(ConfigActivity.this);
                        builder.setMessage(R.string.node_ask_delete)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int x) {
                                        deleteNode(mNodeInfoItems.remove(i));
                                        Toast.makeText(ConfigActivity.this,
                                                R.string.node_has_deleted,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton(R.string.cancel, null).show();
                        mListviewNodeList.invalidateViews();
                    } else if (viewId == R.id.btnEditNodeInfo) {
                        if(i < mNodeInfoItems.size()) {
                            mPositionClicked = i;
                            showNodeDialog(REQUEST_EDIT_NODE, true, mNodeInfoItems.get(i));
                        }
                    }
                }
            });
            Button btnDeleteCurrentContext = (Button) findViewById(R.id.btnDeleteCurrentContext);
            btnDeleteCurrentContext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Display dialog to ask for deletion of current context
                    AlertDialog.Builder builder = new AlertDialog.Builder(ConfigActivity.this);
                    builder.setMessage(R.string.ask_delete_context)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int x) {
                                    VStore.getInstance().clearCurrentContext();
                                    Toast.makeText(ConfigActivity.this,
                                            R.string.context_deleted,
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(R.string.cancel, null).show();
                }
            });
            //Load the default dB threshold from the config
            EditText inputDbThresh = (EditText) findViewById(R.id.inputDefaultDBThreshold);
            inputDbThresh.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
                @Override
                public void afterTextChanged(Editable editable) {
                    /*try {
                        VStoreConfig.setDefaultDBThresh(getApplicationContext(),
                                Integer.parseInt(editable.toString()));
                    } catch(NumberFormatException e) { }*/
                }
            });
            //TODO
            //inputDbThresh.setText(""+mConfig.getDefaultDBThreshold());

            //Load the context refresh time from the config
            EditText inputRefreshTime = (EditText) findViewById(R.id.inputRefreshTime);
            inputRefreshTime.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
                @Override
                public void afterTextChanged(Editable editable) {
                    /*try {
                        mConfig.setContextRefreshTime(getApplicationContext(),
                                Integer.parseInt(editable.toString()));
                    } catch(NumberFormatException e) {}*/
                    //TODO
                }
            });
            //inputRefreshTime.setText(""+mConfig.getContextRefreshTime());

            //Load the multiple nodes setting from the config
            Switch switchMultipleNodes = (Switch) findViewById(R.id.switchMultipleNodes);
            switchMultipleNodes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    //mConfig.setIsMultipleNodesAllowed(getApplicationContext(), b);
                }
            });
            //switchMultipleNodes.setChecked(mConfig.isMultipleNodesAllowed());
            //TODO

            //Make it possible to scroll listview in scrollview
            ((ScrollView) findViewById(R.id.scrollConfig)).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    findViewById(R.id.listviewNodeList).getParent()
                            .requestDisallowInterceptTouchEvent(false);
                    return false;
                }
            });
            mListviewNodeList.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // Disallow the touch request for parent scroll on touch of child view
                    ((ScrollView) findViewById(R.id.scrollConfig)).requestDisallowInterceptTouchEvent(true);
                    return false;
                }
            });
        }
    }

    /**
     * This methods shows a dialog for creating a new node or editing node settings.
     * @param edit True, if a node should be edited. False, if a new one should be created.
     * @param node The node that should be edited, if the edit parameter is set to true.
     */
    public void showNodeDialog(int requestCode, boolean edit, NodeInfo node) {
        //Remove the dialog fragment (edit window), if it is already on the backstack
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment f = getSupportFragmentManager().findFragmentByTag("nodedialog");
        if (f != null) {
            ft.remove(f);
        }
        ft.addToBackStack(null);

        String title;
        //Set corresponding title for the dialog fragment
        if(edit) {
            title = getString(R.string.edit_node);
        } else {
            title = getString(R.string.create_new_node);
        }
        // Create and show the dialog fragment
        EditNodeDialog newFragment = EditNodeDialog.newInstance(requestCode, title, node);
        if(newFragment != null) {
            newFragment.show(ft, "nodedialog");
        }
    }

    /**
     * This method deletes a storage node from the database.
     * @param node The node to delete.
     */
    public void deleteNode(NodeInfo node) {
        NodeManager manager = NodeManager.get();
        manager.deleteNode(node.getIdentifier());
        mListviewNodeList.invalidateViews();
    }

    private void checkPermissions() {
        //Check Location permission
        int mFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if(mFineLocationPermission == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    EventBus.getDefault().postSticky(new PermissionsGrantedEvent());
                } else {
                    EventBus.getDefault().postSticky(new PermissionsDeniedEvent());
                }
                finish();
                break;
        }
    }

    private List<NodeInfo> loadNodeList() {
        NodeManager manager = NodeManager.get();
        Map<String, NodeInfo> nodelist = manager.getNodeList();
        return new ArrayList<>(nodelist.values());
    }

    @Override
    public void dialogResult(int requestCode, boolean cancelled, NodeInfo node) {
        if(!cancelled) {
            if (node != null) {
                if (requestCode == REQUEST_ADD_NODE) {
                    final String nodeAddress = node.getAddress();
                    final int nodePort = node.getPort();
                    final VLatLng nodeLatLng = node.getLatLng();
                    final int nodeBwUp = node.getBandwidthUp();
                    final int nodeBwDown = node.getBandwidthDown();
                    NodeInfo ni = new NodeInfo("", nodeAddress, nodePort, NodeType.UNKNOWN, nodeLatLng);
                    ni.setBandwidthUp(nodeBwUp);
                    ni.setBandwidthDown(nodeBwDown);

                    NodeManager manager = NodeManager.get();
                    NodeInfo n = manager.addNode(ni);
                    //Add the node configuration to the node manager
                    if(n != null) {
                        mNodeInfoItems.add(0, n);
                        //Show that the node has been added
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.node_added_new),
                                Toast.LENGTH_SHORT)
                                .show();
                        //Refresh the list
                        mListviewNodeList.setVisibility(View.VISIBLE);
                        mListviewNodeList.invalidateViews();
                        mPositionClicked = -1;
                        return;
                    } else {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.failed_contact_node),
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                } else if (requestCode == REQUEST_EDIT_NODE) {
                    mNodeInfoItems.set(mPositionClicked, node);
                    NodeManager manager = NodeManager.get();
                    manager.updateNode(node);
                    Toast.makeText(this, getString(R.string.node_config_updated), Toast.LENGTH_SHORT).show();
                }
                mListviewNodeList.invalidateViews();
            }
            mPositionClicked = -1;
        }
    }


    private void showDlNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setOngoing(false)
                .setContentTitle("vStore Download in progress (0%)...")
                .setContentText("")
                .setSmallIcon(R.drawable.ic_launcher)
                .setProgress(100, 0, false)
                .setVibrate(null);
        if(android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            builder.setChannelId("vStore");
        }
        Notification notification = builder.build();
        notificationManager.notify(dl_notify, notification);
    }

    Integer dl_notify = 4;
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null)
        {
            // Download the scanned file
            String fileId = scanResult.getContents();
            if(fileId == null || fileId.equals("")) { return; }
            VStore.getInstance().getFile(fileId, null);
            showDlNotification();
            Intent mainActIntent = new Intent(this, MainActivity.class);
            startActivity(mainActIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AwareController.configureAwarePlugins(getApplicationContext());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ConfigDownloadSucceededEvent evt) {
        mNodeInfoItems = loadNodeList();
        mListviewNodeList.invalidateViews();
    }
}

