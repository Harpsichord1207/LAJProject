package cn.harpsichord.lajproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;

import java.util.ArrayList;
import java.util.List;

public class XFActivity extends AppCompatActivity {

    private static final String TAG = "XFActivity";

    private TextView listenText;

    private RecognizerListener recognizerListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int i, byte[] bytes) {

        }

        @Override
        public void onBeginOfSpeech() {
            Toast.makeText(XFActivity.this, "Speak!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onEndOfSpeech() {
            switchStatus();
            Toast.makeText(XFActivity.this, "Stop!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            String resultString = recognizerResult.getResultString();
            final CharSequence text = resultString + " " + listenText.getText();
            listenText.setText(text);  // TODO: run on ui thread?
        }

        @Override
        public void onError(SpeechError speechError) {
            Toast.makeText(XFActivity.this, speechError.toString(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };

    private boolean recognizerRunning = false;

    private final SynthesizerListener synthesizerListener = new SynthesizerListener() {
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
                Toast.makeText(XFActivity.this, "发生错误，请重试！", Toast.LENGTH_SHORT).show();
                Log.e(TAG, speechError.toString());
            }
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };

    private SpeechRecognizer recognizer;
    private Button listenBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activiry_xf);

        requestPermissions(); //TODO: 处理请求权限失败的情况

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);
        }
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=cc41d66f");

        // speak
        SpeechSynthesizer synthesizer = SpeechSynthesizer.createSynthesizer(this, null);
        synthesizer.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        synthesizer.setParameter(SpeechConstant.SPEED, "50");
        synthesizer.setParameter(SpeechConstant.VOLUME, "100");
        synthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);

        Button speak = findViewById(R.id.speak_button);
        EditText content = findViewById(R.id.iat_text);

        speak.setOnClickListener(v -> {
            String s = content.getText().toString().trim();
            if ("".equals(s)) {
                Toast.makeText(this, "请输入文本", Toast.LENGTH_SHORT).show();
            } else {
                synthesizer.startSpeaking(s, synthesizerListener);
            }
        });

        // recognise
        recognizer = SpeechRecognizer.createRecognizer(this, i -> System.out.println("SpeechRecognizer Init"));
        recognizer.setParameter(SpeechConstant.RESULT_TYPE, "plain");
        recognizer.setParameter(SpeechConstant.VAD_EOS, "3000");

        listenBtn = findViewById(R.id.listen_button);
        listenText = findViewById(R.id.listen_text);
        listenBtn.setOnClickListener(v -> {
            switchStatus();
        });
    }

    private void switchStatus() {
        if (recognizerRunning) {
            recognizer.stopListening();
            recognizerRunning = false;
            listenBtn.setText("Listen");
        } else {
            recognizer.startListening(recognizerListener);
            recognizerRunning = true;
            listenBtn.setText("Listening");
        }
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.INTERNET);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }

        if (permissions.isEmpty()) {
            return;
        }

        ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), 1);
    }

}
