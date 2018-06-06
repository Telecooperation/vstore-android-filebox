package vstore.android_filebox.rules_elements;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import vstore.android_filebox.R;
import vstore.android_filebox.rules_elements.decision_dialog.DecisionDialog;
import vstore.android_filebox.utils.ContextUtils;
import vstore.android_filebox.utils.StringUtils;
import vstore.framework.VStore;
import vstore.framework.context.types.activity.ActivityType;
import vstore.framework.context.types.network.VNetwork;
import vstore.framework.context.types.network.cellular.CellularNetwork;
import vstore.framework.context.types.network.wifi.WiFi;
import vstore.framework.context.types.noise.VNoise;
import vstore.framework.context.types.place.PlaceConstants;
import vstore.framework.context.types.place.PlaceType;
import vstore.framework.rule.DecisionLayer;
import vstore.framework.rule.VStoreRule;

/**
 * This DialogFragment shows a dialog for creating/editing the properties of a rule
 */
public class CreateRuleDialog extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
    public static final int CANCEL_REQUEST = -1;
    public static final int OK_REQUEST = 1;
    private static final int REQUEST_MIME_TYPES = 0;
    private static final int REQUEST_CONTEXT_LOCATION = 1;
    private static final int REQUEST_CONTEXT_PLACE = 2;
    private static final int REQUEST_CONTEXT_ACTIVITY = 3;
    private static final int REQUEST_CONTEXT_NETWORK = 4;
    private static final int REQUEST_CONTEXT_NOISE = 5;
    private static final int REQUEST_DECISION_SETTINGS = 6;
    private static final int REQUEST_NODE_CONSTRAINTS = 7;

    interface EditRuleFragmentResult {
        void dialogResult(int requestCode, boolean cancelled, VStoreRule rule);
    }
    private EditRuleFragmentResult interfaceResult;
    private int mRequestCode;

    private VStoreRule mRule;

    private EditText mInputRuleName;
    private CheckBox mDomainPublic;
    private CheckBox mDomainPrivate;
    private TextView mTxtDisplayMimeTypes;
    private EditText mInputMinFileSize;
    private TextView mTxtNoContextTriggers;
    private LinearLayout mLayoutContextContent;
    private TextView mTxtNoDecision;
    private HashMap<Integer, CheckBox> mDayBoxes;
    private Button mBtnRuleTimeStart;
    private Button mBtnRuleTimeEnd;
    //private ImageButton mBtnEditConstraints;
    //private TextView mTxtConstraintsText;

    private boolean mStartTimeClicked;
    private boolean mEditable;

    DecimalFormat mFormat;
    DecimalFormat mFormat2;

    /**
     * Create a new instance of the CreateRuleDialog.
     */
    static CreateRuleDialog newInstance(int requestCode, String title, VStoreRule rule, boolean editable) {
        CreateRuleDialog f = new CreateRuleDialog();
        f.setRequestCode(requestCode);

        Bundle args = new Bundle();
        args.putString("title", title);
        if(rule != null) {
            f.mRule = rule;
        } else {
            return null;
        }
        f.setArguments(args);

        f.mEditable = editable;

        return f;
    }

    private void setRequestCode(int requestCode) {
        mRequestCode = requestCode;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int theme = R.style.AppDialogTheme;
        setStyle(STYLE_NORMAL, theme);

        mFormat = new DecimalFormat("#.######");
        mFormat.setRoundingMode(RoundingMode.CEILING);

        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.getDefault());
        otherSymbols.setDecimalSeparator('.');
        otherSymbols.setGroupingSeparator(' ');
        mFormat2 = new DecimalFormat("#.##", otherSymbols);
        mFormat2.setRoundingMode(RoundingMode.CEILING);
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View d = getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_create_rule, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        mInputRuleName = (EditText) d.findViewById(R.id.inputRuleName);
        mDomainPublic = (CheckBox) d.findViewById(R.id.checkboxPublicDomain);
        mDomainPrivate = (CheckBox) d.findViewById(R.id.checkboxPrivateDomain);
        //Save references to the checkboxes for later
        mDayBoxes = new HashMap<>();
        mDayBoxes.put(1, (CheckBox)d.findViewById(R.id.checkboxMon));
        mDayBoxes.put(2, (CheckBox)d.findViewById(R.id.checkboxTue));
        mDayBoxes.put(3, (CheckBox)d.findViewById(R.id.checkboxWed));
        mDayBoxes.put(4, (CheckBox)d.findViewById(R.id.checkboxThur));
        mDayBoxes.put(5, (CheckBox)d.findViewById(R.id.checkboxFri));
        mDayBoxes.put(6, (CheckBox)d.findViewById(R.id.checkboxSat));
        mDayBoxes.put(7, (CheckBox)d.findViewById(R.id.checkboxSun));
        mBtnRuleTimeStart = (Button) d.findViewById(R.id.btnRuleTimeStart);
        mBtnRuleTimeEnd = (Button) d.findViewById(R.id.btnRuleTimeEnd);
        mTxtDisplayMimeTypes = (TextView) d.findViewById(R.id.txtDisplayMimeTypes);
        mInputMinFileSize = (EditText) d.findViewById(R.id.inputMinFilesize);
        mTxtNoContextTriggers = (TextView) d.findViewById(R.id.txtNoContextTriggers);
        ImageButton mBtnEditMimeTypes = (ImageButton) d.findViewById(R.id.btnEditMimeTypes);
        ImageButton btnAddContext = (ImageButton) d.findViewById(R.id.btnAddContext);
        mLayoutContextContent = (LinearLayout) d.findViewById(R.id.layoutContextContent);
        ImageButton btnEditDecision = (ImageButton) d.findViewById(R.id.btnEditDecision);
        mTxtNoDecision = (TextView) d.findViewById(R.id.txtNoDecision);
        //mBtnEditConstraints = (ImageButton) d.findViewById(R.id.btnEditConstraints);
        //mTxtConstraintsText = (TextView) d.findViewById(R.id.txtConstraintsText);

        if(mEditable) {
            mDomainPublic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean pub) {
                    boolean priv = mDomainPrivate.isChecked();
                    updateSharingDomainState(pub, priv);
                }
            });

            mDomainPrivate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean priv) {
                    boolean pub = mDomainPublic.isChecked();
                    updateSharingDomainState(pub, priv);
                }
            });
            mBtnRuleTimeStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mStartTimeClicked = true;
                    showTimePickerDialog();
                }
            });
            mBtnRuleTimeEnd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mStartTimeClicked = false;
                    showTimePickerDialog();
                }
            });
            mBtnEditMimeTypes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showMimeTypeDialog();
                }
            });
            btnAddContext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showContextDialog();
                }
            });
            btnEditDecision.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDecisionDialog();
                }
            });

            /*mBtnEditConstraints.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showConstraintsDialog();
                }
            });*/

        } else {
            mInputRuleName.setEnabled(false);
            mDomainPrivate.setEnabled(false);
            mDomainPublic.setEnabled(false);
            for(CheckBox c : mDayBoxes.values()) {
                c.setEnabled(false);
            }
            mBtnRuleTimeStart.setEnabled(false);
            mBtnRuleTimeEnd.setEnabled(false);
            mBtnEditMimeTypes.setEnabled(false);
            mInputMinFileSize.setEnabled(false);
            btnAddContext.setEnabled(false);
            btnEditDecision.setEnabled(false);
            //mBtnEditConstraints.setEnabled(false);
        }

        //Update views to the state of the passed rule
        mInputRuleName.setText(mRule.getName());
        refreshSharingDomain();
        refreshDay();
        refreshTime();
        refreshMimeTypes();
        refreshFileSize();
        refreshContextTriggers();
        refreshDecisions();
        //refreshConstraints();
        String title = getString(R.string.edit_rule);
        //Check if we have arguments
        if(getArguments() != null) {
            //Set the correct title for the dialog
            title = getArguments().getString("title", getString(R.string.edit_rule));
        }

        builder.setView(d)
                .setTitle(title)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(mEditable) {
                            //Get the values from the day checkboxes
                            for (int b = 1; b < 8; b++) {
                                if (mDayBoxes.get(b).isChecked()) {
                                    if (!mRule.getWeekdays().contains(b)) {
                                        mRule.getWeekdays().add(b);
                                    }
                                } else {
                                    mRule.getWeekdays().remove(Integer.valueOf(b));
                                }
                            }
                            //Get the values for the filesize
                            try {
                                float sizeInMB = Float.parseFloat(mInputMinFileSize.getText().toString());
                                mRule.setMinFileSize((long)(sizeInMB * 1024.0f * 1024.0f));
                            } catch(NumberFormatException e) { }

                            if (mInputRuleName.getText().toString().equals("")) {
                                Toast.makeText(getActivity(),
                                        getString(R.string.please_enter_name),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            } else {
                                dismiss();
                                mRule.setName(mInputRuleName.getText().toString());
                                interfaceResult.dialogResult(mRequestCode, false, mRule);
                            }
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                        interfaceResult.dialogResult(mRequestCode, true, null);
                    }
                });

        return builder.create();
    }

    private void updateSharingDomainState(boolean pub, boolean priv) {
        if(pub && priv) {
            mRule.setSharingDomain(-1);
            return;
        }
        if(!pub && priv) {
            mRule.setSharingDomain(1);
            return;
        }
        if(pub && !priv) {
            mRule.setSharingDomain(0);
            return;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            interfaceResult = (EditRuleFragmentResult) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement EditRuleFragmentResult");
        }
    }

    /**
     * Used for receiving the result from the second popups for mimetypes and context editing.
     * @param requestCode The request code for the request that started the second dialog.
     * @param resultCode The result code from the request
     * @param data The intent containing extra data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != CANCEL_REQUEST) {
            switch (requestCode) {
                case REQUEST_MIME_TYPES:
                    if (data.hasExtra("mimetypes") && data.hasExtra("checked")) {
                        //Get both arrays from the extra
                        CharSequence[] mimetypes = data.getCharSequenceArrayExtra("mimetypes");
                        boolean[] checked = data.getBooleanArrayExtra("checked");
                        //Add checked mimetypes to the list, remove unchecked ones
                        for (int i = 0; i < mimetypes.length; ++i) {
                            if (checked[i]) {
                                mRule.addMimeType(mimetypes[i].toString());
                            } else {
                                mRule.removeMimeType(mimetypes[i].toString());
                            }
                        }
                        refreshMimeTypes();
                        Toast.makeText(getActivity(),
                                getString(R.string.mime_types_updated),
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                    break;

                case REQUEST_CONTEXT_LOCATION:
                    //Process the results from the location context popup
                    if (data.hasExtra("lat") && data.hasExtra("lng") && data.hasExtra("radius")) {
                        double lat = data.getDoubleExtra("lat", 0);
                        double lng = data.getDoubleExtra("lng", 0);
                        int radius = data.getIntExtra("radius", 0);
                        if(lat > 0 && lng > 0 && radius > 0) {
                            mRule.getRuleContext().setLocationContext(lat, lng, radius);
                            Toast.makeText(getActivity(),
                                    getString(R.string.location_context_updated),
                                    Toast.LENGTH_SHORT)
                                    .show();
                        } else {
                            Toast.makeText(getActivity(),
                                    getString(R.string.error_location_data),
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    } else if(data.hasExtra("delete")) {
                        mRule.clearLocationContext();
                        Toast.makeText(getActivity(),
                                getString(R.string.location_context_deleted),
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                    break;

                case REQUEST_CONTEXT_PLACE:
                    if (data.hasExtra("types") && data.hasExtra("checked")) {
                        //Get both arrays from the extra
                        CharSequence[] types = data.getCharSequenceArrayExtra("types");
                        boolean[] checked = data.getBooleanArrayExtra("checked");
                        //Add checked mimetypes to the list, remove unchecked ones
                        for (int i = 0; i < types.length; ++i) {
                            PlaceType type
                                    = PlaceConstants.getPlaceTypeFromString(types[i].toString());
                            if (checked[i]) {
                                mRule.getRuleContext().addPlaceType(type);
                            } else {
                                mRule.getRuleContext().removePlaceType(type);
                            }
                        }
                        Toast.makeText(getActivity(),
                                getString(R.string.place_context_updated),
                                Toast.LENGTH_SHORT)
                                .show();
                    } else if(data.hasExtra("delete")) {
                        mRule.clearPlaceContext();
                        Toast.makeText(getActivity(),
                                getString(R.string.place_context_deleted),
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                    break;

                case REQUEST_CONTEXT_ACTIVITY:
                    if(data.hasExtra("newActivity")) {
                        //Get the extra containing the activity id
                        String activityId = data.getStringExtra("newActivity");
                        if(activityId != null && !activityId.equals(""))
                        {
                            mRule.getRuleContext().setActivityContext(ActivityType.valueOf(activityId));
                            Toast.makeText(getActivity(),
                                    getString(R.string.activity_context_updated),
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    } else if(data.hasExtra("delete")) {
                        mRule.clearActivityContext();
                        Toast.makeText(getActivity(),
                                getString(R.string.activity_context_deleted),
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                    break;

                case REQUEST_CONTEXT_NETWORK:
                    if(data.hasExtra("vnetworkjson")) {
                        //Get the VNetwork json string from the extra and create a new
                        //VNetwork object from it
                        String strJson = data.getStringExtra("vnetworkjson");
                        try {
                            if(strJson == null || "".equals(strJson)) {
                                mRule.getRuleContext().setNetworkContext(null);
                            } else {
                                VNetwork vnet = new VNetwork(strJson);
                                mRule.getRuleContext().setNetworkContext(vnet);
                            }
                            Toast.makeText(getActivity(),
                                    getString(R.string.network_context_updated),
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                        catch(ParseException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    else if(data.hasExtra("delete"))
                    {
                        mRule.clearNetworkContext();
                        Toast.makeText(getActivity(),
                                getString(R.string.network_context_deleted),
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                    break;

                case REQUEST_CONTEXT_NOISE:
                    if(data.hasExtra("vnoisejson")) {
                        //Get the VNoise json string from the extra and create a new object from it
                        String strJson = data.getStringExtra("vnoisejson");
                        try
                        {
                            VNoise vnoise = new VNoise(strJson);
                            mRule.getRuleContext().setNoiseContext(vnoise);
                            Toast.makeText(getActivity(),
                                    getString(R.string.noise_context_updated),
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                        catch(ParseException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    else if(data.hasExtra("delete"))
                    {
                        mRule.clearNoiseContext();
                        Toast.makeText(getActivity(),
                                getString(R.string.noise_context_deleted),
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                    break;

                /*case REQUEST_NODE_CONSTRAINTS:
                    if(data.hasExtra("bandwidth_down") && data.hasExtra("bandwidth_up") && data.hasExtra("upload_duration")) {
                        mRule.setBandwidth(data.getIntExtra("bandwidth_down", 0), data.getIntExtra("bandwidth_up", 0));
                        mRule.setMaxUploadDuration(data.getIntExtra("upload_duration", 0));
                        Toast.makeText(getActivity(),
                                getString(R.string.constraints_updated),
                                Toast.LENGTH_SHORT)
                                .show();
                    } else if(data.hasExtra("delete")) {
                        mRule.clearConstraints();
                        Toast.makeText(getActivity(),
                                getString(R.string.constraints_deleted),
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                    refreshConstraints();
                    break;*/

                case REQUEST_DECISION_SETTINGS:
                    if(data.hasExtra("decisions")) {
                        try {
                            JSONArray decisions = new JSONArray(data.getStringExtra("decisions"));
                            mRule.setDecisionLayers(new ArrayList<DecisionLayer>());
                            for(int i = 0; i < decisions.length(); ++i) {
                                JSONObject layerJson = decisions.getJSONObject(i);
                                mRule.addDecisionLayer(new DecisionLayer(layerJson.toString()));
                            }
                        } catch(JSONException e)
                        {
                            e.printStackTrace();
                        }
                        refreshDecisions();
                    }
                    break;
            }
            refreshContextTriggers();
        }
    }

    private void showTimePickerDialog() {
        TimePickerDialog d = new TimePickerDialog(getActivity(), this, 10, 10, true);
        d.show();
    }

    private void showMimeTypeDialog() {
        //Get a list from all mimetypes supported by the framework
        //and match them against the list of mimetypes selected for this rule
        HashMap<String, String> types = VStore.getInstance().getFileManager().getSupportedTypes();
        HashMap<String, Boolean> checkedTypes = new HashMap<>();
        if(mRule == null)
        {
            for(String s : types.values())
            {
                checkedTypes.put(s, false);
            }
        }
        else
        {
            for(String s : types.values())
            {
                if(mRule.getMimeTypes().contains(s))
                {
                    checkedTypes.put(s, true);
                }
                else
                {
                    checkedTypes.put(s, false);
                }
            }
        }
        //Create a new dialog to select the mimetypes for this rule
        FragmentManager fragmentManager = getFragmentManager();
        SelectMimeTypesDialog dialog = SelectMimeTypesDialog.newInstance(checkedTypes);
        dialog.show(fragmentManager, "mime_types_dialog");
        dialog.setTargetFragment(CreateRuleDialog.this, REQUEST_MIME_TYPES);
    }

    /**
     * This method opens a dialog where the user can select what context data he wants to
     * edit for the rule. In the click listener for the positive button, the correct
     * context configuration dialog is then shown.
     */
    private void showContextDialog() {
        ArrayList<String> items = new ArrayList<>();
        //Only present context for choice that is not already configured for this rule
        if(!mRule.hasLocationContext()) items.add("Location");
        if(!mRule.hasPlaceContext()) items.add("Place");
        if(!mRule.hasActivityContext()) items.add("Activity");
        if(!mRule.hasNetworkContext()) items.add("Network");
        if(!mRule.hasNoiseContext()) items.add("Noise");

        //Only show a context configure dialog if some context is left for selection
        if(items.size() > 0) {
            final String[] itemArr = Arrays.copyOf(items.toArray(), items.size(), String[].class);
            //Build the dialog
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.title_context_select_dialog)
                    .setSingleChoiceItems(itemArr, 0, null)
                    .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                        //Attach a click listener to the done button to
                        //open the correct dialog the user selected.
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                            int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                            if (selectedPosition < itemArr.length) {
                                //Create a new dialog to configure selected context for this rule
                                switch (itemArr[selectedPosition]) {
                                    case "Location":
                                        //Open location context dialog and pass no initial location
                                        showLocationContextDialog(null, -1, mEditable);
                                        break;

                                    case "Place":
                                        showPlaceTypeContextDialog();
                                        break;

                                    case "Activity":
                                        showActivityContextDialog(ActivityType.UNKNOWN);
                                        break;

                                    case "Network":
                                        showNetworkContextDialog(null);
                                        break;

                                    case "Noise":
                                        showNoiseContextDialog(null);
                                        break;
                                }
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        } else {
            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.context_all_selected)
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        }
    }

    /**
     * Opens a dialog to configure the location context for this rule.
     * @param latlng The initial latitude and longitude to display on the map
     * @param radius The initial radius for the location
     */
    private void showLocationContextDialog(LatLng latlng, int radius, boolean editable) {
        FragmentManager fragmentManager = getFragmentManager();
        ContextLocationDialog d = ContextLocationDialog.newInstance(latlng, radius, editable);
        d.show(fragmentManager, "context_location_dialog");
        d.setTargetFragment(CreateRuleDialog.this, REQUEST_CONTEXT_LOCATION);
    }

    /**
     * Opens a dialog to configure the place types context for this rule.
     */
    private void showPlaceTypeContextDialog() {
        //Get a list from all placetypes supported by the framework
        //and match them against the list of placetypes selected for this rule
        List<PlaceType> supported = PlaceConstants.getPossiblePlaceTypes();
        List<PlaceType> selectedPlaces = mRule.getRuleContext().getPlaceTypes();
        HashMap<PlaceType, Boolean> data = new HashMap<>();

        //Put readable place types into a list to display
        for (int i = 0; i < supported.size(); ++i)
        {
            if (selectedPlaces != null && selectedPlaces.contains(supported.get(i)))
            {
                data.put(supported.get(i), true);
            }
            else
            {
                data.put(supported.get(i), false);
            }
        }
        FragmentManager fragmentManager = getFragmentManager();
        ContextPlaceTypesDialog d = ContextPlaceTypesDialog.newInstance(data);
        d.show(fragmentManager, "context_placetype_dialog");
        d.setTargetFragment(CreateRuleDialog.this, REQUEST_CONTEXT_PLACE);
    }

    /**
     * Opens a dialog to configure the activity for this rule.
     * @param currentActivity The id of the activity to initially mark as selected.
     */
    private void showActivityContextDialog(ActivityType currentActivity) {
        FragmentManager fragmentManager = getFragmentManager();
        ContextActivityDialog d = ContextActivityDialog.newInstance(currentActivity);
        d.show(fragmentManager, "context_activity_dialog");
        d.setTargetFragment(CreateRuleDialog.this, REQUEST_CONTEXT_ACTIVITY);
    }

    private void showNetworkContextDialog(VNetwork netCtx) {
        FragmentManager fragmentManager = getFragmentManager();
        ContextNetworkDialog d = ContextNetworkDialog.newInstance(netCtx);
        d.show(fragmentManager, "context_network_dialog");
        d.setTargetFragment(CreateRuleDialog.this, REQUEST_CONTEXT_NETWORK);
    }

    private void showNoiseContextDialog(VNoise noiseCtx) {
        FragmentManager fragmentManager = getFragmentManager();
        ContextNoiseDialog d = ContextNoiseDialog.newInstance(noiseCtx);
        d.show(fragmentManager, "context_noise_dialog");
        d.setTargetFragment(CreateRuleDialog.this, REQUEST_CONTEXT_NOISE);
    }

    private void showDecisionDialog() {
        FragmentManager fragmentManager = getFragmentManager();
        DecisionDialog d = DecisionDialog.newInstance(mRule.getDecisionLayers());
        d.show(fragmentManager, "decision_dialog");
        d.setTargetFragment(CreateRuleDialog.this, REQUEST_DECISION_SETTINGS);

        /*final String[] itemArr = new String[NodeType.values().length];
        int checkedPos = 0;
        for(int i = 0; i<NodeType.values().length; ++i) {
            itemArr[i] = StringUtils.capitalizeOnlyFirstLetter(NodeType.values()[i].name());
            if(mRule.getNodeTypeDecision() != null
                    && mRule.getNodeTypeDecision().equals(NodeType.values()[i])) {
                checkedPos = i;
            }
        }

        //Build the dialog
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_rule_decision_select)
                .setSingleChoiceItems(itemArr, checkedPos, null)
                .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                    //Attach a click listener to the done button
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        if (i < itemArr.length) {
                            mRule.setDecisionNodeType(NodeType.valueOf(itemArr[i].toUpperCase()));
                            refreshDecision();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                })
                .show();*/

    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        if(mStartTimeClicked) {
            mRule.setTimeStart(hourOfDay, minute);
        } else {
            mRule.setTimeEnd(hourOfDay, minute);
        }
        refreshTime();
    }

    /*private void showConstraintsDialog() {
        FragmentManager fragmentManager = getFragmentManager();
        ConstraintsDialog d = ConstraintsDialog.newInstance(mRule.getNodeConstraints());
        d.show(fragmentManager, "constraints_dialog");
        d.setTargetFragment(CreateRuleDialog.this, REQUEST_NODE_CONSTRAINTS);
    }*/

    private void refreshSharingDomain() {
        if (mRule != null) {
            switch (mRule.getSharingDomain()) {
                case -1:
                    mDomainPrivate.setChecked(true);
                    mDomainPublic.setChecked(true);
                    break;
                case 0:
                    mDomainPublic.setChecked(true);
                    break;
                case 1:
                    mDomainPrivate.setChecked(true);
                    break;
                default:
                    mDomainPrivate.setChecked(true);
                    mDomainPublic.setChecked(true);
                    break;
            }
        }
    }

    private void refreshDay() {
        List<Integer> days = mRule.getWeekdays();
        for(int i = 1; i<8; i++) {
            if(days.contains(i)) {
                mDayBoxes.get(i).setChecked(true);
            } else {
                mDayBoxes.get(i).setChecked(false);
            }
        }
    }
    private void refreshTime() {
        //Add leading 0 to the time if necessary
        String a = ((mRule.getStartHour() < 10) ? "0" : "");
        String b = ((mRule.getStartMinutes() < 10) ? "0" : "");
        mBtnRuleTimeStart.setText(a+mRule.getStartHour() + ":" + b+mRule.getStartMinutes());
        //Add leading 0 to the time if necessary
        a = ((mRule.getEndHour() < 10) ? "0" : "");
        b = ((mRule.getEndMinutes() < 10) ? "0" : "");
        mBtnRuleTimeEnd.setText(a+mRule.getEndHour() + ":" + b+mRule.getEndMinutes());
    }

    /**
     * Refreshes the mimetype display list
     */
    private void refreshMimeTypes() {
        if(mRule != null && mRule.getMimeTypes() != null) {
            if(mRule.getMimeTypes().size() > 0) {
                mTxtDisplayMimeTypes.setText("");
                for (String mimetype : mRule.getMimeTypes()) {
                    mTxtDisplayMimeTypes.append(mimetype + "\n");
                }
            } else {
                mTxtDisplayMimeTypes.setText(R.string.no_mimes_selected);
            }
        }
    }

    private void refreshFileSize() {
        if(mRule != null && mRule.hasFileSizeConfigured()) {
            float sizeInMB = mRule.getMinFileSize() / 1024.0f / 1024.0f;
            mInputMinFileSize.setText(""+mFormat2.format(sizeInMB));
        } else {
            mInputMinFileSize.setText(""+0);
        }
    }

    /**
     * Refreshes the context trigger list that displays currently configured context trigger
     * information.
     */
    private void refreshContextTriggers() {
        mLayoutContextContent.removeAllViews();
        mTxtNoContextTriggers.setVisibility(View.GONE);

        if(mRule != null && mRule.hasContext()) {
            if(mRule.hasLocationContext()) {
                //Create a row for showing that location is configured
                View row = getActivity().getLayoutInflater()
                        .inflate(R.layout.dialog_rule_context_row, mLayoutContextContent, false);
                row.findViewById(R.id.btnEditRow).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        double lat = mRule.getRuleContext().getLocationContext().getLatitude();
                        double lng = mRule.getRuleContext().getLocationContext().getLongitude();
                        int radius = mRule.getRuleContext().getRadius();
                        showLocationContextDialog(new LatLng(lat,lng), radius, mEditable);
                    }
                });
                ((TextView)row.findViewById(R.id.txtRowTitle)).setText("Location");
                double lat = mRule.getRuleContext().getLocationContext().getLatitude();
                double lng = mRule.getRuleContext().getLocationContext().getLongitude();
                int radius = mRule.getRuleContext().getRadius();

                ((TextView)row.findViewById(R.id.txtRowContent)).setText(
                        mFormat.format(lat) + ", "
                        + mFormat.format(lng) + "\n"
                        + "Radius: " + radius + "m");

                mLayoutContextContent.addView(row);
            }
            if(mRule.hasPlaceContext()) {
                //Create a row for showing that place type context is configured
                View row = getActivity().getLayoutInflater()
                        .inflate(R.layout.dialog_rule_context_row, mLayoutContextContent, false);
                row.findViewById(R.id.btnEditRow).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showPlaceTypeContextDialog();
                    }
                });
                if(!mEditable) {
                    row.findViewById(R.id.btnEditRow).setEnabled(false);
                }
                ((TextView)row.findViewById(R.id.txtRowTitle)).setText("Place types");
                List<PlaceType> types = mRule.getRuleContext().getPlaceTypes();

                TextView txt = ((TextView)row.findViewById(R.id.txtRowContent));
                txt.setText("");
                for (PlaceType type : types) {
                    txt.append(PlaceConstants.getReadableString(type) + "\n");
                }

                mLayoutContextContent.addView(row);
            }
            if(mRule.hasActivityContext())
            {
                //Create a row for showing that activity context is configured for the rule
                View row = getActivity().getLayoutInflater()
                        .inflate(R.layout.dialog_rule_context_row, mLayoutContextContent, false);
                row.findViewById(R.id.btnEditRow).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActivityType activityCtx = mRule.getRuleContext().getActivityContext();
                        showActivityContextDialog(activityCtx);
                    }
                });
                if(!mEditable) {
                    row.findViewById(R.id.btnEditRow).setEnabled(false);
                }
                ((TextView)row.findViewById(R.id.txtRowTitle)).setText("Activity");
                TextView txt = ((TextView)row.findViewById(R.id.txtRowContent));
                txt.setText(ContextUtils
                        .getStringForActivity(mRule.getRuleContext().getActivityContext()));
                mLayoutContextContent.addView(row);
            }
            if(mRule.hasNetworkContext()) {
                //Create a row for showing that network context is configured for the rule
                View row = getActivity().getLayoutInflater()
                        .inflate(R.layout.dialog_rule_context_row, mLayoutContextContent, false);
                row.findViewById(R.id.btnEditRow).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        VNetwork nCtx = mRule.getRuleContext().getNetworkContext();
                        showNetworkContextDialog(nCtx);
                    }
                });
                if(!mEditable) {
                    row.findViewById(R.id.btnEditRow).setEnabled(false);
                }
                ((TextView)row.findViewById(R.id.txtRowTitle)).setText("Network");
                TextView txt = ((TextView)row.findViewById(R.id.txtRowContent));
                txt.setText("");
                VNetwork nCtx = mRule.getRuleContext().getNetworkContext();
                WiFi wCtx = nCtx.getWiFiContext();
                if(wCtx != null && wCtx.isWifiConnected()) {
                    txt.append("WiFi: " + wCtx.getWifiSSID() + "\n");
                }
                CellularNetwork mobCtx = nCtx.getMobileContext();
                if(mobCtx != null && mobCtx.isMobileConnected()) {
                    txt.append("Mobile: " + mobCtx.getMobileNetworkType());
                }
                mLayoutContextContent.addView(row);
            }
            if(mRule.hasNoiseContext()) {
                //Create a row for showing that noise context is configured for the rule
                View row = getActivity().getLayoutInflater()
                        .inflate(R.layout.dialog_rule_context_row, mLayoutContextContent, false);
                row.findViewById(R.id.btnEditRow).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        VNoise noiseCtx = mRule.getRuleContext().getNoiseContext();
                        showNoiseContextDialog(noiseCtx);
                    }
                });
                if(!mEditable) {
                    row.findViewById(R.id.btnEditRow).setEnabled(false);
                }
                ((TextView)row.findViewById(R.id.txtRowTitle)).setText("Noise");
                TextView txt = ((TextView)row.findViewById(R.id.txtRowContent));
                txt.setText("");
                VNoise nCtx = mRule.getRuleContext().getNoiseContext();
                /*if(nCtx.getRMSThreshold() != -1) {
                    txt.append("RMS Threshold: " + mFormat.format(nCtx.getRMSThreshold()) + "\n");
                }*/
                if(nCtx.getDBThreshold() != -1) {
                    txt.append("dB Threshold: " + mFormat.format(nCtx.getDBThreshold()));
                }
                mLayoutContextContent.addView(row);
            }

        } else {
            mTxtNoContextTriggers.setText(R.string.rule_no_context_configured);
            mTxtNoContextTriggers.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Refreshes the decision layout that displays the currently configured storage decision.
     */
    private void refreshDecisions() {
        mTxtNoDecision.setText("");
        if(mRule.getDecisionLayers().size() > 0) {
            int i = 1;
            for (DecisionLayer layer : mRule.getDecisionLayers()) {
                String type = StringUtils.capitalizeOnlyFirstLetter(layer.targetType.toString());
                if (layer.isSpecific) {
                    mTxtNoDecision.append(i + ". Specific " + type);
                } else {
                    mTxtNoDecision.append(i + ". " + type);
                    if (layer.minRadius > 0 && layer.maxRadius > 0) {
                        mTxtNoDecision.append(" " + layer.minRadius + "km - " + layer.maxRadius + "km");
                    } else if (layer.minRadius == 0 && layer.maxRadius > 0) {
                        mTxtNoDecision.append(" " + " <= " + layer.maxRadius + "km");
                    } else if (layer.maxRadius == 0 && layer.minRadius > 0) {
                        mTxtNoDecision.append(" " + " >= " + layer.minRadius + "km");
                    }

                    if (layer.minBwUp > 0 && layer.minBwDown > 0) {
                        mTxtNoDecision.append(" Bw: Up " + layer.minBwUp + "MBit/s, Down " + layer.minBwDown + "MBit/s");
                    } else if (layer.minBwUp == 0 && layer.minBwDown > 0) {
                        mTxtNoDecision.append(" Bw: Down " + layer.minBwDown + "MBit/s");
                    } else if (layer.minBwDown == 0 && layer.minBwUp > 0) {
                        mTxtNoDecision.append(" Bw: Up " + layer.minBwUp + "MBit/s");
                    }
                }
                if(mRule.getDecisionLayers().size() > 1)
                    mTxtNoDecision.append("\n-----------\n");

                ++i;
            }
        } else {
            mTxtNoDecision.setText(R.string.rule_no_decision);
        }
    }

    /*private void refreshConstraints() {
        if(mRule.hasMinimumBandwidthRequirement()) {
            mTxtConstraintsText.setText(
                    getActivity().getString(
                            R.string.bandwidth_text,
                            mRule.getNodeBandwidthDown(),
                            mRule.getNodeBandwidthUp()));
        } else if(mRule.hasMaxUploadDuration()) {
            mTxtConstraintsText.setText(
                    getActivity().getString(
                            R.string.max_upload_time,
                            mRule.getMaxUploadDuration()));
        } else {
            mTxtConstraintsText.setText(R.string.rule_no_constraints);
        }
    }*/
}
