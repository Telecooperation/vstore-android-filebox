package vstore.android_filebox.qrcode;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import vstore.android_filebox.R;

public class QRCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);

        String fileId = getIntent().getStringExtra("VSTORE_FILE_ID");
        ImageView img = findViewById(R.id.imgQRCode);
        Bitmap qr = QRCodeHandler.textToQRCode(fileId);
        img.setImageBitmap(qr);

        Button b = findViewById(R.id.btnBack);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                QRCodeActivity.this.finish();
            }
        });
    }
}
