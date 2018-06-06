package vstore.android_filebox.rules_elements;


import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.HashMap;

import vstore.android_filebox.R;
import vstore.android_filebox.utils.ContextUtils;
import vstore.framework.context.types.activity.ActivityType;
import vstore.framework.context.types.activity.VActivity;

/**
 * This Dialog displays a list to chose the activity for the rule.
 */
public class ContextActivityDialog extends DialogFragment {

    /**
     * This field holds the activity given to the dialog when it was created.
     * Used for preselecting a given activity.
     */
    private ActivityType mCurrentActivityId;

    /**
     * Get a new instance of the ContextActivity dialog fragment
     * @param activity
     * @return The instance of the ContextActivity dialog fragment.
     */
    public static ContextActivityDialog newInstance(ActivityType activity) {
        ContextActivityDialog dialog = new ContextActivityDialog();
        dialog.mCurrentActivityId = activity;

        return dialog;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //Get all activities from the framework
        HashMap<String, ActivityType> allActivities = VActivity.getTypes();
        //Bring items into an array structure so that the dialog accepts them
        final Item[] items = new Item[allActivities.keySet().size()];
        int i = 0;
        for(String s : allActivities.keySet()) {
            ActivityType activityId = allActivities.get(s);
            boolean selected = ((mCurrentActivityId == activityId) ? true : false);
            int icon = ContextUtils.getIconForActivity(activityId);
            String text = ContextUtils.getStringForActivity(activityId);
            items[i] = new Item(activityId, text, icon, selected);
            i++;
        }


        /**
         * Create a custom list adapter to make it possible to display an icon along with the
         * text that describes the activity.
         * select_dialog_item and text1 are default values from Android.
         */
        ListAdapter adapter = new ArrayAdapter<Item>(
                getActivity(), android.R.layout.select_dialog_item, android.R.id.text1, items)
        {
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                TextView tv = (TextView)v.findViewById(android.R.id.text1);
                //Put the image on the TextView
                tv.setCompoundDrawablesWithIntrinsicBounds(items[position].getIcon(), 0, 0, 0);
                //Add space between image and text
                int padding = (int) (20 * getResources().getDisplayMetrics().density + 0.5f);
                tv.setCompoundDrawablePadding(padding);
                //Color the currently selected activity
                if(items[position].isSelected()
                        && tv.getText().equals(
                                ContextUtils.getStringForActivity(items[position].getActivityId()))) {
                    tv.setTextColor(getResources().getColor(R.color.colorAccent));
                } else {
                    tv.setTextColor(getResources().getColor(R.color.black));
                }
                return v;
            }
        };

        builder.setTitle(R.string.title_configure_context_activity)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Check if the selected index is valid
                        if(i < items.length) {
                            //Return the new activity id back to the calling fragment
                            Intent intent = new Intent();
                            intent.putExtra("newActivity", items[i].getActivityId().toString());
                            getTargetFragment().onActivityResult(
                                    getTargetRequestCode(),
                                    CreateRuleDialog.OK_REQUEST,
                                    intent);
                        }
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


    /**
     * This internal class just represents a row in the dialog containing the text of an activity
     * as well as an icon corresponding to this activity.
     */
    private static class Item {
        private final ActivityType mActivityId;
        private final String mText;
        private final int mIcon;
        private final boolean mSelected;

        public Item(ActivityType activityId, String text, Integer icon, boolean selected) {
            this.mActivityId = activityId;
            this.mText = text;
            this.mIcon = icon;
            mSelected = selected;
        }

        public ActivityType getActivityId() {
            return mActivityId;
        }
        public String getText() {
            return mText;
        }
        public int getIcon() {
            return mIcon;
        }
        public boolean isSelected() {
            return mSelected;
        }

        @Override
        public String toString() {
            return mText;
        }
    }

}
