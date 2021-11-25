package cn.harpsichord.lajproject;

import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alphamovie.lib.AlphaMovieView;


public class OpenGLActivity extends AppCompatActivity {

    private AlphaMovieView alphaMovieView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_glactivity);

        alphaMovieView = findViewById(R.id.alpha_movie_test);
        alphaMovieView.setVideoFromUri(this, Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alpha_channel_test));
    }

    @Override
    protected void onResume() {
        super.onResume();
        alphaMovieView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        alphaMovieView.onPause();
    }
}

