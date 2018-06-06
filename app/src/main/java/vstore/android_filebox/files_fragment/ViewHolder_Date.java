package vstore.android_filebox.files_fragment;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import vstore.android_filebox.R;

class ViewHolder_Date extends RecyclerView.ViewHolder {

    private TextView mDateTitle;

    ViewHolder_Date(View v) {
        super(v);
        mDateTitle = (TextView) v.findViewById(R.id.txtDateTitle);
    }

    TextView getTitleView() {
        return mDateTitle;
    }

    public void setTitleView(TextView v) {
        this.mDateTitle = v;
    }
}