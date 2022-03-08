package cn.harpsichord.lajproject.rokid;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

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

import java.io.IOException;
import java.util.List;

import cn.harpsichord.lajproject.R;

public class RokidActivity extends AppCompatActivity {

    public CascadeClassifier cascadeClassifier;
    private final String TAG = "ROKID";

    private Mat mRgba;
    private Size minSize;
    private Size maxSize;

    private JavaCamera2View javaCameraView;
    private VideoView videoView;

    private LinearLayout linearLayout1;
    private TextView showTextView;
    private ImageView targetShowImageView;
    private ImageView fullShowImageView;

    private LinearLayout linearLayout2;
    private TextView showTextView2;
    private ImageView targetShowImageView2;
    private ImageView fullShowImageView2;

    private LinearLayout linearLayout3;
    private TextView showTextView3;
    private ImageView targetShowImageView3;
    private ImageView fullShowImageView3;

    // 每隔几帧检测一次降低CPU压力
    private final int FrameSkip = 5;
    // 连续几次都检测到目标才认为到达指定场景
    private final int ContinueDetectCount = 2;
    private int frameCount = 0;
    private int detectCount = 0;

    private RokidEnum.RokidStatus rokidStatus = RokidEnum.RokidStatus.s00;

    private void init(Context context) {
        // Window Manager
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        // All views
        javaCameraView = findViewById(R.id.JavaCamera2View2Rokid);
        videoView = findViewById(R.id.front_video_over_camera_rokid);

        linearLayout1 = findViewById(R.id.help_info);
        showTextView = findViewById(R.id.show_target_text);
        targetShowImageView = findViewById(R.id.show_target_image);
        fullShowImageView = findViewById(R.id.show_full_image);

        linearLayout2 = findViewById(R.id.help_info_2);
        showTextView2 = findViewById(R.id.show_target_text_2);
        targetShowImageView2 = findViewById(R.id.show_target_image_2);
        fullShowImageView2 = findViewById(R.id.show_full_image_2);

        linearLayout3 = findViewById(R.id.help_info_3);
        showTextView3 = findViewById(R.id.show_target_text_3);
        targetShowImageView3 = findViewById(R.id.show_target_image_3);
        fullShowImageView3 = findViewById(R.id.show_full_image_3);
    }

    private void configViews(Context context) {
        videoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.cczs_v1));
        videoView.setOnCompletionListener(mp -> {
            videoView.setVisibility(View.GONE);
            linearLayout2.setVisibility(View.VISIBLE);
        });

        linearLayout2.setVisibility(View.GONE);
        linearLayout3.setVisibility(View.GONE);

        CameraBridgeViewBase.CvCameraViewListener2 listener2 = new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                mRgba = new Mat();
                if (width < 1000) {
                    // Rokid 只有640x480的分辨率 minSize 得小一点
                    minSize = new Size(40, 40); // 越小越精确越卡
                } else {
                    minSize = new Size(150, 150);
                }
                maxSize = new Size();
                Log.w(TAG, "Current Resolution: " + width + " X " + height);
            }

            @Override
            public void onCameraViewStopped() {
                mRgba.release();
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                mRgba = inputFrame.rgba();
                return detectTarget(mRgba, checkIfDetect());
            }
        };
        javaCameraView.setCvCameraViewListener(listener2);
        javaCameraView.setCameraPermissionGranted();
        javaCameraView.enableView();

    }

    private boolean loadOpenCV(Context context) {
        if (!OpenCVLoader.initDebug()) {
            return false;
        }

        try {
            cascadeClassifier = RokidClassifier.loadCascadeClassifier(context);
        } catch (IOException e) {
            Log.w(TAG, "Failed to load classifier: " + e);
            return false;
        }

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rokid);

        init(this);
        configViews(this);

        if (!loadOpenCV(this)) {
            Log.e(TAG, "Failed to load OpenCV!");
            finish();
        }


    }

    private Mat detectTarget(Mat matSrc, boolean detect){

        // 为了减轻计算压力，可以每隔几帧检测一次，不检测的帧滞留结果
        // 不过现在是检测到就触发播放视频/图片，所以不需要滞留了，之前的逻辑可以参考git变化记录

        if (!detect) {
            return matSrc;
        }

        Mat matGray = new Mat();
        Imgproc.cvtColor(matSrc, matGray, Imgproc.COLOR_BGRA2GRAY);
        MatOfRect targets = new MatOfRect();
        // TODO: 这些参数是否可以再优化？
        cascadeClassifier.detectMultiScale(matGray, targets, 1.2, 3, 0, minSize, maxSize);
        List<Rect> targetList = targets.toList();

        if (targetList.size() == 0) {
            return matSrc;
        }

        detectCount ++;
        if (detectCount >= ContinueDetectCount) {
            for (Rect rect: targetList) {
                Imgproc.rectangle(matSrc, rect.tl(), rect.br(), new Scalar(255, 0, 0, 255), 5);
            }
            triggerAction(matSrc.clone(), targetList.get(0));
        }
        Log.w(TAG, "Stage: " + rokidStatus +", " + detectCount + "/" + ContinueDetectCount);
        return matSrc;
    }

    private boolean checkIfDetect() {
        // 如果不是场景的开场，也不用检测
        if (!RokidEnum.isNewStage(rokidStatus)) {
            return false;
        }
        //每FrameSkip帧检测一次
        if (frameCount % FrameSkip != 0) {
            frameCount ++;
            return false;
        }
        frameCount = 0;
        return true;
    }

    private void triggerAction(Mat cloneMat, Rect rect) {
        detectCount = 0;

        // 触发场景1
        if (rokidStatus == RokidEnum.RokidStatus.s00) {
            runOnUiThread(() -> {
                Bitmap bitmap = Bitmap.createBitmap(rect.width, rect.height, Bitmap.Config.ARGB_8888);
                // 截取识别到的图像
                Utils.matToBitmap(cloneMat.submat(rect), bitmap);
                Bitmap bitmap2 = Bitmap.createBitmap(cloneMat.width(), cloneMat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(cloneMat, bitmap2);
                targetShowImageView.setImageBitmap(bitmap);
                fullShowImageView.setImageBitmap(bitmap2);
                showTextView.setText("识别场景1: 27F前台完成");
                videoView.setVisibility(View.VISIBLE);
                videoView.start();
                rokidStatus = RokidEnum.RokidStatus.s01;
            });
        }

    }

}