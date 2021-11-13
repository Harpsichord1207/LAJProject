package cn.harpsichord.lajproject;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

public class OpenCVActivity extends AppCompatActivity {

    private static final String TAG = "OpenCVActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_cvactivity);

        ImageView iv = findViewById(R.id.imageView);

        Button get1Btn = findViewById(R.id.get1_image);
        get1Btn.setOnClickListener(v -> {
            iv.setImageResource(R.drawable.d1);
        });

        Button get2Btn = findViewById(R.id.get2_image);
        get2Btn.setOnClickListener(v -> {
            iv.setImageResource(R.drawable.d2);
        });

        Button b1 = findViewById(R.id.process1_image);
        b1.setOnClickListener(v -> {
            try {
                Mat mat1 = Utils.loadResource(OpenCVActivity.this, R.drawable.d1);
                Mat mat2 = Utils.loadResource(OpenCVActivity.this, R.drawable.d2);
                Mat dst = new Mat();
                Core.bitwise_and(mat1, mat2, dst); // dst = mat1 & mat2
                Imgproc.cvtColor(dst, dst, Imgproc.COLOR_BGR2RGB); // BGT -> RGB
                Bitmap bitmap = Bitmap.createBitmap(dst.width(), dst.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(dst, bitmap);
                iv.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Button b2 = findViewById(R.id.process2_image);
        b2.setOnClickListener(v -> {
            try {
                Mat mat1 = Utils.loadResource(OpenCVActivity.this, R.drawable.d1);
                Mat mat2 = Utils.loadResource(OpenCVActivity.this, R.drawable.d2);
                Mat dst = new Mat();
                Core.bitwise_or(mat1, mat2, dst); // dst = mat1 & mat2
                Imgproc.cvtColor(dst, dst, Imgproc.COLOR_BGR2RGB); // BGT -> RGB
                Bitmap bitmap = Bitmap.createBitmap(dst.width(), dst.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(dst, bitmap);
                iv.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}