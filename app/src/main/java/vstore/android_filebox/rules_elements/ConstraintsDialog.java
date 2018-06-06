package vstore.android_filebox.rules_elements;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import vstore.android_filebox.R;
import vstore.framework.rule.NodeConstraints;

/**
 * (Currently not used in the framework.)
 */
public class ConstraintsDialog extends DialogFragment {
    private NodeConstraints mConstraints;

    private CheckBox mCheckBoxBandwidth;
    private RelativeLayout mLayoutBandwidth;
    private CheckBox mCheckBoxUploadDuration;
    private RelativeLayout mLayoutDuration;
    private EditText mInputBwDown;
    private EditText mInputBwUp;
    private EditText mInputUploadDuration;

    public static ConstraintsDialog newInstance(NodeConstraints constraints) {
        ConstraintsDialog dialog = new ConstraintsDialog();
        if(constraints != null) {
            dialog.mConstraints = constraints;
            return dialog;
        }
        return null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_rule_constraints, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        mCheckBoxBandwidth = (CheckBox) view.findViewById(R.id.checkboxBandwidth);
        mLayoutBandwidth = (RelativeLayout) view.findViewById(R.id.layoutBandwidth);
        mCheckBoxBandwidth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if(checked) {
                    mLayoutBandwidth.setVisibility(View.VISIBLE);
                } else {
                    mLayoutBandwidth.setVisibility(View.GONE);
                }
            }
        });
        mCheckBoxUploadDuration = (CheckBox) view.findViewById(R.id.checkboxUploadDuration);
        mLayoutDuration = (RelativeLayout) view.findViewById(R.id.layoutUploadDuration);
        mCheckBoxUploadDuration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if(checked) {
                    mLayoutDuration.setVisibility(View.VISIBLE);
                } else {
                    mLayoutDuration.setVisibility(View.GONE);
                }
            }
        });

        mInputBwDown = (EditText) view.findViewById(R.id.inputBandwidthDown);
        mInputBwUp = (EditText) view.findViewById(R.id.inputBandwidthUp);
        mInputUploadDuration = (EditText) view.findViewById(R.id.inputUploadDuration);

        builder.setView(view)
                .setTitle(R.string.title_rule_constraints)
                .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Try to parse entered numbers
                        if(mCheckBoxUploadDuration.isChecked()) {
                            try {
                                mConstraints.maxUploadDuration = Integer.parseInt(mInputUploadDuration.getText().toString());
                            } catch (IllegalArgumentException ex) {
                                Toast.makeText(getActivity(), R.string.invalid_duration, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            mConstraints.maxUploadDuration = 0;
                        }

                        //Put results into intent and return
                        Intent intent = new Intent();
                        intent.putExtra("upload_duration", mConstraints.maxUploadDuration);
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

        setInitialValues();
        return builder.create();
    }

    private void setInitialValues() {
        //Set initial values if a configuration was provided when opening the dialog
        if(mConstraints.maxUploadDuration != 0) {
            mCheckBoxUploadDuration.setChecked(true);
        } else {
            mCheckBoxUploadDuration.setChecked(false);
        }
    }
}
