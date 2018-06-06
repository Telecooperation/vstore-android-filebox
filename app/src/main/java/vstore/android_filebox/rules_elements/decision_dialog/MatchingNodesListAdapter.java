package vstore.android_filebox.rules_elements.decision_dialog;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

import vstore.android_filebox.R;
import vstore.android_filebox.utils.StringUtils;
import vstore.framework.VStore;
import vstore.framework.context.types.location.VLatLng;
import vstore.framework.context.types.location.VLocation;
import vstore.framework.node.NodeInfo;

public class MatchingNodesListAdapter extends ArrayAdapter<NodeInfo> {

    DecimalFormat df = new DecimalFormat("#.##");

    public MatchingNodesListAdapter(@NonNull Context context, @LayoutRes int resource) {
        super(context, resource);
    }

    public MatchingNodesListAdapter(Context context, int resource, List<NodeInfo> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        df.setRoundingMode(RoundingMode.CEILING);

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.decision_layer_node_list_row, null);
        }
        NodeInfo n = getItem(position);
        if (n != null)
        {
            VLocation l = VStore.getInstance().getCurrentContext().getLocationContext();
            VLatLng latlng = l.getLatLng();
            TextView t1 = (TextView) v.findViewById(R.id.rowTextView);
            String text = n.getAddress() + ":" + n.getPort();
            text += "\n   " + StringUtils.capitalizeOnlyFirstLetter(n.getNodeType().toString());
            text += " - " + df.format(n.getGeographicDistanceTo(latlng)) + "km";
            text += "\n   Up: " + n.getBandwidthUp() + " MBit/s - Down: " + n.getBandwidthDown() + " MBit/s";
            t1.setText(text);
        }
        return v;
    }
}
