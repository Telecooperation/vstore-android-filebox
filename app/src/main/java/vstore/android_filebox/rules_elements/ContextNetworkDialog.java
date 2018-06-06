package vstore.android_filebox.rules_elements;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import vstore.android_filebox.R;
import vstore.android_filebox.utils.ContextUtils;
import vstore.framework.context.types.network.VNetwork;
import vstore.framework.context.types.network.cellular.CellularNetwork;
import vstore.framework.context.types.network.wifi.WiFi;

/**
 * This dialog presents an interface for configuring the network context that has to be given
 * for a rule.
 */
public class ContextNetworkDialog extends DialogFragment {

    private VNetwork mNetworkContext;

    private CheckBox mCheckBoxWifi;
    private RelativeLayout mLayoutSsid;
    private EditText mInputWifiSsid;
    private CheckBox mCheckBoxMobile;
    private RelativeLayout mLayoutMobileFast;
    private Spinner mSpinnerMobileType;

    public static ContextNetworkDialog newInstance(VNetwork network) {
        ContextNetworkDialog netDialog = new ContextNetworkDialog();
        if(network != null) {
            netDialog.mNetworkContext = network;
        }
        return netDialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View dialogLayout = getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_context_network, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        mCheckBoxWifi = (CheckBox) dialogLayout.findViewById(R.id.checkboxWifi);
        mLayoutSsid = (RelativeLayout) dialogLayout.findViewById(R.id.layoutSsid);
        mInputWifiSsid = (EditText) dialogLayout.findViewById(R.id.inputWifiSsid);
        mCheckBoxWifi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    mLayoutSsid.setVisibility(View.VISIBLE);
                    mNetworkContext.setWiFiContext(new WiFi(true, ""));
                } else {
                    mLayoutSsid.setVisibility(View.GONE);
                    mInputWifiSsid.setText("");
                    mNetworkContext.setWiFiContext(null);
                }
            }
        });
        mLayoutMobileFast = (RelativeLayout) dialogLayout.findViewById(R.id.layoutMobileFast);
        mCheckBoxMobile = (CheckBox) dialogLayout.findViewById(R.id.checkboxMobile);
        mCheckBoxMobile.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    mLayoutMobileFast.setVisibility(View.VISIBLE);
                    mNetworkContext.setMobileContext(new CellularNetwork(true, false, null));
                } else {
                    mLayoutMobileFast.setVisibility(View.GONE);
                    mNetworkContext.setMobileContext(null);
                }
            }
        });
        mSpinnerMobileType = (Spinner) dialogLayout.findViewById(R.id.spinnerMobileType);
        ArrayAdapter<CharSequence> spinAdapter = new ArrayAdapter<CharSequence>(
                getActivity(),
                android.R.layout.simple_spinner_item,
                ContextUtils.getMobileStrings());
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerMobileType.setAdapter(spinAdapter);
        mSpinnerMobileType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String type = (String)adapterView.getItemAtPosition(i);
                CellularNetwork.MobileType t = CellularNetwork.MobileType.valueOf(type);
                mNetworkContext.getMobileContext().setMobileNetworkType(t);
                switch(type) {
                    case "3G":
                    case "3.5G":
                    case "4G":
                        mNetworkContext.getMobileContext().setMobileNetworkFast(true);
                        break;

                    default:
                    case "2G":
                        mNetworkContext.getMobileContext().setMobileNetworkFast(false);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        setInitialValues();
        builder.setView(dialogLayout)
                .setTitle(R.string.title_configure_context_network)
                .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        WiFi wifiCtx = mNetworkContext.getWiFiContext();
                        CellularNetwork mobCtx = mNetworkContext.getMobileContext();
                        if(wifiCtx != null) {
                            //Get entered WiFi SSID if wifi connected was selected
                            wifiCtx.setWifiSSID(mInputWifiSsid.getText().toString());
                        }
                        Intent intent = new Intent();
                        if(wifiCtx == null && mobCtx == null) {
                            intent.putExtra("vnetworkjson", "");
                        }
                        else {
                            //Put results into intent and return
                            intent.putExtra("vnetworkjson", mNetworkContext.getJson().toString());
                        }
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
                })
                .setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent();
                        intent.putExtra("delete", true);
                        getTargetFragment().onActivityResult(
                                getTargetRequestCode(),
                                CreateRuleDialog.OK_REQUEST,
                                intent);
                    }
                });

        return builder.create();
    }

    private void setInitialValues() {
        //Set initial values if a configuration was provided when opening the dialog
        if(mNetworkContext != null) {
            WiFi wifiCtx = mNetworkContext.getWiFiContext();
            if(wifiCtx != null && wifiCtx.isWifiConnected()) {
                mCheckBoxWifi.setChecked(true);
                mInputWifiSsid.setText(wifiCtx.getWifiSSID());
            }
            CellularNetwork mobCtx = mNetworkContext.getMobileContext();
            if(mobCtx != null && mobCtx.isMobileConnected()) {
                mCheckBoxMobile.setChecked(true);
                //Select first by default
                mSpinnerMobileType.setSelection(0);
                if(mobCtx.getMobileNetworkType() != null) {
                    String mobType = mobCtx.getMobileNetworkType().name();
                    for (int i = 0; i < mSpinnerMobileType.getAdapter().getCount(); i++) {
                        if (mSpinnerMobileType.getItemAtPosition(i).toString()
                                .equals(mobType)) {
                            mSpinnerMobileType.setSelection(i);
                            break;
                        }
                    }
                }
            }
        } else {
            //Create a default network configuration for starting
            mNetworkContext = new VNetwork(null, null);
        }
    }
}
