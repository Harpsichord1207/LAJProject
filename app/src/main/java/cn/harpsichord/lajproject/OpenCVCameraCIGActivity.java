package cn.harpsichord.lajproject;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.alphamovie.lib.AlphaMovieView;

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
    private List<Rect> targetList ;

    private int frameCount = 0;
    private String currentModelName;
    private TextView modeNameTextView;
    private CascadeClassifier cascadeClassifier;

    // 0: not played, 1: playing, 2: finished
    private int videoStatus = 0;
    private AlphaMovieView videoView;
    private static String alphaVideoUri = "https://cig-test.s3.cn-north-1.amazonaws.com.cn/liutao/CIGAR/media/alpha_channel_test.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initWindow();
        setContentView(R.layout.activity_open_cv_cig_camera);
        // ProgressBar pb = findViewById(R.id.wait_progress_bar);

        Button changeModelBtn = findViewById(R.id.change_model_button);
        changeModelBtn.setOnClickListener(this);

        Button exitButton = findViewById(R.id.exit_button_2);
        exitButton.setOnClickListener(this);

        JavaCamera2View javaCameraView = findViewById(R.id.JavaCamera2View2);
        javaCameraView.setCvCameraViewListener(this);

        modeNameTextView = findViewById(R.id.mode_name_text);

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
            // ???????????????????????????
            javaCameraView.enableView();
        }

        videoView = findViewById(R.id.front_video_over_camera);
        videoView.setVideoByUrl(alphaVideoUri);
        videoView.setOnVideoEndedListener(() -> {
            videoStatus = 2;
            videoView.stop();
        });
        // videoView.setZOrderOnTop(true); // not work in runOnUiThread?
        // pb.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoView.onPause();
    }

    private void playVideo() {
        runOnUiThread(() -> {
            // ???????????????????????????????????????videoView.getMediaPlayer().reset();
            // ????????????url????????????????????????View???onCreate?????????setVideoByUrl???????????????reset MediaPlayer

            // TODO ?????????
            videoView.setVisibility(View.VISIBLE);
            System.out.println("Point 1");
            videoStatus = 1;
            videoView.start();
            System.out.println("Point 2");
        });
    }

    private void initWindow() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        minSize = new Size(50, 50); // ?????????????????????
        maxSize = new Size();
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
            // ???????????????????????????3????????????????????????????????????????????????
            detect = false;
        } else {
            frameCount = 0;
        }
        return detectTarget(mRgba, detect);
    }

    private Mat detectTarget(Mat matSrc, boolean detect){
        if (detect) {
            Mat matGray = new Mat();
            Imgproc.cvtColor(matSrc, matGray, Imgproc.COLOR_BGRA2GRAY);
            MatOfRect targets = new MatOfRect();
            cascadeClassifier.detectMultiScale(matGray, targets, 1.2, 3, 0, minSize, maxSize);
            targetList = targets.toList();
        }
        if (targetList == null) {
            return matSrc;
        }
        if (targetList.size() > 0 && videoStatus == 0) {
            playVideo();
        }
        for (Rect rect: targetList) {
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

    private void getAndChangeCascadeClassifier() throws IOException {

        String[] models = new String[]{
                "cigdatalogo_cascade_1",
                "cigdatalogo_cascade_3",
                "cigdatalogo_cascade_4",
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
        modeNameTextView.setText(currentModelName);

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