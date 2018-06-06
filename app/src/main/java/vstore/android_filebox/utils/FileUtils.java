package vstore.android_filebox.utils;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import vstore.framework.VStore;
import vstore.framework.file.VFileType;
import vstore.framework.file.VStoreFile;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

public class FileUtils {

    public static Uri getContentUriFromFile(Context context, File file) {
        String filePath = file.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.Media._ID },
                MediaStore.Images.Media.DATA + "=? ",
                new String[] { filePath }, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            cursor.close();
            return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id);
        } else {
            if (file.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    /**
     * Convenience method to open a file. You need to provide a uri and a mimetype.
     * @param c The Android context.
     * @param uri The uri to the file.
     * @param mimetype The mimetype of the file.
     */
    public static void openFile(Context c, Uri uri, String mimetype) {
        if(c != null && uri != null && mimetype != null) {
            try {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, mimetype);
                intent.setFlags(FLAG_GRANT_READ_URI_PERMISSION);
                c.startActivity(intent);
            } catch(ActivityNotFoundException e) {
                //No compatible activity found. Let user choose any app.
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "*/*");
                intent.setFlags(FLAG_GRANT_READ_URI_PERMISSION);
                c.startActivity(intent);
            }
        }
    }

    /**
     * This method returns the content provider uri for this file. You can use this uri to open
     * the file with other applications.
     * @param c The Android context.
     * @return The content uri
     */
    public static Uri getContentUriForFile(Context c, VStoreFile f) {
        return FileProvider.getUriForFile(
                c,
                vstore.android_filebox.FileProvider.FILE_PROVIDER_AUTHORITY,
                new File(f.getFullPath()));
    }


    /**
     * Copies the file into a temporary folder and returns the path to this folder.
     *
     * @param c The Android context
     * @param contentUri The uri of the file in the content provider
     * @param tmpName The name for the temporary file.
     * @return The absolute path to the file.
     */
    public static String getPathFromContentUri(Context c, Uri contentUri, String tmpName) {
        //Resolve normal file from content provider
        String mimetype = c.getContentResolver().getType(contentUri);
        String extension = VFileType.getExtensionFromMimeType(mimetype);

        Cursor returnCursor =
                c.getContentResolver()
                        .query(contentUri, null, null, null, null);
        if(returnCursor != null)
        {
            returnCursor.moveToFirst();
            String descriptiveName = returnCursor.getString(
                    returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

            String targetName;
            if(FileUtils.hasExtension(tmpName)) {
                targetName = tmpName;
            } else {
                targetName =  tmpName + "." + extension;
            }

            File fCopied = copyFileFromContentUri(
                    c,
                    contentUri,
                    descriptiveName,
                    VStore.getInstance().getFileManager().getStoredFilesDir(),
                    targetName);
            returnCursor.close();
            return fCopied.getAbsolutePath();
        }
        return null;
    }

    /**
     * Reads the name of a file from the content provider.
     * @param c The android context.
     * @param uri The uri of the file in the content provider.
     * @param defaultVal The default value to return if no name is found.
     * @return The name of the file, or the default value.
     */
    public static String getNameFromContentProvider(Context c, Uri uri, String defaultVal) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = c.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
            result = defaultVal + "_" + result;
        }
        return result;
    }

    /**
     * Copies a file into the given output folder coming from a "content://" uri.
     * @param c The Android context.
     * @param uri The content scheme uri.
     * @param inputFileName The file name of the input file.
     * @param outputPath The path where to save the file.
     * @param outputName The name of the output file that should be created.
     * @return A file object containing a reference to the created output file.
     * Will return null if Uri is no "content://" uri.
     */
    public static File copyFileFromContentUri(Context c, Uri uri, String inputFileName,
                                              File outputPath, String outputName) {
        if(uri.getScheme().equals("content")) {
            ParcelFileDescriptor parcelFd;
            try {
                //Get the content resolver instance for this context, and use it
                //to get a ParcelFileDescriptor for the file.
                parcelFd = c.getContentResolver().openFileDescriptor(uri, "r");
            } catch (FileNotFoundException e) {
                Toast.makeText(c, "Error: File not found!", Toast.LENGTH_SHORT).show();
                return null;
            }
            FileDescriptor fd = parcelFd.getFileDescriptor();
            FileInputStream in = new FileInputStream(fd);
            File copiedFile = copyFileFromInputStream(in, inputFileName, outputPath, outputName);
            try {
                in.close();
            } catch(IOException e) { }
            return copiedFile;
        }
        return null;
    }

    /**
     * Copies a file fro the input stream to the given output path with the given output name.
     * The given file input stream will not be closed.
     * @param in The file input stream to copy data from.
     * @param inputFileName The filename of the input file.
     * @param outputPath The output path where to save the new file.
     * @param outputName The name of the new file.
     * @return A File object containing a reference to the path of the created file.
     */
    private static File copyFileFromInputStream(FileInputStream in, String inputFileName,
                                                File outputPath, String outputName) {
        OutputStream out;
        try {
            //Create output directory if it doesn't exist
            if (!outputPath.exists()) {
                outputPath.mkdirs();
            }
            if (outputName == null || "".equals(outputName)) {
                outputName = outputPath.getAbsolutePath() + "/" + inputFileName;
            } else {
                outputName = outputPath.getAbsolutePath() + "/" + outputName;
            }
            out = new FileOutputStream(outputName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();

            //Write the output file (file has now been copied)
            out.flush();
            out.close();
            return new File(outputName);

        }  catch (FileNotFoundException fnfe1) {
            return null;
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Writes the given String to the given file in the given output path. Creates necessary
     * directories on the way to output path if they do not exist.
     * @param s The string to write to the file.
     * @param outputPath The path where the output file should be created.
     * @param outputName The name of the output file.
     * @return The file object referencing the created file.
     */
    public static File writeStringToFile(String s, File outputPath, String outputName) {
        //Create output directory if it doesn't exist
        if (!outputPath.exists()) {
            outputPath.mkdirs();
        }
        if (outputName == null || "".equals(outputName)) {
            return null;
        } else {
            outputName = outputPath.getAbsolutePath() + "/" + outputName;
        }
        try {
            PrintWriter out = new PrintWriter(outputName);
            out.write(s);
            out.close();
            return new File(outputName);
        } catch(FileNotFoundException e) {
            return null;
        }
    }

    /**
     * @param filename The filename to check
     * @return True, if the file has an extension
     */
    public static boolean hasExtension(String filename) {
        String extension = "";

        int i = filename.lastIndexOf('.');
        int p = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));

        if (i > p) {
            extension = filename.substring(i+1);
        }
        return !extension.equals("");
    }
}