package cn.harpsichord.lajproject;

import android.content.Context;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

//import com.alphamovie.lib.AlphaMovieView;
import java.io.File;

import cn.harpsichord.lajproject.CIGAlphaMovie.AlphaMovieView;
import cn.harpsichord.lajproject.CIGAlphaMovie.VideoRenderer;

public class OpenGLActivity extends AppCompatActivity {

    private static final String TAG = "OpenGLActivity";
    private AlphaMovieView alphaMovieView;
    private static String alphaVideoUri = "https://cig-test.s3.cn-north-1.amazonaws.com.cn/liutao/CIGAR/media/alpha_channel_test.mp4";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_glactivity);


        SeekBar sb = findViewById(R.id.change_alpha_seekbar);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                System.out.println("Current Alpha: " + progress / 100.0F);
                // 这个并不是设置视频绿幕的alpha，而是整个view的，没有提供相关方法单独设置绿幕的
                 alphaMovieView.setAlpha(progress / 100.0F);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // 手机文件管理器的根目录
        // File dir = Environment.getExternalStorageDirectory();
        // File f = new File(dir, "cczs.mov");

        String videoFilePath = "android.resource://" + getPackageName() + "/" + R.raw.cczs;


        alphaMovieView = findViewById(R.id.alpha_movie_test);
        // alphaMovieView.setVideoFromUri(this, Uri.parse(videoFilePath));  // stuck here for waiting video load?
        alphaMovieView.setVideoByUrl(alphaVideoUri);
        alphaMovieView.start();
        VideoView videoView = findViewById(R.id.local_mov_test);
        videoView.setVideoURI(Uri.parse(videoFilePath));
        videoView.start();
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
