package cn.harpsichord.lajproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
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

public class OpenCVCameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    private static final String TAG = "OpenCVCameraActivity";
    private JavaCamera2View javaCameraView;
    private boolean isFrontCamera;
    private Mat mRgba;
    private CascadeClassifier cascadeClassifier;
    private Size minSize;
    private Size maxSize;
    private long frameCount;
    private long startTs;
    private TextView fpsText;
    private boolean disableDetect = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initWindow();

        setContentView(R.layout.activity_open_cvcamera);

        Button switchButton = findViewById(R.id.switch_button);
        switchButton.setOnClickListener(this);

        Button exitButton = findViewById(R.id.exit_button);
        exitButton.setOnClickListener(this);

        Button disableButton = findViewById(R.id.disable_button);
        disableButton.setOnClickListener(this);

        javaCameraView = findViewById(R.id.JavaCamera2View);
        javaCameraView.setCvCameraViewListener(this);

        fpsText = findViewById(R.id.fps_meter);
        assert fpsText != null;

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(OpenCVCameraActivity.this, "Failed to init OpenCV!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            try {
                cascadeClassifier = getClassifier();
            } catch (IOException e) {
                Toast.makeText(OpenCVCameraActivity.this, "Failed to getClassifier!", Toast.LENGTH_SHORT).show();
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
        minSize = new Size(120, 120); // 越小越精确越卡
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
        if (disableDetect) {
            detect = false;
        }
        if (frameCount % 3 != 0) {
            // 如果开启了检测人脸，每3帧检测一次
            detect = false;
        }

        if (isFrontCamera) {
            // 如果是前置摄像头，做一个镜像翻转
            Mat flipMat = new Mat();
            Core.flip(mRgba, flipMat, 1);
            return detect? detectFace(flipMat): flipMat;
        } else {
            return detect? detectFace(mRgba): mRgba;
        }
    }

    private Mat detectFace(Mat matSrc) {
        Mat matGray = new Mat();
        Imgproc.cvtColor(matSrc, matGray, Imgproc.COLOR_BGRA2GRAY);
        MatOfRect faces = new MatOfRect();
        cascadeClassifier.detectMultiScale(matGray, faces, 1.2, 3, 0, minSize, maxSize);
        List<Rect> facesList = faces.toList();
        for (Rect rect: facesList) {
            Imgproc.rectangle(matSrc, rect.tl(), rect.br(), new Scalar(255, 0, 0, 255), 5);
        }
        return matSrc;
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.switch_button) {
            javaCameraView.disableView();  // 点击的瞬间disable，避免有反的图片残影
            if (isFrontCamera) {
                javaCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                isFrontCamera = false;
            } else {
                javaCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                isFrontCamera = true;
            }
            javaCameraView.enableView();
        } else if (v.getId() == R.id.exit_button) {
            finish();
        } else if (v.getId() == R.id.disable_button) {
            Button bv = (Button) v;
            if (disableDetect) {
                disableDetect = false;
                bv.setText("D");
                bv.setTextColor(Color.GREEN);
            } else {
                disableDetect = true;
                bv.setTextColor(Color.RED);
                bv.setText("E");
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

    public CascadeClassifier getClassifier() throws IOException {
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
        boolean delete1 = file.delete();
        boolean delete2 = cascade.delete();
        if (!(delete1 && delete2)) {
            Log.e(TAG, "Failed to delete some files.");
        }
        return cascadeClassifier;
    }
}