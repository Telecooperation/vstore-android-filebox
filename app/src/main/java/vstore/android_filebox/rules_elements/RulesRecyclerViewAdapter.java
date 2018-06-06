package vstore.android_filebox.rules_elements;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

import vstore.android_filebox.R;
import vstore.framework.rule.VStoreRule;

public class RulesRecyclerViewAdapter extends RecyclerView.Adapter<RulesViewHolder> {
    //Holds the elements for the recycler view
    private List<VStoreRule> mRuleList;
    private Context mContext;

    DecimalFormat df = new DecimalFormat("#.##");

    public RulesRecyclerViewAdapter(Context context, List<VStoreRule> itemList) {
        mRuleList = itemList;
        mContext = context;
        df.setRoundingMode(RoundingMode.CEILING);
    }

    @Override
    public RulesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rule_card, parent, false);
        //Set tick drawables in a compatible way
        Drawable dTick;
        Resources ctw = parent.getContext().getResources();
        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            dTick = ctw.getDrawable(R.drawable.ic_check_black_24dp, parent.getContext().getTheme());
        } else {
            dTick = VectorDrawableCompat.create(ctw, R.drawable.ic_check_black_24dp, parent.getContext().getTheme());
        }
        ((ImageView)layoutView.findViewById(R.id.imgTick1)).setImageDrawable(dTick);
        ((ImageView)layoutView.findViewById(R.id.imgTick2)).setImageDrawable(dTick);

        RulesViewHolder rcv = new RulesViewHolder(layoutView);
        return rcv;
    }

    @Override
    public void onBindViewHolder(RulesViewHolder holder, int position) {
        holder.resetCard(mContext);
        VStoreRule r = mRuleList.get(position);
        //Display name for the card
        holder.mTxtCardTitle.setText(r.getName());
        if(!r.isGlobal()) {
            holder.mTxtCardTitle.setBackgroundColor(
                    mContext.getResources().getColor(R.color.colorPrimary_light));
        } else {
            holder.mTxtCardTitle.setBackgroundColor(
                    mContext.getResources().getColor(R.color.lightorange));
        }
        //Display public/private
        switch(r.getSharingDomain()) {
            case -1:
                holder.mLayoutPublic.setVisibility(View.VISIBLE);
                holder.mLayoutPrivate.setVisibility(View.VISIBLE);
                break;
            case 0:
                holder.mLayoutPublic.setVisibility(View.VISIBLE);
                holder.mLayoutPrivate.setVisibility(View.GONE);
                break;
            case 1:
                holder.mLayoutPublic.setVisibility(View.GONE);
                holder.mLayoutPrivate.setVisibility(View.VISIBLE);
                break;
            default:
                holder.mLayoutPublic.setVisibility(View.VISIBLE);
                holder.mLayoutPrivate.setVisibility(View.VISIBLE);
                break;
        }
        //Display days and time
        holder.resetDays(mContext);
        if(r.getWeekdays().size() > 0) {
            for (int d : r.getWeekdays()) {
                holder.setDayActive(d);
            }
            holder.mLayoutDaysSmall.setVisibility(View.VISIBLE);
        }
        if(!(r.getStartHour() == 0 && r.getStartMinutes() == 0 && r.getEndHour() == 0 && r.getEndMinutes() == 0)) {
            //Add leading 0 to the time if necessary
            String a = ((r.getStartHour() < 10) ? "0" : "");
            String b = ((r.getStartMinutes() < 10) ? "0" : "");
            holder.mTimeStart.setText(a + r.getStartHour() + ":" + b + r.getStartMinutes());
            //Add leading 0 to the time if necessary
            a = ((r.getEndHour() < 10) ? "0" : "");
            b = ((r.getEndMinutes() < 10) ? "0" : "");
            holder.mTimeEnd.setText(a + r.getEndHour() + ":" + b + r.getEndMinutes());
            holder.mLayoutTimeSmall.setVisibility(View.VISIBLE);
        }
        //Display mime types for the card
        if(r.getMimeTypes() != null && r.getMimeTypes().size() > 0) {
            holder.mTxtFileTypes.setText("");
            int i = 0;
            for(String s : r.getMimeTypes()) {
                if(i==0) {
                    holder.mTxtFileTypes.append(s);
                } else {
                    holder.mTxtFileTypes.append("\n" + s);
                }
                if(i == 4) {
                    holder.mTxtFileTypes.append("\n...");
                    break;
                }
                ++i;
            }
        } else {
            holder.mTxtFileTypes.setText(mContext.getString(R.string.rule_no_mime_triggers));
        }
        //Display file size (if configured)
        if(r.hasFileSizeConfigured()) {
            holder.mTxtFileSize.setText(df.format(r.getMinFileSize() / 1024.0f / 1024.0f) + " MB");
            holder.mCardlayoutMinFileSize.setVisibility(View.VISIBLE);
        }
        //Display context information for the card
        if(r.hasContext()) {
            holder.mTxtContext.setText("");
            if(r.hasLocationContext())
                holder.mTxtContext.append("Location\n");
            if(r.hasPlaceContext())
                holder.mTxtContext.append("Place\n");
            if(r.hasActivityContext())
                holder.mTxtContext.append("Activity\n");
            if(r.hasNetworkContext())
                holder.mTxtContext.append("Network\n");
            if(r.hasNoiseContext())
                holder.mTxtContext.append("Noise\n");
            String text = holder.mTxtContext.getText().toString();
            if(!text.equals("")) {
                holder.mTxtContext.setText(text.substring(0, text.length()-1));
            }
        } else {
            holder.mTxtContext.setText(mContext.getString(R.string.rule_no_context_triggers));
        }
        //Display node constraints (e.g. minimum bandwidth or upload duration)
        /*if(r.hasMinimumBandwidthRequirement()) {
            holder.mTxtRuleCardConstraintsTitle.setVisibility(View.VISIBLE);
            holder.mTxtRuleCardConstraints.setText(
                    mContext.getString(
                        R.string.bandwidth_text,
                        r.getNodeBandwidthDown(),
                        r.getNodeBandwidthUp()));
            holder.mTxtRuleCardConstraints.setVisibility(View.VISIBLE);
        } else if(r.hasMaxUploadDuration()) {
            holder.mTxtRuleCardConstraintsTitle.setVisibility(View.VISIBLE);
            holder.mTxtRuleCardConstraints.setText(
                    mContext.getString(
                            R.string.max_upload_time,
                            r.getMaxUploadDuration()));
            holder.mTxtRuleCardConstraints.setVisibility(View.VISIBLE);
        }*/

        //Display decision information for the card
        int size = r.getDecisionLayers().size();
        if(size > 0) {
            if(size == 1) {
                holder.mTxtDecision.setText("1 decision layer");
            } else {
                holder.mTxtDecision.setText(r.getDecisionLayers().size() + " decision layers");
            }
        } else {
            holder.mTxtDecision.setText(mContext.getString(R.string.rule_no_decision));
        }
        //Display detail score on the card
        holder.mTxtRuleScore.setText(df.format(r.getDetailScore()) + "%");
    }

    @Override
    public int getItemCount() {
        return this.mRuleList.size();
    }
}
