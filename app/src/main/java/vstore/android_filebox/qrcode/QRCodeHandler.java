package vstore.android_filebox.qrcode;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class QRCodeHandler {

    public static void scanQRCode(Activity activity) {
        IntentIntegrator integrator = new IntentIntegrator(activity);
        integrator.initiateScan();
    }

    public static Bitmap textToQRCode(String text) {
        BitMatrix bitMatrix;
        try
        {
            bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 500, 500, null);
        }
        catch (IllegalArgumentException | WriterException e)
        {
            return null;
        }

        int matrixWidth = bitMatrix.getWidth();
        int matrixHeight = bitMatrix.getHeight();
        int pixels[] =  new int[matrixWidth * matrixHeight];

        for (int y = 0; y < matrixHeight; y++)
        {
            int offset = y * matrixWidth;
            for (int x = 0; x < matrixWidth; x++)
            {
                pixels[offset + x] = (bitMatrix.get(x, y)) ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.RGB_565);
        bitmap.setPixels(pixels, 0, 500, 0, 0, matrixWidth, matrixHeight);
        return bitmap;
    }
}
