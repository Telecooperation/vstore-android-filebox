package vstore.android_filebox;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import vstore.android_filebox.events.ExportFileEvent;
import vstore.android_filebox.qrcode.QRCodeActivity;
import vstore.android_filebox.utils.StringUtils;
import vstore.framework.file.MetaData;
import vstore.framework.node.NodeType;


public class MetadataFragment extends DialogFragment {

    private MetaData mMeta;

    DecimalFormat df = new DecimalFormat("#.##");

    /**
     * Create a new instance of the CreateRuleDialog.
     */
    public static MetadataFragment newInstance(MetaData meta) {
        MetadataFragment f = new MetadataFragment();
        f.mMeta = meta;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int theme = R.style.AppDialogTheme;
        setStyle(STYLE_NORMAL, theme);
        df.setRoundingMode(RoundingMode.CEILING);
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View d = getActivity().getLayoutInflater()
                .inflate(R.layout.fragment_metadata, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        TextView txtFilename = (TextView) d.findViewById(R.id.txtFilename);
        txtFilename.setText(mMeta.getFilename());
        TextView txtFilesize = (TextView) d.findViewById(R.id.txtFilesize);
        txtFilesize.setText((df.format(mMeta.getFilesize()/1024.0f/1024.0f)) + "MB");
        TextView txtMimetype = (TextView) d.findViewById(R.id.txtMimetype);
        txtMimetype.setText(mMeta.getMimeType());
        TextView txtCreationDate = (TextView) d.findViewById(R.id.txtCreationDate);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        txtCreationDate.setText(dateFormat.format(mMeta.getCreationDate()));
        TextView txtNodeType = (TextView) d.findViewById(R.id.txtNodeType);
        NodeType t = mMeta.getNodeType();
        txtNodeType.setText(StringUtils.capitalizeOnlyFirstLetter(t.toString()));

        Button btnShowQR = d.findViewById(R.id.btnShowQR);
        btnShowQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), QRCodeActivity.class);
                intent.putExtra("VSTORE_FILE_ID", mMeta.getUUID());
                startActivity(intent);
            }
        });

        builder.setView(d)
                .setTitle(R.string.file_metadata)
                .setPositiveButton(R.string.done, null)
                .setNeutralButton(R.string.download, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ExportFileEvent evt = new ExportFileEvent();
                        evt.mUUID = mMeta.getUUID();
                        EventBus.getDefault().post(evt);
                    }
                });
        return builder.create();
    }
}
