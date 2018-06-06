package vstore.android_filebox.search_fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import vstore.android_filebox.R;
import vstore.framework.context.ContextFilter;

/**
 * This dialog enables the Search Activity (or Contextual Files Activity) to define
 * by what type of context the files should be filtered.
 */
public class FilterDialog extends DialogFragment {

    private ContextFilter mContextFilter;
    private boolean mEnableMostLikelyPlaceFilter;

    private CheckBox mCheckboxLocationFilter;
    private LinearLayout mLayoutRadius;
    private EditText mInputRadiusFilter;
    private CheckBox mCheckboxPlaceFilter;
    private CheckBox mCheckboxActivityFilter;
    private CheckBox mCheckboxNetworkFilter;
    private CheckBox mCheckboxNoiseFilter;
    private CheckBox mCheckboxWeekdayFilter;
    private CheckBox mCheckboxTimeOfDayFilter;
    private TextView mTxtTimespanInfo;
    private EditText mInputTimespan;

    public static FilterDialog newInstance(ContextFilter filter, boolean enableMostlikelyPlaceFilter) {
        FilterDialog d = new FilterDialog();
        if(filter == null) {
            filter = new ContextFilter();
        }
        d.mContextFilter = filter;
        d.mEnableMostLikelyPlaceFilter = enableMostlikelyPlaceFilter;

        return d;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int theme = R.style.AppDialogTheme;
        setStyle(STYLE_NORMAL, theme);
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View d = getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_filter, null);

        mCheckboxLocationFilter = (CheckBox) d.findViewById(R.id.checkboxLocationFilter);
        mLayoutRadius = (LinearLayout) d.findViewById(R.id.layoutRadius);
        mCheckboxLocationFilter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mContextFilter.enableLocation(b);
                if(b) {
                    mLayoutRadius.setVisibility(View.VISIBLE);
                } else {
                    mLayoutRadius.setVisibility(View.GONE);
                }
            }
        });
        mInputRadiusFilter = (EditText) d.findViewById(R.id.inputRadiusFilter);
        mCheckboxPlaceFilter = (CheckBox) d.findViewById(R.id.checkboxPlaceFilter);
        mCheckboxPlaceFilter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mContextFilter.enableMostLikelyPlace(b);
            }
        });
        if(!mEnableMostLikelyPlaceFilter) {
            mCheckboxPlaceFilter.setChecked(false);
            mCheckboxPlaceFilter.setEnabled(false);
        }
        mCheckboxActivityFilter = (CheckBox) d.findViewById(R.id.checkboxActivityFilter);
        mCheckboxActivityFilter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mContextFilter.enableActivity(b);
            }
        });
        mCheckboxNetworkFilter = (CheckBox) d.findViewById(R.id.checkboxNetworkFilter);
        mCheckboxNetworkFilter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mContextFilter.enableNetwork(b);
            }
        });
        mCheckboxNoiseFilter = (CheckBox) d.findViewById(R.id.checkboxNoiseFilter);
        mCheckboxNoiseFilter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mContextFilter.enableNoise(b);
            }
        });

        mCheckboxWeekdayFilter = (CheckBox) d.findViewById(R.id.checkboxWeekdayFilter);
        mCheckboxWeekdayFilter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mContextFilter.enableWeekday(b);
            }
        });

        mTxtTimespanInfo = (TextView) d.findViewById(R.id.txtTimespanInfo);
        mInputTimespan = (EditText) d.findViewById(R.id.inputTimespan);
        mCheckboxTimeOfDayFilter = (CheckBox) d.findViewById(R.id.checkboxTimeOfDayFilter);
        mCheckboxTimeOfDayFilter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mContextFilter.enableTimeOfDay(b);
                if(b) {
                    mInputTimespan.setVisibility(View.VISIBLE);
                    mTxtTimespanInfo.setVisibility(View.VISIBLE);
                } else {
                    mInputTimespan.setVisibility(View.GONE);
                    mTxtTimespanInfo.setVisibility(View.GONE);
                }
            }
        });

        Button btnResetFilter = (Button) d.findViewById(R.id.btnResetFilter);
        btnResetFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mContextFilter = ContextFilter.getDefaultFilter();
                setValues();
            }
        });

        setValues();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String title = getString(R.string.title_filter);
        builder.setView(d)
                .setTitle(title)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            mContextFilter.setRadius(Integer.parseInt(mInputRadiusFilter.getText().toString()));
                        } catch(NumberFormatException ex) {
                            mContextFilter.setRadius(120);
                        }
                        try {
                            mContextFilter.setTimeSpanMS(Integer.parseInt(mInputTimespan.getText().toString()) * 60 * 60 * 1000);
                        } catch(NumberFormatException ex) {
                            mContextFilter.setTimeSpanMS(3*60*60*1000);
                        }
                        Intent intent = new Intent();
                        intent.putExtra("filterconfig", mContextFilter.getJson().toString());
                        getTargetFragment().onActivityResult(
                                getTargetRequestCode(),
                                SearchFragment.OK_REQUEST,
                                intent);
                    }
                })
                .setNegativeButton(R.string.cancel, null);

        return builder.create();
    }

    private void setValues() {
        mCheckboxLocationFilter.setChecked(mContextFilter.isLocationEnabled());
        if(mContextFilter.isLocationEnabled()) {
            mLayoutRadius.setVisibility(View.VISIBLE);
        } else {
            mLayoutRadius.setVisibility(View.GONE);
        }
        mInputRadiusFilter.setText(""+ mContextFilter.getRadius());
        mCheckboxPlaceFilter.setChecked(mContextFilter.isPlaceEnabled());
        mCheckboxActivityFilter.setChecked(mContextFilter.isActivityEnabled());
        mCheckboxNetworkFilter.setChecked(mContextFilter.isNetworkEnabled());
        mCheckboxNoiseFilter.setChecked(mContextFilter.isNoiseEnabled());
        mCheckboxWeekdayFilter.setChecked(mContextFilter.isWeekdayEnabled());
        mCheckboxTimeOfDayFilter.setChecked(mContextFilter.isTimeOfDayEnabled());
        mInputTimespan.setText(""+(mContextFilter.getTimeSpanMS()/1000/60/60));
    }
}
