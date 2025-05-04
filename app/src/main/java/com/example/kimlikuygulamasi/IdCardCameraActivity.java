package com.example.kimlikuygulamasi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IdCardCameraActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final String TAG = "IdCardCameraActivity";

    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;

    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private boolean isFront;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_id_card_camera);

        isFront = getIntent().getBooleanExtra("is_front", true);

        TextView instructionText = findViewById(R.id.instruction_text);
        Button captureButton = findViewById(R.id.btn_capture);
        textureView = findViewById(R.id.camera_preview);

        textureView.setSurfaceTextureListener(textureListener);

        instructionText.setText(isFront ? getString(R.string.kimlik_on_instruction) : getString(R.string.kimlik_arka_instruction));

        checkCameraPermission();

        captureButton.setOnClickListener(v -> takePicture());
    }

    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {}
    };

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (textureView.isAvailable()) {
                    openCamera();
                } else {
                    textureView.setSurfaceTextureListener(textureListener);
                }
            } else {
                String errorMsg = getString(R.string.camera_permission_denied);
                Log.e(TAG, errorMsg);
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = null;
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id;
                    break;
                }
            }
            if (cameraId == null) {
                String errorMsg = getString(R.string.error_no_back_camera);
                Log.e(TAG, errorMsg);
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (map != null) {
                imageDimension = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class));
                imageReader = ImageReader.newInstance(imageDimension.getWidth(), imageDimension.getHeight(),
                        ImageFormat.JPEG, 1);
                imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);
            } else {
                String errorMsg = getString(R.string.error_camera_config);
                Log.e(TAG, errorMsg);
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(cameraId, stateCallback, null);
            } else {
                String errorMsg = getString(R.string.camera_permission_denied);
                Log.e(TAG, errorMsg);
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (CameraAccessException e) {
            String errorMsg = getString(R.string.error_camera_access) + e.getMessage();
            Log.e(TAG, errorMsg, e);
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private Size chooseOptimalSize(Size[] choices) {
        // Kamera için en uygun çözünürlüğü seç (1080p'ye yakın olan)
        int targetWidth = 1920;
        int targetHeight = 1080;
        Size optimalSize = choices[0];
        int minDiff = Integer.MAX_VALUE;

        for (Size size : choices) {
            int diff = Math.abs(size.getWidth() - targetWidth) + Math.abs(size.getHeight() - targetHeight);
            if (diff < minDiff) {
                optimalSize = size;
                minDiff = diff;
            }
        }
        return optimalSize;
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, getString(R.string.camera_opened_successfully));
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, getString(R.string.camera_disconnected));
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            String errorMsg = getString(R.string.error_camera_open) + error;
            Log.e(TAG, errorMsg);
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
            Toast.makeText(IdCardCameraActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
        }
    };

    private void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null || imageDimension == null) {
                Log.e(TAG, getString(R.string.error_preview_texture));
                return;
            }

            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, getString(R.string.camera_preview_configured));
                    cameraCaptureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    String errorMsg = getString(R.string.error_preview_configuration);
                    Log.e(TAG, errorMsg);
                    Toast.makeText(IdCardCameraActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            String errorMsg = getString(R.string.error_preview_creation) + e.getMessage();
            Log.e(TAG, errorMsg, e);
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null) {
            Log.e(TAG, getString(R.string.error_device_null));
            return;
        }

        try {
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
            Log.d(TAG, getString(R.string.camera_preview_updated));
        } catch (CameraAccessException e) {
            String errorMsg = getString(R.string.error_preview_update) + e.getMessage();
            Log.e(TAG, errorMsg, e);
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
        }
    }

    private void takePicture() {
        if (cameraDevice == null) {
            Log.e(TAG, getString(R.string.error_device_null));
            return;
        }

        try {
            // Geçici resim yüzeyi oluştur
            Size[] jpegSizes = null;
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (map != null) {
                jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
            }

            int width = 640; // Varsayılan çözünürlük
            int height = 480;

            if (jpegSizes != null && jpegSizes.length > 0) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }

            // Değişken adı değiştirildi: reader -> stillImageReader
            ImageReader stillImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(stillImageReader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(stillImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // Fotoğrafı kaydet ve MainActivity'ye geri dön
            final File file = createImageFile();
            ImageReader.OnImageAvailableListener readerListener = imageReader -> {
                try (Image image = imageReader.acquireLatestImage()) {
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                    saveImageToFile(bytes, file);
                } catch (Exception e) {
                    Log.e(TAG, getString(R.string.error_saving_image) + e.getMessage(), e);
                }
            };

            stillImageReader.setOnImageAvailableListener(readerListener, backgroundHandler);

            final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(IdCardCameraActivity.this, getString(R.string.photo_captured), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, getString(R.string.photo_saved) + file.getPath());

                    // Sonucu MainActivity'ye geri döndür
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("image_path", file.getAbsolutePath());
                    resultIntent.putExtra("is_front", isFront);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            };

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureCallback, backgroundHandler);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, getString(R.string.error_camera_capture) + e.getMessage(), e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, getString(R.string.error_capture_session));
                }
            }, backgroundHandler);

        } catch (CameraAccessException | IOException e) {
            Log.e(TAG, getString(R.string.error_taking_picture) + e.getMessage(), e);
        }
    }

    private final ImageReader.OnImageAvailableListener onImageAvailableListener = reader -> {
        Image image = null;
        try {
            image = reader.acquireLatestImage();
            if (image != null) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);

                File imageFile = createImageFile();
                saveImageToFile(bytes, imageFile);

                // Sonucu MainActivity'ye geri döndür
                Intent resultIntent = new Intent();
                resultIntent.putExtra("image_path", imageFile.getAbsolutePath());
                resultIntent.putExtra("is_front", isFront);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, getString(R.string.error_processing_image) + e.getMessage(), e);
        } finally {
            if (image != null) {
                image.close();
            }
        }
    };

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = (isFront ? "FRONT_" : "BACK_") + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void saveImageToFile(byte[] bytes, File file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
            Log.d(TAG, getString(R.string.image_saved_successfully) + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, getString(R.string.error_saving_image_file) + e.getMessage(), e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        stopBackgroundThread();
        closeCamera();
        super.onPause();
    }

    private void closeCamera() {
        Log.d(TAG, getString(R.string.closing_camera));
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        Log.d(TAG, getString(R.string.background_thread_started));
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            Log.d(TAG, getString(R.string.background_thread_stopped));
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                String errorMsg = getString(R.string.error_stopping_background) + e.getMessage();
                Log.e(TAG, errorMsg, e);
            }
        }
    }
}