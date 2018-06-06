package vstore.android_filebox.search_fragment;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import vstore.android_filebox.R;

/**
 * The ViewHolder to hold a reference to the views of a row
 */
class ThumbViewHolder extends RecyclerView.ViewHolder {
    private ImageView mThumbnailView;
    private TextView mTxtFileType;
    private RelativeLayout mLoaderLayout;
    private ProgressBar mProgressBar;
    private TextView mTxtPercent;
    private TextView mTxtPostedTime;
    private TextView mTxtFilename;

    ThumbViewHolder(View itemView) {
        super(itemView);

        mThumbnailView = (ImageView) itemView.findViewById(R.id.thumbnailView);
        mTxtFileType = (TextView) itemView.findViewById(R.id.txtFileType);
        mLoaderLayout = (RelativeLayout) itemView.findViewById(R.id.loadingPanel);
        mTxtPostedTime = (TextView) itemView.findViewById(R.id.txtPostedTime);
        mTxtFilename = (TextView) itemView.findViewById(R.id.txtFileName);
    }

    ImageView getThumbnailView() {
        return mThumbnailView;
    }

    void setFileTypeText(String text) {
        mTxtFileType.setText(text);
    }

    TextView getTextPostedTime() { return mTxtPostedTime; }

    TextView getTextFilename() { return mTxtFilename; }

    public void resetViewHolder() {
        mThumbnailView.setImageDrawable(null);
        mTxtFileType.setText("");
        mLoaderLayout.setVisibility(View.GONE);
        mTxtPostedTime.setText("");
        mTxtFilename.setText("");
    }
}