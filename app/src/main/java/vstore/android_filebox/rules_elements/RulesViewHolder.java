package vstore.android_filebox.rules_elements;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.HashMap;

import vstore.android_filebox.R;

//import static vstore.vstore-android-filebox.R.id.txtRuleCardConstraints;


class RulesViewHolder extends RecyclerView.ViewHolder {

    private View mParent;

    TextView mTxtCardTitle;
    TextView mTxtFileTypes;
    TextView mTxtFileSize;
    TextView mTxtContext;
    //TextView mTxtRuleCardConstraintsTitle;
    //TextView mTxtRuleCardConstraints;
    TextView mTxtDecision;
    RelativeLayout mLayoutPublic;
    RelativeLayout mLayoutPrivate;
    LinearLayout mLayoutDaysSmall;
    LinearLayout mLayoutTimeSmall;
    LinearLayout mCardlayoutMinFileSize;
    TextView mTimeStart;
    TextView mTimeEnd;
    TextView mTxtRuleScore;

    private HashMap<Integer, TextView> mDayViews;

    RulesViewHolder(View itemView) {
        super(itemView);
        mParent = itemView;
        mTxtCardTitle = (TextView)  itemView.findViewById(R.id.txtCardTitle);
        mTxtFileTypes = (TextView) itemView.findViewById(R.id.txtFileTypes);
        mCardlayoutMinFileSize = (LinearLayout) itemView.findViewById(R.id.cardlayoutMinFileSize);
        mTxtFileSize = (TextView) itemView.findViewById(R.id.txtRuleMinFileSize);
        mTxtContext = (TextView) itemView.findViewById(R.id.txtContext);
        //mTxtRuleCardConstraintsTitle = (TextView) itemView.findViewById(R.id.txtRuleCardConstraintsTitle);
        //mTxtRuleCardConstraints = (TextView) itemView.findViewById(txtRuleCardConstraints);
        mTxtDecision = (TextView) itemView.findViewById(R.id.txtDecision);
        mLayoutPublic = (RelativeLayout) itemView.findViewById(R.id.layoutPublic);
        mLayoutPrivate = (RelativeLayout) itemView.findViewById(R.id.layoutPrivate);
        mLayoutDaysSmall = (LinearLayout) itemView.findViewById(R.id.layoutDaysSmall);
        mLayoutTimeSmall = (LinearLayout) itemView.findViewById(R.id.layoutTimeSmall);
        mTimeStart = (TextView) itemView.findViewById(R.id.txtSmallTimeStart);
        mTimeEnd = (TextView) itemView.findViewById(R.id.txtSmallTimeEnd);
        mTxtRuleScore = (TextView) itemView.findViewById(R.id.txtRuleScore);

        mDayViews = new HashMap<>();
        mDayViews.put(1, (TextView)mParent.findViewById(R.id.txtSmallMon));
        mDayViews.put(2, (TextView)mParent.findViewById(R.id.txtSmallTue));
        mDayViews.put(3, (TextView)mParent.findViewById(R.id.txtSmallWed));
        mDayViews.put(4, (TextView)mParent.findViewById(R.id.txtSmallThur));
        mDayViews.put(5, (TextView)mParent.findViewById(R.id.txtSmallFri));
        mDayViews.put(6, (TextView)mParent.findViewById(R.id.txtSmallSat));
        mDayViews.put(7, (TextView)mParent.findViewById(R.id.txtSmallSun));
    }

    public void setDayActive(int d) {
        if(d > 0 && d < 8) {
            mDayViews.get(d).setTextColor(Color.BLACK);
        }
    }

    public void resetDays(Context c) {
        for(TextView d : mDayViews.values()) {
            d.setTextColor(c.getResources().getColor(R.color.half_black));
        }
    }

    public void resetCard(Context c) {
        mTxtCardTitle.setText("");
        mTxtCardTitle.setBackgroundColor(c.getResources().getColor(R.color.colorPrimary_light));
        mTxtFileTypes.setText("");
        mCardlayoutMinFileSize.setVisibility(View.GONE);
        mTxtFileSize.setText("");
        mTxtContext.setText("");
        //mTxtRuleCardConstraintsTitle.setVisibility(View.GONE);
        //mTxtRuleCardConstraints.setVisibility(View.GONE);
        //mTxtRuleCardConstraints.setText("");
        mTxtDecision.setText("");
        mLayoutPublic.setVisibility(View.GONE);
        mLayoutPrivate.setVisibility(View.GONE);
        mLayoutDaysSmall.setVisibility(View.GONE);
        mLayoutTimeSmall.setVisibility(View.GONE);
        mTimeStart.setText("");
        mTimeEnd.setText("");
        mTxtRuleScore.setText("");
        resetDays(c);
    }
}
