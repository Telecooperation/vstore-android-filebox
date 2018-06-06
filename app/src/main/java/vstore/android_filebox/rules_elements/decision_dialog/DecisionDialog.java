package vstore.android_filebox.rules_elements.decision_dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import vstore.android_filebox.R;
import vstore.android_filebox.rules_elements.CreateRuleDialog;
import vstore.android_filebox.utils.StringUtils;
import vstore.android_filebox.utils.UIUtils;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeManager;
import vstore.framework.node.NodeType;
import vstore.framework.rule.DecisionLayer;

public class DecisionDialog extends DialogFragment {

    private View mRootView;

    private NodeManager mNodeManager;

    final String[] spinnerData = new String[NodeType.values().length];
    private LinearLayout mLayoutDecisionLayers;
    private boolean mNeedToConfirmLastRow;
    private boolean mAddedNewRow;

    List<DecisionLayer> mLayers;

    private ArrayList<MatchingNodesListAdapter> mListAdapters;

    public static DecisionDialog newInstance(List<DecisionLayer> layers) {
        DecisionDialog dialog = new DecisionDialog();
        if(layers != null) {
            dialog.mLayers = layers;
        } else {
            dialog.mLayers = new ArrayList<>();
        }
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mNodeManager = NodeManager.get();
        mNeedToConfirmLastRow = false;
        mAddedNewRow = false;
        mListAdapters = new ArrayList<>();
        fillSpinnerArray();

        mRootView = getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_decision, null);

        ImageButton btnAddDecisionLayer = (ImageButton) mRootView.findViewById(R.id.btnAddDecisionLayer);
        mLayoutDecisionLayers = (LinearLayout) mRootView.findViewById(R.id.layoutDecisionLayers);
        btnAddDecisionLayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mNeedToConfirmLastRow) {
                    if(mLayers.size() > 0 && mLayers.get(mLayers.size()-1).isSpecific) {
                        Toast.makeText(getActivity(), R.string.hint_last_is_specific, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mNeedToConfirmLastRow = true;
                    mAddedNewRow = true;

                    View row = inflateNewRow();
                    row.findViewById(R.id.layoutEditRow).setVisibility(View.VISIBLE);
                    row.findViewById(R.id.layoutRowText).setVisibility(View.GONE);

                    //Create new list adapter for specific nodes
                    MatchingNodesListAdapter a = new MatchingNodesListAdapter(
                            getActivity(),
                            R.layout.decision_layer_node_list_row,
                            new ArrayList<NodeInfo>());
                    ((ListView)row.findViewById(R.id.listNodes)).setAdapter(a);
                    mListAdapters.add(a);
                    mLayers.add(new DecisionLayer());

                    //Add row to tree
                    mLayoutDecisionLayers.addView(row);
                    setRowClickListeners(row);

                    initSpinner(row, null);

                    //Hide "no elements" text
                    mRootView.findViewById(R.id.txtNoLayersYet).setVisibility(View.GONE);
                } else {
                    Toast.makeText(getActivity(), R.string.confirm_row_first, Toast.LENGTH_SHORT).show();
                }
            }
        });

        initTexts(mLayoutDecisionLayers);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(mRootView)
                .setTitle(R.string.title_decision)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        JSONArray resultJson = new JSONArray();
                        for(DecisionLayer l : mLayers) {
                            try {
                                resultJson.put(new JSONObject(l.toJsonString()));
                            } catch(JSONException e) {}
                        }
                        Intent intent = new Intent();
                        intent.putExtra("decisions", resultJson.toString());
                        getTargetFragment().onActivityResult(
                                getTargetRequestCode(),
                                CreateRuleDialog.OK_REQUEST,
                                intent);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        getTargetFragment().onActivityResult(
                                getTargetRequestCode(),
                                CreateRuleDialog.CANCEL_REQUEST,
                                null);
                    }
                });

        AlertDialog d = builder.create();
        d.show();
        d.getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        return d;
    }

    private View inflateNewRow() {
        View row = getActivity().getLayoutInflater().inflate(R.layout.add_decision_layer, mLayoutDecisionLayers, false);
        //Show edit mode for the row
        row.findViewById(R.id.layoutEditRow).setVisibility(View.GONE);
        row.findViewById(R.id.layoutByParameters).setVisibility(View.VISIBLE);
        row.findViewById(R.id.layoutSpecificNode).setVisibility(View.GONE);
        row.findViewById(R.id.layoutRowText).setVisibility(View.VISIBLE);
        return row;
    }

    private void fillSpinnerArray() {
        for(int i = 0; i<NodeType.values().length; ++i) {
            spinnerData[i] = StringUtils.capitalizeOnlyFirstLetter(NodeType.values()[i].name());
        }
    }

    private void initSpinner(final View row, NodeType t) {
        ViewGroup parentOfRow = (ViewGroup)row.getParent();
        final int index = parentOfRow.indexOfChild(row);
        Spinner s = (Spinner) row.findViewById(R.id.spinnerNodeType);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, spinnerData);
        s.setAdapter(adapter);
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //Get selected type and fill node result list
                NodeType t = NodeType.valueOf(spinnerData[i].toUpperCase());
                mLayers.get(index).targetType = t;
                MatchingNodesListAdapter adapter = mListAdapters.get(index);
                adapter.clear();
                List<NodeInfo> nodes = mNodeManager.getNodesOfType(t);
                adapter.addAll(nodes);
                adapter.notifyDataSetChanged();
                UIUtils.setListViewHeightBasedOnChildren((ListView)row.findViewById(R.id.listNodes));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        //Set initial spinner position
        if(t != null) {
            //Find index of type in array
            int i = 0;
            String type = StringUtils.capitalizeOnlyFirstLetter(t.toString());
            while(!spinnerData[i].equals(type)) { ++i; }
            s.setSelection(i);
        }
    }

    private void initTexts(View root) {
        if(mLayers.size() > 0) {
            for(int i = 0; i<mLayers.size(); ++i) {
                //Create list adapters for the specific nodes setting
                MatchingNodesListAdapter a = new MatchingNodesListAdapter(
                        getActivity(),
                        R.layout.decision_layer_node_list_row,
                        new ArrayList<NodeInfo>());

                //Inflate a row for each item and save adapter
                View row = inflateNewRow();
                ((ListView)row.findViewById(R.id.listNodes)).setAdapter(a);
                mListAdapters.add(a);
                //Add row to the tree
                ((ViewGroup)root).addView(row);
                //Init the spinner at the correct position
                if(mLayers.get(i).targetType != null) {
                    initSpinner(row, mLayers.get(i).targetType);
                } else {
                    initSpinner(row, null);
                }
                setRowClickListeners(row);
                refreshText(row, i);
                row.findViewById(R.id.layoutRowText).setVisibility(View.VISIBLE);
            }
            ((LinearLayout)root.getParent().getParent()).findViewById(R.id.txtNoLayersYet).setVisibility(View.GONE);
        } else {
            ((LinearLayout)root.getParent().getParent()).findViewById(R.id.txtNoLayersYet).setVisibility(View.VISIBLE);
        }
    }

    private void setRowClickListeners(final View row) {
        ViewGroup parentOfRow = (ViewGroup)row.getParent();
        final int index = parentOfRow.indexOfChild(row);

        row.findViewById(R.id.btnSelectSpecificNode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button btn = ((Button)view);
                if(btn.getText().equals(getActivity().getString(R.string.select_specific_node))) {
                    row.findViewById(R.id.layoutByParameters).setVisibility(View.GONE);
                    row.findViewById(R.id.layoutSpecificNode).setVisibility(View.VISIBLE);
                    btn.setText(R.string.enter_params);
                } else {
                    row.findViewById(R.id.layoutByParameters).setVisibility(View.VISIBLE);
                    row.findViewById(R.id.layoutSpecificNode).setVisibility(View.GONE);
                    btn.setText(R.string.select_specific_node);
                }
            }
        });

        row.findViewById(R.id.btnCancelRow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mAddedNewRow) {
                    //Remove row from view
                    ViewGroup parentOfRow = (ViewGroup)view.getParent().getParent().getParent().getParent();
                    parentOfRow.removeView((ViewGroup)view.getParent().getParent().getParent());
                    mAddedNewRow = false;
                    mNeedToConfirmLastRow = false;
                    mLayers.remove(mLayers.size()-1);
                    mListAdapters.remove(mListAdapters.size()-1);
                } else {
                    row.findViewById(R.id.layoutEditRow).setVisibility(View.GONE);
                    row.findViewById(R.id.layoutByParameters).setVisibility(View.GONE);
                    row.findViewById(R.id.layoutSpecificNode).setVisibility(View.GONE);
                    row.findViewById(R.id.layoutRowText).setVisibility(View.VISIBLE);
                }
                if(mLayers.size() == 0) {
                    mRootView.findViewById(R.id.txtNoLayersYet).setVisibility(View.VISIBLE);
                }
            }
        });

        row.findViewById(R.id.btnSaveRow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Parse the contents
                try {
                    if(((Button)row.findViewById(R.id.btnSelectSpecificNode)).getText()
                            .equals(getActivity().getString(R.string.select_specific_node))) {
                        float minRadius, maxRadius;
                        int minBwUp, minBwDown;
                        try {
                            minRadius = Float.parseFloat(((EditText) row.findViewById(R.id.inputMinRadius)).getText().toString());
                        } catch(NumberFormatException e) {
                            minRadius = 0;
                        }
                        try {
                            maxRadius = Float.parseFloat(((EditText) row.findViewById(R.id.inputMaxRadius)).getText().toString());
                        } catch(NumberFormatException e) {
                            maxRadius = 0;
                        }
                        try {
                            minBwUp = Integer.parseInt(((EditText)row.findViewById(R.id.inputMinBwUp)).getText().toString());
                        } catch(NumberFormatException e) {
                            minBwUp = 0;
                        }
                        try {
                            minBwDown = Integer.parseInt(((EditText)row.findViewById(R.id.inputMinBwDown)).getText().toString());
                        } catch(NumberFormatException e) {
                            minBwDown = 0;
                        }
                        if(minRadius > maxRadius) {
                            throw new NumberFormatException();
                        }
                        if(minRadius < 0 || maxRadius < 0 || minBwUp < 0 || minBwDown < 0) {
                            throw new NumberFormatException();
                        }

                        DecisionLayer layer = mLayers.get(index);
                        layer.isSpecific = false;
                        layer.specificNodeId = "";
                        layer.minRadius = minRadius;
                        layer.maxRadius = maxRadius;
                        layer.minBwUp = minBwUp;
                        layer.minBwDown = minBwDown;
                    } else {
                        DecisionLayer layer = mLayers.get(index);
                        layer.minRadius = 0;
                        layer.maxRadius = 0;
                        layer.minBwUp = 0;
                        layer.minBwDown = 0;
                        //Specific node id is configured in item click for list, see below
                    }

                    row.findViewById(R.id.layoutByParameters).setVisibility(View.GONE);
                    row.findViewById(R.id.layoutSpecificNode).setVisibility(View.GONE);
                    row.findViewById(R.id.layoutRowText).setVisibility(View.VISIBLE);
                    row.findViewById(R.id.layoutEditRow).setVisibility(View.GONE);

                    refreshText(row, index);
                    mNeedToConfirmLastRow = false;
                    mAddedNewRow = false;
                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), R.string.wrong_values_entered, Toast.LENGTH_SHORT).show();
                }
            }
        });

        ((ListView)row.findViewById(R.id.listNodes)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    ListView lv = ((ListView)row.findViewById(R.id.listNodes));
                    mLayers.get(index).isSpecific = true;
                    mLayers.get(index).specificNodeId = mListAdapters.get(index).getItem(i).getIdentifier();
                } catch(ClassCastException e) {}
            }
        });

        row.findViewById(R.id.btnEditRow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mLayers.get(index).isSpecific) {
                    row.findViewById(R.id.layoutByParameters).setVisibility(View.GONE);
                    row.findViewById(R.id.layoutSpecificNode).setVisibility(View.VISIBLE);
                    ((Button)row.findViewById(R.id.btnSelectSpecificNode)).setText(getActivity().getString(R.string.enter_params));
                } else {
                    row.findViewById(R.id.layoutByParameters).setVisibility(View.VISIBLE);
                    row.findViewById(R.id.layoutSpecificNode).setVisibility(View.GONE);
                    ((Button)row.findViewById(R.id.btnSelectSpecificNode)).setText(getActivity().getString(R.string.select_specific_node));
                }
                row.findViewById(R.id.layoutRowText).setVisibility(View.GONE);
                row.findViewById(R.id.layoutEditRow).setVisibility(View.VISIBLE);

                //Fill edit texts with values
                ((EditText) row.findViewById(R.id.inputMinRadius)).setText(""+mLayers.get(index).minRadius);
                ((EditText) row.findViewById(R.id.inputMaxRadius)).setText(""+mLayers.get(index).maxRadius);
                ((EditText) row.findViewById(R.id.inputMinBwUp)).setText(""+mLayers.get(index).minBwUp);
                ((EditText) row.findViewById(R.id.inputMinBwDown)).setText(""+mLayers.get(index).minBwDown);
            }
        });
    }

    private void refreshText(View row, int index) {
        TextView txtDecisionRowText = (TextView) row.findViewById(R.id.txtDecisionRowText);
        DecisionLayer layer = mLayers.get(index);
        String nodetype = StringUtils.capitalizeOnlyFirstLetter(layer.targetType.toString());
        if(layer.isSpecific) {
            txtDecisionRowText.setText("Specific node: " + nodetype);
        } else {
            txtDecisionRowText.setText(nodetype);
            if(layer.minRadius > 0 && layer.maxRadius > 0) {
                txtDecisionRowText.append("\n Radius: " + layer.minRadius+"km - " + layer.maxRadius+"km");
            } else if(layer.minRadius == 0 && layer.maxRadius > 0) {
                txtDecisionRowText.append("\n Radius: " + " <= " + layer.maxRadius+"km");
            } else if(layer.maxRadius == 0 && layer.minRadius > 0) {
                txtDecisionRowText.append("\n Radius: " + " >= " + layer.minRadius+"km");
            }

            if(layer.minBwUp > 0 && layer.minBwDown > 0) {
                txtDecisionRowText.append("\n Bandwidth: Up " + layer.minBwUp+"MBit/s, Down " + layer.minBwDown+"MBit/s");
            } else if(layer.minBwUp == 0 && layer.minBwDown > 0) {
                txtDecisionRowText.append("\n Bandwidth: Down " + layer.minBwDown+"MBit/s");
            } else if(layer.minBwDown == 0 && layer.minBwUp > 0) {
                txtDecisionRowText.append("\n Bandwidth: Up " + layer.minBwUp+"MBit/s");
            }
        }
    }
}
