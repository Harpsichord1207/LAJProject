package cn.harpsichord.lajproject.rokid;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import cn.harpsichord.lajproject.R;

public class RokidNativeCameraTest extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private CameraManager cameraManager;
    private String cameraID;
    private final String logTag = "RokidNative";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            for (String cameraId: cameraManager.getCameraIdList()) {
                cameraID = cameraId;
                Log.w(logTag, "Get CameraID = " + cameraId);
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraID);
                Size[] outputSizes = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
                Log.w(logTag, "Size: " + outputSizes);
                break;
            }
        } catch (CameraAccessException e) {
            Toast.makeText(this, "Failed to get camera id list!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
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
}
