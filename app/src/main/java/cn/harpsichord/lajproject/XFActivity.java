package cn.harpsichord.lajproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
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

public class XFActivity extends AppCompatActivity {

    private static final String TAG = "XFActivity";

    private RecognizerListener recognizerListener = new RecognizerListener() {
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
            Log.d(TAG, recognizerResult.getResultString());
        }

        @Override
        public void onError(SpeechError speechError) {

        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };

    private SynthesizerListener synthesizerListener = new SynthesizerListener() {
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activiry_xf);

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
        SpeechRecognizer recognizer = SpeechRecognizer.createRecognizer(this, recognizerListener);// TODO listener
        recognizer.setParameter(SpeechConstant.RESULT_TYPE, "plain");
        recognizer.setParameter(SpeechConstant.VAD_EOS, "3000");
    }
}
