package cn.harpsichord.lajproject;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Executors;

public class OpenCVCameraCIGActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    private static final String TAG = "OpenCVCameraCIGActivity";
    private Mat mRgba;

    private Size minSize;
    private Size maxSize;
    private long frameCount;
    private long startTs;
    private TextView fpsText;
    private List<Rect> facesList ;


    private String currentModelName;
    private CascadeClassifier cascadeClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initWindow();

        setContentView(R.layout.activity_open_cv_cig_camera);

        Button changeModelBtn = findViewById(R.id.change_model_button);
        changeModelBtn.setOnClickListener(this);

        Button exitButton = findViewById(R.id.exit_button_2);
        exitButton.setOnClickListener(this);

        JavaCamera2View javaCameraView = findViewById(R.id.JavaCamera2View2);
        javaCameraView.setCvCameraViewListener(this);

        fpsText = findViewById(R.id.fps_meter_2);
        assert fpsText != null;

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(OpenCVCameraCIGActivity.this, "Failed to init OpenCV!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            try {
                getAndChangeCascadeClassifier();
            } catch (IOException e) {
                Toast.makeText(OpenCVCameraCIGActivity.this, "Failed to getClassifier!", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                finish();
            }
        }

        assert cascadeClassifier != null;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            javaCameraView.setCameraPermissionGranted();
            // 一进来就开启摄像头
            javaCameraView.enableView();
        }

        getFPS();
    }

    private void initWindow() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        minSize = new Size(50, 50); // 越小越精确越卡
        maxSize = new Size();
        frameCount = 0;
        startTs = System.currentTimeMillis();
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        frameCount ++;
        boolean detect = true;
        if (frameCount % 3 != 0) {
            // 如果开启了检测，每3帧检测一次，其他帧滞留原来的结果
            detect = false;
        }
        return detectFace(mRgba, detect);
    }

    private Mat detectFace(Mat matSrc, boolean detect) {
        if (detect) {
            Mat matGray = new Mat();
            Imgproc.cvtColor(matSrc, matGray, Imgproc.COLOR_BGRA2GRAY);
            MatOfRect faces = new MatOfRect();
            cascadeClassifier.detectMultiScale(matGray, faces, 1.2, 3, 0, minSize, maxSize);
            facesList = faces.toList();
        }
        if (facesList == null) {
            return matSrc;
        }
        for (Rect rect: facesList) {
            Imgproc.rectangle(matSrc, rect.tl(), rect.br(), new Scalar(255, 0, 0, 255), 5);
        }
        return matSrc;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.exit_button_2) {
            finish();
        } else if (v.getId() == R.id.change_model_button) {
            try {
                getAndChangeCascadeClassifier();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to change Model!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getFPS() {
        Executors.newSingleThreadExecutor().execute(() -> {
            while (true) {
                long ts = System.currentTimeMillis();
                double l = frameCount * 1000.0 / (ts - startTs);
                String fpsString = "FPS: " + Math.round(l * 100.0) / 100.0;  // round(x, 2)
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    fpsText.setText(fpsString);
                });
            }
        });
    }

    private void getAndChangeCascadeClassifier() throws IOException {

        String[] models = new String[]{
                "cigdatalogo_cascade_1",
                "cigdatalogo_cascade_3",
        };

        if (currentModelName == null) {
            currentModelName = models[0];
        } else {
            int index = -1;
            for (int i=0; i<models.length; i++) {
                if (models[i].equals(currentModelName)) {
                    index = i;
                }
            }
            if (index == models.length - 1) {
                currentModelName = models[0];
            } else {
                currentModelName = models[index+1];
            }
        }

        Toast.makeText(this, "Current Model: " + currentModelName, Toast.LENGTH_LONG).show();

        int rId = this.getResources().getIdentifier(currentModelName, "raw", this.getPackageName());

        InputStream inputStream = getResources().openRawResource(rId);
        File cascade = getDir("cascade", Context.MODE_PRIVATE);  // TODO why?
        File file = new File(cascade, "tmp_cig_logo_model.xml");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesRead);
        }
        inputStream.close();
        fileOutputStream.close();
        cascadeClassifier = new CascadeClassifier(file.getAbsolutePath());
        boolean delete1 = file.delete();
        boolean delete2 = cascade.delete();
        if (!(delete1 && delete2)) {
            Log.e(TAG, "Failed to delete some files.");
        }
    }
}