package vstore.android_filebox.search_fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import vstore.android_filebox.R;
import vstore.android_filebox.utils.StringUtils;
import vstore.framework.VStore;
import vstore.framework.file.MatchingResultRow;
import vstore.framework.file.MetaData;
import vstore.framework.file.VFileType;

/**
 * The adapter for the grid view of the "Files matching context" fragment.
 */
public class MatchedFilesRecyclerAdapter extends RecyclerView.Adapter<ThumbViewHolder> {
    private final Context mContext;
    private final List<MatchingResultRow> mResults;
    private Fragment mFrag;

    public MatchedFilesRecyclerAdapter(Context c, List<MatchingResultRow> uuids, SearchFragment f) {
        this.mContext = c;
        mResults = uuids;
        mFrag = f;
    }

    @Override
    public ThumbViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.viewholder_file, parent, false);
        return new ThumbViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ThumbViewHolder vh, int position) {
        vh.resetViewHolder();

        //Set the attributes
        MatchingResultRow r = mResults.get(position);
        MetaData meta = r.getMetaData();

        //Fetch the thumbnail from the framework
        VStore vstor = VStore.getInstance();
        vstor.getCommunicationManager().requestThumbnail(r.getUUID());
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_downloading);
        vh.getThumbnailView().setImageBitmap(bitmap);

        //Set the mimetype
        String extension = VFileType.getExtensionFromMimeType(meta.getMimeType()).toUpperCase();
        vh.setFileTypeText(extension);

        //Set the time when the file was uploaded
        long postedTime = meta.getTimestamp();
        vh.getTextPostedTime().setText(StringUtils.getTimeAgo(postedTime, mContext));
        vh.getTextPostedTime().setVisibility(View.VISIBLE);

        //Only display the name if it is something useful.
        //Could be extended in the future to analyze the name and not show stuff like "IMG2293_232210.jpg"
        if(meta.getMimeType().equals(VFileType.CONTACT_VCF) || VFileType.AUDIO_TYPES.contains(meta.getMimeType())
                || VFileType.DOC_TYPES.contains(meta.getMimeType())) {
            //Set the filename
            vh.getTextFilename().setText(meta.getFilename());
        } else {
            vh.getTextFilename().setText("");
        }
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getItemCount() {
        return mResults.size();
    }
}
