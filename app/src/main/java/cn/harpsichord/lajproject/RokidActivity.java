package cn.harpsichord.lajproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alphamovie.lib.AlphaMovieView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
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

public class RokidActivity extends AppCompatActivity {

    public String currentModelName;
    public CascadeClassifier cascadeClassifier;
    private final String TAG = "ROKID";
    private AlphaMovieView videoView;

    private Mat mRgba;
    private Size minSize;
    private Size maxSize;
    private long frameCount = 0;
    private List<Rect> targetList;
    private int videoStatus = 0;
    private TextView showTextView;

    private static final String alphaVideoUri = "https://cig-test.s3.cn-north-1.amazonaws.com.cn/liutao/CIGAR/media/alpha_channel_test.mp4";
    private ImageView targetShowImageView;
    private ImageView fullShowImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rokid);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        videoView = findViewById(R.id.front_video_over_camera_rokid);
        videoView.setVideoByUrl(alphaVideoUri);
        videoView.setOnVideoEndedListener(() -> {
            videoStatus = 2;
            videoView.stop();
        });

        showTextView = findViewById(R.id.show_target_text);
        targetShowImageView = findViewById(R.id.show_target_image);
        fullShowImageView = findViewById(R.id.show_full_image);

        JavaCamera2View javaCameraView = findViewById(R.id.JavaCamera2View2Rokid);
        javaCameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                mRgba = new Mat();
                minSize = new Size(60, 60); // 越小越精确越卡 todo 与width/height挂钩
                maxSize = new Size();
                // Rokid 只有640x480的分辨率 minSize 得小一点
                Toast.makeText(RokidActivity.this, "Camera Started: " + width + "x" + height, Toast.LENGTH_LONG).show();
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
                int skip = 5;
                if (frameCount % skip != 0) {
                    // 如果开启了检测，每skip帧检测一次，其他帧滞留原来的结果
                    detect = false;
                } else {
                    frameCount = 0;
                }
                Log.w(TAG, "Mat Size: " + mRgba.size());
                return detectTarget(mRgba, detect);
            }
        });

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(RokidActivity.this, "Failed to init OpenCV!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            try {
                getAndChangeCascadeClassifier();
            } catch (IOException e) {
                Toast.makeText(RokidActivity.this, "Failed to getClassifier!", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                finish();
            }
        }
        javaCameraView.setCameraPermissionGranted();
        javaCameraView.enableView();
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

        for (Rect rect: targetList) {
            Imgproc.rectangle(matSrc, rect.tl(), rect.br(), new Scalar(255, 0, 0, 255), 5);
        }

        if (targetList.size() > 0 && videoStatus == 0) {
            Rect rect = targetList.get(0);

            // 在另一个Thread中运行，matSrc可能已经不是原来那个了，因此要copy一个出来
            Mat cloneMat = matSrc.clone();
            runOnUiThread(() ->
                    {
                        Bitmap bitmap = Bitmap.createBitmap(rect.width, rect.height, Bitmap.Config.ARGB_8888);
                        // 截取识别到的图像
                        Utils.matToBitmap(cloneMat.submat(rect), bitmap);

                        Bitmap bitmap2 = Bitmap.createBitmap(cloneMat.width(), cloneMat.height(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(cloneMat, bitmap2);

                        targetShowImageView.setImageBitmap(bitmap);
                        fullShowImageView.setImageBitmap(bitmap2);
                        showTextView.setText("识别到目标：");
                    }
            );
            playVideo();
        }

        return matSrc;
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
        Log.w(TAG, "Current Model: " + currentModelName);

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

    private void playVideo() {
        runOnUiThread(() -> {
            // 一开始使用本地视频时，需要videoView.getMediaPlayer().reset();
            // 改为通过url播放远端视频，在View的onCreate方法里setVideoByUrl，不再需要reset MediaPlayer

            // TODO 闪黑屏
            videoView.setVisibility(View.VISIBLE);
            videoStatus = 1;
            videoView.start();
        });
    }

}