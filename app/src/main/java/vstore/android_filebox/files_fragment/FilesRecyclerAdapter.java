package vstore.android_filebox.files_fragment;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import vstore.android_filebox.R;
import vstore.android_filebox.utils.ThumbnailUtils;
import vstore.framework.file.VStoreFile;

/**
 * This class is the basic adapter for the file recycler view, extending RecyclerView.Adapter.
 * A custom viewholder is specified to gives access to the views.
 */
public class FilesRecyclerAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;

    //Holds the elements for the recycler view
    private List<Object> mListItems;

    public static final int DATE = 0, FILE = 1;

    public FilesRecyclerAdapter(Context c, List<Object> itemlist) {
        mContext = c;
        mListItems = itemlist;
    }

    @Override
    public int getItemViewType(int position) {
        if (mListItems.get(position) instanceof VStoreFile) {
            return FILE;
        } else if (mListItems.get(position) instanceof DateHeader) {
            return DATE;
        }
        return -1;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        //Inflate the correct layout
        switch (viewType) {
            case DATE:
                View v1 = inflater.inflate(R.layout.viewholder_date, parent, false);
                viewHolder = new ViewHolder_Date(v1);
                break;

            case FILE:
                View v2 = inflater.inflate(R.layout.viewholder_file, parent, false);
                viewHolder = new ViewHolder_File(v2);
                break;

            default:
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case DATE:
                ViewHolder_Date vhdate = (ViewHolder_Date) holder;
                configureViewHolder_Date(vhdate, position);
                break;
            case FILE:
                ViewHolder_File vhfile = (ViewHolder_File) holder;
                configureViewHolder_File(vhfile, position);
                break;
            default:
                break;
        }
    }

    private void configureViewHolder_Date(ViewHolder_Date vhd, int position) {
        String date = ((DateHeader) mListItems.get(position)).getTitle();
        if (date != null) {
            vhd.getTitleView().setText(date);
        }
    }

    private void configureViewHolder_File(ViewHolder_File vhf, int position) {
        //Set the attributes
        VStoreFile f = (VStoreFile) mListItems.get(position);
        vhf.getThumbnailView().setImageBitmap(ThumbnailUtils.getThumbnail(mContext, f));
        vhf.setUploadPending(f.isUploadPending());
        vhf.setFileTypeText(f.getFileExtension().toUpperCase());
        vhf.setFilenameText(f.getDescriptiveName());
    }

    @Override
    public int getItemCount() {
        return mListItems.size();
    }
}
