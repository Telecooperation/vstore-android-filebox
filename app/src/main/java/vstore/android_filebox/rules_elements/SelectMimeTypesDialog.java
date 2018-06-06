package vstore.android_filebox.rules_elements;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.util.HashMap;
import java.util.Set;

import vstore.android_filebox.R;

public class SelectMimeTypesDialog extends DialogFragment {
    private String[] mMimeTypes;
    private boolean[] mIsChecked;

    /**
     * Create a new instance of the CreateRuleDialog.
     *
     * @param checkedTypes Needs to be passed a map of types. The key is the mime type, the value
     *                     denotes if the mime type is selected for the decision rule.
     */
    static SelectMimeTypesDialog newInstance(HashMap<String, Boolean> checkedTypes) {
        SelectMimeTypesDialog f = new SelectMimeTypesDialog();

        if(checkedTypes != null) {
            //Bring data into necessary form for displaying the dialog
            Set<String> keys = checkedTypes.keySet();
            f.mMimeTypes = new String[keys.size()];
            f.mIsChecked = new boolean[keys.size()];
            int i = 0;
            for(String type : keys) {
                f.mMimeTypes[i] = type;
                f.mIsChecked[i] = checkedTypes.get(type);
                i++;
            }
        } else {
            return null;
        }
        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.title_select_mime)
                .setMultiChoiceItems(mMimeTypes, mIsChecked,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            public void onClick(DialogInterface dialog, int position, boolean isChecked) {
                                mIsChecked[position] = isChecked;
                            }
                        })
                .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent();
                        intent.putExtra("mimetypes", mMimeTypes);
                        intent.putExtra("checked", mIsChecked);
                        getTargetFragment().onActivityResult(getTargetRequestCode(), 1, intent);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                    }
                });

        return builder.create();
    }

}
