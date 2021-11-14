package cn.harpsichord.lajproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class OpenCVCameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    private JavaCamera2View javaCameraView;
    private boolean isFrontCamera;
    private Mat mRgba;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initWindow();

        setContentView(R.layout.activity_open_cvcamera);

        Button switchButton = findViewById(R.id.switch_button);
        switchButton.setOnClickListener(this);

        javaCameraView = findViewById(R.id.JavaCamera2View);
        javaCameraView.setCvCameraViewListener(this);

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(OpenCVCameraActivity.this, "Failed to init OpenCV!", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            javaCameraView.setCameraPermissionGranted();
            // 一进来就开启摄像头
            javaCameraView.enableView();
        }
    }

    private void initWindow() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        if (isFrontCamera) {
            // 如果是前置摄像头，做一个镜像翻转
            Mat flipMat = new Mat();
            Core.flip(mRgba, flipMat, 1);
            return flipMat;
        } else {
            return mRgba;
        }
    }

    @Override
    public void onClick(View v) {
        javaCameraView.disableView();  // 点击的瞬间disable，避免有反的图片残影
        if (v.getId() == R.id.switch_button) {
            if (isFrontCamera) {
                javaCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                isFrontCamera = false;
            } else {
                javaCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                isFrontCamera = true;
            }
        }
        javaCameraView.enableView();
    }
}