package cn.harpsichord.lajproject.rokid;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;

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
    private SpeechRecognizer speechRecognizer;
    private final String TAG = "ROKID";

    private Mat mRgba;
    private Size minSize;
    private Size maxSize;

    private JavaCamera2View javaCameraView;
    private VideoView videoView;
    private ImageView pic1;
    private TextView nextHintText;

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
        pic1 = findViewById(R.id.trigger_image_1);
        nextHintText = findViewById(R.id.next_text_hint);
        
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

        SpeechUtility.createUtility(context, SpeechConstant.APPID +"=cc41d66f");
    }

    private void configViews(Context context) {
        videoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.cczs_v1));
        videoView.setOnCompletionListener(mp -> {
            videoView.setVisibility(View.GONE);
            linearLayout2.setVisibility(View.VISIBLE);
            rokidStatus = RokidEnum.RokidStatus.s10;
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
                mRgba.release();
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

        // TODO: 不释放掉内存一直涨，但之前没这个问题呀？
        matGray.release();
        targets.release();

        if (targetList.size() == 0) {
            return matSrc;
        }

        detectCount ++;
        Log.w(TAG, "Stage: " + rokidStatus +", " + detectCount + "/" + ContinueDetectCount);
        if (detectCount >= ContinueDetectCount) {
            for (Rect rect: targetList) {
                Imgproc.rectangle(matSrc, rect.tl(), rect.br(), new Scalar(255, 0, 0, 255), 5);
            }
            triggerAction(matSrc.clone(), targetList.get(0));
            detectCount = 0;
        }
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
        Log.w(TAG, "Start Action with status: " + rokidStatus);
        // TODO: 复用公共代码
        // 触发场景1
        if (rokidStatus == RokidEnum.RokidStatus.s00) {
            rokidStatus = RokidEnum.RokidStatus.s01;
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
                cloneMat.release();
            });
        } else if (rokidStatus == RokidEnum.RokidStatus.s10) {
            rokidStatus = RokidEnum.RokidStatus.s11;
            runOnUiThread(()->{
                Bitmap bitmap = Bitmap.createBitmap(rect.width, rect.height, Bitmap.Config.ARGB_8888);
                // 截取识别到的图像
                Utils.matToBitmap(cloneMat.submat(rect), bitmap);
                Bitmap bitmap2 = Bitmap.createBitmap(cloneMat.width(), cloneMat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(cloneMat, bitmap2);
                targetShowImageView2.setImageBitmap(bitmap);
                fullShowImageView2.setImageBitmap(bitmap2);
                showTextView2.setText("识别场景2: 27F展板1完成");
                pic1.setVisibility(View.VISIBLE);
                pic1.setAlpha(.4f);

                String text = "比特视界(北京)科技有限公司(英文简称: BITONE)，成立于2009年，为新意互动旗下子公司，总部位于北京，在上海、底特律(美国)，均设有分支机构。";
                text += "自2009年成立以来，BITONE 已在汽车虚拟影像领域深耕10余年，成为汽车行业备受认可的虚拟影像品牌。";
                text += "我们采用高端计算机图像技术与富有创意的产品可视化方案结合、全数字技术与真实拍摄结合等方式，";
                text += "为客户输出包括数字CG影像、AR/VR虚拟互动体验、沉浸式数字运营整体解决方案以及数字演员等虚拟影像应用解决方案。";
                text += "用于发布会、车展、TVC广告、交互体验等一系列品牌展示及整合营销中，帮助客户重塑品牌与视觉定位，抢占展览体验及市场营销先机，吸引潜在受众。";

                speakText(text, RokidEnum.RokidStatus.s12);
                cloneMat.release();
            });
        }
    }

    private void speakText(String text, RokidEnum.RokidStatus finishStatus) {

        // TODO: stop after quit activity?
        SpeechSynthesizer synthesizer = SpeechSynthesizer.createSynthesizer(RokidActivity.this, null);
        synthesizer.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        synthesizer.setParameter(SpeechConstant.SPEED, "50");
        synthesizer.setParameter(SpeechConstant.VOLUME, "100");
        synthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);

        SynthesizerListener synthesizerListener = new SynthesizerListener() {
            @Override
            public void onSpeakBegin() {

            }

            @Override
            public void onBufferProgress(int i, int i1, int i2, String s) {

            }

            @Override
            public void onSpeakPaused() {

            }

            @Override
            public void onSpeakResumed() {

            }

            @Override
            public void onSpeakProgress(int i, int i1, int i2) {

            }

            @Override
            public void onCompleted(SpeechError speechError) {
                if (speechError != null) {
                    Log.e(TAG, "Speak Error:  " + speechError);
                } else {
                    rokidStatus = finishStatus;
                    if (finishStatus == RokidEnum.RokidStatus.s12) {
                        nextHintText.setVisibility(View.VISIBLE);
                        speakText("请说下一个播放新展板", RokidEnum.RokidStatus.s13);
                    } else if (finishStatus == RokidEnum.RokidStatus.s13) {
                        // 开启语音识别
                        listenText("下一个", RokidEnum.RokidStatus.s14);
                    } else if (finishStatus == RokidEnum.RokidStatus.s15) {
                        linearLayout3.setVisibility(View.VISIBLE);
                        pic1.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        };
        synthesizer.startSpeaking(text, synthesizerListener);
    }

    private void listenText(String stopKeyword, RokidEnum.RokidStatus stopStatus){
        speechRecognizer = SpeechRecognizer.createRecognizer(RokidActivity.this, i -> Log.w(TAG, "SpeechRecognizer Init!"));
        speechRecognizer.setParameter(SpeechConstant.RESULT_TYPE, "plain");
        speechRecognizer.setParameter(SpeechConstant.VAD_EOS, "3000");
        speechRecognizer.startListening(new RecognizerListener() {
            @Override
            public void onVolumeChanged(int i, byte[] bytes) {

            }

            @Override
            public void onBeginOfSpeech() {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onResult(RecognizerResult recognizerResult, boolean b) {
                String resultString = recognizerResult.getResultString();
                Log.w(TAG, "Current Recognize Result: " + resultString);
                if (resultString.contains(stopKeyword.trim())) {
                    Log.w(TAG, "SpeechRecognizer got keyword!!!");
                    speechRecognizer.stopListening();
                    rokidStatus = stopStatus;
                    // TODO Trigger S13 图文播放
                    Toast.makeText(RokidActivity.this, "Current Status: " + rokidStatus, Toast.LENGTH_LONG).show();
                    runOnUiThread(()->{
                        nextHintText.setVisibility(View.GONE);
                        pic1.setImageBitmap(BitmapFactory.decodeResource(RokidActivity.this.getResources(), R.drawable.bigdata_pic));
                        String text = "CIG大数据及应用聚焦于汽车营销垂直领域，以受众数据为基础，整合线上媒介行为数据、汽车类垂直类型媒体数据、腾讯类社交娱乐数据，以及其他互联网公开数据，整合成自有的中国汽车受众数据中心（CAA），通过自研技术、算法和AI能力，搭建了服务于全数字营销链条的应用服务体系，包括数据即服务（DAAS）、信息即服务（IAAS）、结果即服务（AAAS）。";
                        speakText(text, RokidEnum.RokidStatus.s15);
                    });
                }
            }

            @Override
            public void onError(SpeechError speechError) {
                Log.w(TAG, "Listen Error: " + speechError);
            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        });
    }

}