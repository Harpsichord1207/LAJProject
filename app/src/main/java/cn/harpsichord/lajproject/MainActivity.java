package cn.harpsichord.lajproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ButtonBarLayout;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import java.util.Random;

import cn.harpsichord.lajproject.rokid.RokidActivity;
import cn.harpsichord.lajproject.rokid.RokidNativeCameraTest;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    ProgressBar bar;
    FakeLongTask task;

    class FakeLongTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            double a = 0.0;
            Random r = new Random();
            for (;;) {
                if (this.isCancelled()) {
                    Log.e("FakeLongTask", "isCancelled!");
                    break;
                }
                a += Math.random();
                if (a >= 100.0) {
                    a = 100.0;
                }
                int b = (int) a;
                publishProgress(b);
                if (b >= 100) {
                    break;
                }
                long sleep = r.nextInt(200);
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            bar.setProgress(values[0]);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.w(TAG, "Product Name: " + Build.PRODUCT);
        // boolean isRokid = "msm8998".equalsIgnoreCase(Build.PRODUCT);

        Button button = findViewById(R.id.test_button);
        TextView textView = findViewById(R.id.test_text);
        bar = findViewById(R.id.download_progress_bar);

        button.setOnClickListener(v -> {
            String text = textView.getText().toString();
            Log.d(TAG, text);
            long number = Long.parseLong(text);
            number = number + 1;
            textView.setText(String.valueOf(number));
        });

        Button cameraButton = findViewById(R.id.test_camera);
        cameraButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        });

        Button downloadButton = findViewById(R.id.download_button);
        downloadButton.setOnClickListener(v -> {
            cancelTask();
            task = new FakeLongTask();
            task.execute();
        });

        Button opencvBtn = findViewById(R.id.load_opencv);
        opencvBtn.setOnClickListener(v -> {
            if (OpenCVLoader.initDebug()) {
                Intent intent = new Intent(this, OpenCVActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Failed to init OpenCV!", Toast.LENGTH_SHORT).show();
            }
        });

        Button face1Btn = findViewById(R.id.opencv_static_image);
        face1Btn.setOnClickListener(v -> {
            Intent intent = new Intent(this, StaticFaceActivity.class);
            startActivity(intent);
        });

        Button face2Btn = findViewById(R.id.opencv_camera);
        face2Btn.setOnClickListener(v -> {
            Intent intent = new Intent(this, OpenCVCameraActivity.class);
            startActivity(intent);
        });

        Button voiceBtn = findViewById(R.id.voice_button);
        voiceBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, XFActivity.class));
        });

        Button rokidBtn = findViewById(R.id.Rokid_btn);
        rokidBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, RokidActivity.class);
            startActivity(intent);
        });

        Button cigLogoBtn = findViewById(R.id.find_cig_data_button);
        cigLogoBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, OpenCVCameraCIGActivity.class);
            startActivity(intent);
        });

        Button openglBtn = findViewById(R.id.opengl_button);
        openglBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, OpenGLActivity.class);
            startActivity(intent);
        });

        Button vipBtn = findViewById(R.id.vip_button);
        vipBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, VIPActivity.class);
            startActivity(intent);
        });

    }

    private void cancelTask() {
        if (task == null) {return;}
        if (task.isCancelled()) {return;}
        task.cancel(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
        cancelTask();
    }
}

