package cn.harpsichord.lajproject;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.Collections;


public class CameraActivity extends Activity {

    private static final String TAG = "CameraActivity";
    private Handler handler;
    private TextureView textureView;
    private CaptureRequest.Builder mPreviewBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        HandlerThread handlerThread = new HandlerThread("CAMERA2");
        handlerThread.start();

        handler = new Handler(handlerThread.getLooper());

        textureView = findViewById(R.id.camera_texture_view);

        Button clkBtn = findViewById(R.id.button_in_camera);
        clkBtn.setOnClickListener(v -> Toast.makeText(CameraActivity.this, "暂时没什么用", Toast.LENGTH_SHORT).show());

    }

    @Override
    protected void onResume() {
        super.onResume();
        textureView.setSurfaceTextureListener(surfaceTextureListener);
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {

            CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

            try {
                String[] cameraList = cameraManager.getCameraIdList();
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraList[0]);
                Integer integer = cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                Log.d(TAG, "SUPPORTED_HARDWARE_LEVEL = " + integer);
                if (ActivityCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Ask Camera Permission!");
                    ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
                    return;
                }
                Log.d(TAG, "Start to open camera.");
                cameraManager.openCamera(cameraList[0], stateCallback, handler);
            } catch (CameraAccessException e) {
                Log.e(TAG, "CameraAccessException!");
                e.printStackTrace();
            }

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
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            try {
                startPreview(camera);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.e(TAG, "onDisconnected");
            camera.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "onError");
            camera.close();
        }
    };

    private void startPreview(CameraDevice cameraDevice) throws CameraAccessException {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
        Surface surface = new Surface(surfaceTexture);
        try {
            mPreviewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mPreviewBuilder.addTarget(surface);
        cameraDevice.createCaptureSession(Collections.singletonList(surface), mSessionStateCallback, handler);
    }

    private final CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            try {
                session.capture(mPreviewBuilder.build(), mSessionCaptureCallback, handler);
                session.setRepeatingRequest(mPreviewBuilder.build(), mSessionCaptureCallback, handler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
          }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.e(TAG,"相机创建失败！");
        }
    };

    private final CameraCaptureSession.CaptureCallback mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult){
        }
    };
}