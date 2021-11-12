package cn.harpsichord.lajproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = findViewById(R.id.test_button);
        TextView textView = findViewById(R.id.test_text);

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

    }
}