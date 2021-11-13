package cn.harpsichord.lajproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class StaticFaceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_static_face);

        ImageView iv = findViewById(R.id.staticImageView);

        Button detect = findViewById(R.id.detect1);
        detect.setOnClickListener(v -> {

            if (!OpenCVLoader.initDebug()) {
                Toast.makeText(StaticFaceActivity.this, "Failed to init OpenCV!", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // TODO getClassifier on first Create
                iv.setImageBitmap(detectFace(getClassifier()));
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(StaticFaceActivity.this, "Failed to classify!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private CascadeClassifier getClassifier() throws IOException {
        InputStream inputStream = getResources().openRawResource(R.raw.lbpcascade_frontalface_improved);
        File cascade = getDir("cascade", Context.MODE_PRIVATE);  // TODO why?
        File file = new File(cascade, "lbpcascade_frontalface_improved.xml");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesRead);
        }
        inputStream.close();
        fileOutputStream.close();
        CascadeClassifier cascadeClassifier = new CascadeClassifier(file.getAbsolutePath());
        file.delete();
        cascade.delete();
        return cascadeClassifier;
    }

    private Bitmap detectFace(CascadeClassifier cascadeClassifier) throws IOException {
        Mat matSrc = Utils.loadResource(StaticFaceActivity.this, R.drawable.face);
        Mat matDst = new Mat();
        Mat matGray = new Mat();
        Imgproc.cvtColor(matSrc, matGray, Imgproc.COLOR_BGRA2GRAY);
        MatOfRect faces = new MatOfRect();
        cascadeClassifier.detectMultiScale(matGray, faces, 1.05, 3, 0, new Size(30, 30), new Size());
        List<Rect> facesList = faces.toList();
        matSrc.copyTo(matDst);
        for (Rect rect: facesList) {
            Imgproc.rectangle(matDst, rect.tl(), rect.br(), new Scalar(255, 0, 0, 255), 4);
        }
        Imgproc.cvtColor(matDst, matDst, Imgproc.COLOR_BGR2RGB); // BGT -> RGB
        Bitmap bitmap = Bitmap.createBitmap(matDst.width(), matDst.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matDst, bitmap);
        matSrc.release();
        matDst.release();
        matGray.release();
        return bitmap;
    }
}