package vstore.android_filebox.rules_elements;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.util.HashMap;
import java.util.Set;

import vstore.android_filebox.R;
import vstore.framework.context.types.place.PlaceConstants;
import vstore.framework.context.types.place.PlaceType;

/**
 *
 */
public class ContextPlaceTypesDialog extends DialogFragment {
    private String[] mTypes;
    private boolean[] mIsChecked;

    /**
     * Create a new instance of the ContextPlaceTypes dialog fragment.
     */
    static ContextPlaceTypesDialog
    newInstance(HashMap<PlaceType, Boolean> placetypes) {
        ContextPlaceTypesDialog f = new ContextPlaceTypesDialog();

        if(placetypes != null) {
            //Bring data into necessary form for displaying the dialog
            Set<PlaceType> keys = placetypes.keySet();
            f.mTypes = new String[keys.size()];
            f.mIsChecked = new boolean[keys.size()];
            int i = 0;
            for(PlaceType p : keys) {
                f.mTypes[i] = PlaceConstants.getReadableString(p);
                f.mIsChecked[i] = placetypes.get(p);
                i++;
            }
        } else {
            return null;
        }

        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.title_configure_context_placetypes)
                .setMultiChoiceItems(mTypes, mIsChecked,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int position, boolean checked) {
                                mIsChecked[position] = checked;
                            }
                        })
                .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent();
                        intent.putExtra("types", mTypes);
                        intent.putExtra("checked", mIsChecked);
                        getTargetFragment().onActivityResult(
                                getTargetRequestCode(),
                                CreateRuleDialog.OK_REQUEST,
                                intent);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) { }
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
}
