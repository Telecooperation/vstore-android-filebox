package vstore.android_filebox.config_activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import vstore.android_filebox.R;
import vstore.framework.node.NodeInfo;


public class NodeListAdapter extends ArrayAdapter<NodeInfo> {
    public NodeListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public NodeListAdapter(Context context, int resource, List<NodeInfo> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(final int position, View view, final ViewGroup parent) {
        final View v;
        if (view == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.nodeinfo_list_row, parent, false);
        } else {
            v = view;
        }

        NodeInfo n = getItem(position);
        if (n != null) {
            TextView txtNodeRowID = (TextView) v.findViewById(R.id.txtNodeRowID);
            TextView txtNodeRowTitle = (TextView) v.findViewById(R.id.txtNodeRowTitle);
            TextView txtNodeRowAddress = (TextView) v.findViewById(R.id.txtNodeRowAddress);
            TextView txtNodeRowPort = (TextView) v.findViewById(R.id.txtNodeRowPort);
            TextView txtNodeBandwidth = (TextView) v.findViewById(R.id.txtNodeBandwidth);
            TextView txtNodeRowLocation = (TextView) v.findViewById(R.id.txtNodeRowLocation);
            TextView txtNodeRowType = (TextView) v.findViewById(R.id.txtNodeRowType);
            Button btnEdit = (Button) v.findViewById(R.id.btnEditNodeInfo);
            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View btnView) {
                    ((ListView) parent).performItemClick(btnView, position, 0);
                }
            });
            Button btnDelete = (Button) v.findViewById(R.id.btnDeleteNode);
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View btnView) {
                    ((ListView) parent).performItemClick(btnView, position, 0);
                }
            });
            txtNodeRowID.setText("ID: " + n.getIdentifier());
            txtNodeRowTitle.setText("Node "+position);
            txtNodeRowAddress.setText("Address: " + n.getAddress());
            txtNodeRowPort.setText("Port: " + n.getPort());
            txtNodeBandwidth.setText("Downstream: " + n.getBandwidthDown() + " MBit/s\nUpstream: " + n.getBandwidthUp() + " MBit/s");
            txtNodeRowLocation.setText("Location: " + n.getLatLng().getLatitude() + ", " + n.getLatLng().getLongitude());
            String type = n.getNodeType().name();
            txtNodeRowType.setText("Type: " + type.substring(0,1).toUpperCase() + type.substring(1).toLowerCase());
        }

        return v;
    }
}
