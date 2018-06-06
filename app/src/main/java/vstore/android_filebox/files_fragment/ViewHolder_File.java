package vstore.android_filebox.files_fragment;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import vstore.android_filebox.R;

/**
 * The ViewHolder to hold a reference to the views of a row
 */
class ViewHolder_File extends RecyclerView.ViewHolder {
    private ImageView mThumbnailView;
    private ImageView mUploadPending;
    private TextView mTxtFileType;
    private TextView mTxtFilename;

    ViewHolder_File(View itemView) {
        super(itemView);

        mThumbnailView = (ImageView) itemView.findViewById(R.id.thumbnailView);
        mUploadPending = (ImageView) itemView.findViewById(R.id.imgUploadPending);
        mTxtFileType = (TextView) itemView.findViewById(R.id.txtFileType);
        mTxtFilename = (TextView) itemView.findViewById(R.id.txtFileName);
    }

    ImageView getThumbnailView() {
        return mThumbnailView;
    }

    public void setThumbnailView(ImageView v) {
        mThumbnailView = v;
    }

    void setUploadPending(boolean state) {
        if(state) {
            mUploadPending.setVisibility(View.VISIBLE);
        } else {
            mUploadPending.setVisibility(View.GONE);
        }
    }

    void setFileTypeText(String text) {
        mTxtFileType.setText(text);
    }
    void setFilenameText(String text) { mTxtFilename.setText(text); }
}