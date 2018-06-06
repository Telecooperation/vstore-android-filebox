package vstore.android_filebox.config_activity;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import vstore.android_filebox.R;
import vstore.framework.context.types.location.VLatLng;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeType;

/**
 * This DialogFragment shows a dialog for creating/editing the properties of a storage node
 */
public class EditNodeDialog extends DialogFragment {
    public static final int CANCEL_REQUEST = -1;
    public static final int OK_REQUEST = 1;

    private static final int REQUEST_NODE_LOCATION = 0;

    interface EditNodeResult {
        void dialogResult(int requestCode, boolean cancelled, NodeInfo node);
    }
    private EditNodeResult interfaceResult;
    private int mRequestCode;
    private String mTitle;

    private NodeInfo mNode;

    private EditText mInputNodeAddress;
    private EditText mInputNodePort;
    private double mLatitude;
    private double mLongitude;

    private TextView mTxtCurrentLocation;

    /**
     * Create a new instance of the EditNodeDialog.
     */
    static EditNodeDialog newInstance(int requestCode, String title, NodeInfo node) {
        EditNodeDialog d = new EditNodeDialog();
        d.setRequestCode(requestCode);
        d.mTitle = title;

        if(node != null) {
            d.mNode = node;
            d.mLatitude = node.getLatLng().getLatitude();
            d.mLongitude = node.getLatLng().getLongitude();
        }
        return d;
    }

    private void setRequestCode(int requestCode) {
        mRequestCode = requestCode;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View dialogLayout = getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_edit_node, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        mInputNodeAddress = (EditText) dialogLayout.findViewById(R.id.inputNodeAddress);
        mInputNodePort = (EditText) dialogLayout.findViewById(R.id.inputNodePort);
        mTxtCurrentLocation = (TextView) dialogLayout.findViewById(R.id.txtCurrentLocation);
        Button btnChooseLocation = (Button) dialogLayout.findViewById(R.id.btnChooseLocation);
        btnChooseLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Create a new dialog to select the location for this node
                FragmentManager fragmentManager = getFragmentManager();
                NodeLocationDialog dialog = NodeLocationDialog.newInstance(new LatLng(mLatitude, mLongitude));
                dialog.show(fragmentManager, "node_location_dialog");
                dialog.setTargetFragment(EditNodeDialog.this, REQUEST_NODE_LOCATION);
            }
        });

        //Load the data from the node to edit if one was given before
        if(mNode != null) {
            mLatitude = mNode.getLatLng().getLatitude();
            mLongitude = mNode.getLatLng().getLongitude();
            refreshDataViews();
        } else {
            mTxtCurrentLocation.setText(R.string.no_location_selected);
        }

        //Build the dialog
        builder.setTitle(mTitle)
                .setView(dialogLayout)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        interfaceResult.dialogResult(mRequestCode, true, null);
                    }
                })
                .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Get data from the fields
                        String id = "";
                        String address = mInputNodeAddress.getText().toString();
                        int port = Integer.parseInt(mInputNodePort.getText().toString());
                        NodeType type = null;
                        if(mNode != null) {
                            id = mNode.getIdentifier();
                            type = mNode.getNodeType();
                        }
                        mNode = new NodeInfo(id, address, port, type, new VLatLng(mLatitude,mLongitude));
                        interfaceResult.dialogResult(mRequestCode, false, mNode);
                    }
                });

        return builder.create();
    }

    private void refreshDataViews() {
        mInputNodeAddress.setText(mNode.getAddress());
        mInputNodePort.setText(""+mNode.getPort());
        refreshLocationView();
    }
    private void refreshLocationView() {
        if(mLatitude != 0 && mLongitude != 0){
            mTxtCurrentLocation.setText("Latitude: " + mLatitude + "\nLongitude: " + mLongitude);
        } else {
            mTxtCurrentLocation.setText(R.string.no_location_selected);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != CANCEL_REQUEST) {
            switch (requestCode) {
                case REQUEST_NODE_LOCATION:
                    if (data.hasExtra("lat") && data.hasExtra("lng")) {
                        mLatitude = data.getDoubleExtra("lat", 0);
                        mLongitude = data.getDoubleExtra("lng", 0);
                        if(mLatitude != 0 && mLongitude != 0) {
                            Toast.makeText(getActivity(),
                                    "Location updated!",
                                    Toast.LENGTH_SHORT)
                                    .show();
                            refreshLocationView();
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //Attach the interface result for returning the data back to the activity on button click
        try {
            interfaceResult = (EditNodeResult) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement EditNodeResult");
        }
    }
}

