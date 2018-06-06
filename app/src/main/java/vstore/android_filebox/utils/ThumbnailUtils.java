package vstore.android_filebox.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import vstore.android_filebox.R;
import vstore.framework.VStore;
import vstore.framework.file.FileManager;
import vstore.framework.file.VFileType;
import vstore.framework.file.VStoreFile;

/**
 * Quick and dirty stuff for thumbnails. This is most likely not the best way to handle thumbnail
 * creation.
 */
public class ThumbnailUtils {

    /**
     * Gets a thumbnail for this file. Returns a default thumbnail if the file is not a
     * supported file type.
     * @param c The Android context
     * @return The thumbnail for this file.
     */
    public static Bitmap getThumbnail(Context c, VStoreFile f) {
        //Create path to thumbnail
        File thumb = new File(VStore.getInstance().getFileManager().getThumbnailsDir(),
                f.getUuid() + ".png");

        //Check if thumbnail exists
        if(thumb.exists())
        {
            Bitmap bitmap = BitmapFactory.decodeFile(thumb.getPath());
            if(bitmap != null) {
                return bitmap;
            }
        }
        //Create a thumbnail if the file type is an image file and the thumbnail was not
        //returned above
        try {
            if(VFileType.IMAGE_TYPES.contains(f.getFileType())) {
                return BitmapFactory.decodeFile(createImageThumbnail(f).getAbsolutePath());
            }
            if(VFileType.VIDEO_TYPES.contains(f.getFileType())) {
                return createVideoThumbnail(f);
            }
            if(VFileType.CONTACT_TYPES.contains(f.getFileType())) {
                return contactThumbnail(c);
            }
            if(VFileType.AUDIO_TYPES.contains(f.getFileType())) {
                return audioThumbnail(c);

            }
        }
        catch(NullPointerException e)
        {
            return defaultThumbnail(c);
        }
        return defaultThumbnail(c);
    }

    /**
     * Creates a thumbnail for a given image file.

     * @param file The file to create a thumbnail for.
     * @return A file containing the bitmap.
     */
    public static File createImageThumbnail(VStoreFile file) {
        File f = new File(file.getFullPath());
        if(f.exists())
        {
            File fThumbOutput = new File(FileManager.get().getThumbnailsDir(), file.getUuid() + ".png");
            try
            {
                Bitmap thumbnail = android.media.ThumbnailUtils.extractThumbnail(
                        BitmapFactory.decodeFile(f.getAbsolutePath()), 512, 512);

                FileOutputStream fos = new FileOutputStream(fThumbOutput);
                thumbnail.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.flush();
                fos.close();
                thumbnail.recycle();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return null;
            }
            return fThumbOutput;
        }
        return null;
    }

    /**
     * Creates a thumbnail from the given video file.
     *
     * @param file The video file (should be a supported type from {@link VFileType}).
     * @return Null, if file not supported. The thumbnail otherwise.
     */
    public static Bitmap createVideoThumbnail( VStoreFile file) {
        File f = new File(file.getFullPath());
        if(f.exists() && VFileType.VIDEO_TYPES.contains(file.getFileType()))
        {
            Bitmap thumb = android.media.ThumbnailUtils.createVideoThumbnail(f.getAbsolutePath(),
                    MediaStore.Images.Thumbnails.MINI_KIND);
            writeThumbToDisk(thumb, file.getUuid());
            return thumb;
        }
        return null;
    }

    /**
     * This method returns a default thumbnail for an unknown file.
     * @param c The Android context.
     * @return A default bitmap showing that the file is unknown.
     */
    private static Bitmap defaultThumbnail(Context c) {
        return BitmapFactory.decodeResource(
                c.getApplicationContext().getResources(), R.mipmap.ic_unknown_file);
    }

    /**
     * This method returns a default contact thumbnail.
     * @param c The Android context.
     * @return A contact bitmap showing that the file is a contact.
     */
    private static Bitmap contactThumbnail(Context c) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        return BitmapFactory.decodeResource(
                c.getApplicationContext().getResources(), R.mipmap.ic_contact, options);
    }

    /**
     * This method returns a default audio thumbnail.
     * @param c The Android context.
     * @return An audio bitmap showing that the file is an audio file.
     */
    private static Bitmap audioThumbnail(Context c) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        return BitmapFactory.decodeResource(
                c.getApplicationContext().getResources(), R.mipmap.ic_audio, options);
    }

    /**
     * This method compresses the thumbnail to a 85% quality PNG and saves it to disk.
     *
     * @param thumb The thumbnail bitmap data
     * @param uuid The UUID (used for the filename).
     */
    private static void writeThumbToDisk(Bitmap thumb, String uuid) {
        File outThumbFile = new File(FileManager.get().getThumbnailsDir(), uuid + ".png");
        try {
            FileOutputStream fOutStream = new FileOutputStream(outThumbFile);
            thumb.compress(Bitmap.CompressFormat.PNG, 85, fOutStream);
            fOutStream.flush();
            fOutStream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
