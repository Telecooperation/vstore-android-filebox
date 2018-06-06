package vstore.android_filebox.rules_elements;


import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import vstore.android_filebox.R;
import vstore.framework.context.types.noise.VNoise;

public class ContextNoiseDialog extends DialogFragment {

    private VNoise mNoiseContext;

    //private EditText mInputRMSThreshold;
    private EditText mInputDBThreshold;

    /**
     * Creates a new instance of this noise context dialog fragment.
     * @param noiseContext The initial noise context to display.
     * @return An instance of this dialog.
     */
    public static ContextNoiseDialog newInstance(VNoise noiseContext) {
        ContextNoiseDialog noiseDialog = new ContextNoiseDialog();
        if(noiseContext != null) {
            noiseDialog.mNoiseContext = noiseContext;
        }
        return noiseDialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View dialogLayout = getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_context_noise, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //mInputRMSThreshold = (EditText) dialogLayout.findViewById(R.id.inputRMSThreshold);
        mInputDBThreshold = (EditText) dialogLayout.findViewById(R.id.inputDBThreshold);

        setInitialValues();
        builder.setView(dialogLayout)
                .setTitle(R.string.title_configure_context_noise)
                .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Get the values from the text fields
                        try {
                            //int rmsThresh = Integer.parseInt(mInputRMSThreshold.getText().toString());
                            int dbThresh = Integer.parseInt(mInputDBThreshold.getText().toString());
                            //mNoiseContext.setRMSThreshold(rmsThresh);
                            mNoiseContext.setDbThreshold(dbThresh);
                            Intent intent = new Intent();
                            intent.putExtra("vnoisejson", mNoiseContext.getJson().toString());
                            getTargetFragment().onActivityResult(
                                    getTargetRequestCode(),
                                    CreateRuleDialog.OK_REQUEST,
                                    intent);
                        } catch(NumberFormatException e) {
                            Toast.makeText(getActivity(), "Invalid value entered!", Toast.LENGTH_SHORT).show();
                        }
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
        if(mNoiseContext != null) {
            //mInputRMSThreshold.append(Double.toString(mNoiseContext.getRMSThreshold()));
            mInputDBThreshold.setText(Integer.toString(mNoiseContext.getDBThreshold()));
        } else {
            //Create a default noise configuration for starting
            mNoiseContext = new VNoise(0, 0, 0, 0);
        }
    }
}
