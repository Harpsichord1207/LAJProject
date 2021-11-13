package cn.harpsichord.lajproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import java.util.Random;

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

