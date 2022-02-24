package cn.harpsichord.lajproject.rokid;

import android.content.Context;
import android.util.Log;

import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class RokidClassifier {

    private static final String TAG = "RClassifier";
    private static final String modeName = "cigdatalogo_cascade_1";

    public static CascadeClassifier loadCascadeClassifier(Context context) throws IOException {
        int rId = context.getResources().getIdentifier(modeName, "raw", context.getPackageName());
        InputStream inputStream = context.getResources().openRawResource(rId);
        File cascade = context.getDir("cascade", Context.MODE_PRIVATE);  // TODO why?
        File file = new File(cascade, "tmp_" + System.currentTimeMillis() + ".xml");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesRead);
        }
        inputStream.close();
        fileOutputStream.close();
        CascadeClassifier cascadeClassifier = new CascadeClassifier(file.getAbsolutePath());
        boolean delete1 = file.delete();
        boolean delete2 = cascade.delete();
        if (!(delete1 && delete2)) {
            Log.e(TAG, "Failed to delete some files.");
        }
        return cascadeClassifier;
    }

}
