package cn.harpsichord.lajproject.rokid;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import cn.harpsichord.lajproject.CameraActivity;
import cn.harpsichord.lajproject.R;

public class RokidNativeCameraTest extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private CameraManager cameraManager;
    private String cameraID;
    private final String logTag = "RokidNative";
    private Size maxSize;

    private HandlerThread backgroundHandlerThread;
    private Handler backgroundHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                cameraID = cameraId;
                Log.w(logTag, "Get CameraID = " + cameraId);
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraID);
                Size[] outputSizes = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
                int maxR = -1;
                for (Size s : outputSizes) {
                    int r = s.getHeight() * s.getWidth();
                    if (r > maxR) {
                        maxSize = s;
                    }
                }
                break;
            }
        } catch (CameraAccessException e) {
            Toast.makeText(this, "Failed to get camera id list!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }
        Log.w(logTag, "Max Size: " + maxSize);
        ImageReader imageReader = ImageReader.newInstance(maxSize.getWidth(), maxSize.getHeight(), ImageFormat.JPEG, 1);
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
        }, backgroundHandler);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(RokidNativeCameraTest.this, new String[]{Manifest.permission.CAMERA}, 1);
            return;
        }

        startBackgroundThread();

        try {
            cameraManager.openCamera(cameraID, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.w(logTag, "Camera on Opened!");
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {

                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.e(logTag, "Open Camera Error Code: " + error);
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(logTag, "Failed to open camera!");
            e.printStackTrace();
        }

    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        Toast.makeText(this, width + " X " + height, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBackgroundThread();
    }

    private void startBackgroundThread() {
        backgroundHandlerThread = new HandlerThread(logTag);
        backgroundHandlerThread.start();
        backgroundHandler = new Handler(backgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundHandlerThread.quitSafely();
        try {
            backgroundHandlerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
