package com.example.kimlikuygulamasi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IdCardCameraActivity extends AppCompatActivity {
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

    // TextureView.SurfaceTextureListener'ı sınıfın kendisi üzerinden implemente edelim
    // bu şekilde field olarak tutmayalım

    private final ActivityResultLauncher<String[]> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    permissions -> {
                        boolean allGranted = true;
                        for (Boolean isGranted : permissions.values()) {
                            if (!isGranted) {
                                allGranted = false;
                                break;
                            }
                        }

                        if (allGranted) {
                            if (textureView != null && textureView.isAvailable()) {
                                openCamera();
                            } else if (textureView != null) {
                                // setOnSurfaceTextureAvailableListener metodu ile event tanımlayalım
                                setupTextureViewListener();
                            } else {
                                Log.e(TAG, "TextureView bulunamadı!");
                                Toast.makeText(this, "Kamera görüntüsü hazırlanamadı", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            String errorMsg = getString(R.string.camera_permission_denied);
                            Log.e(TAG, errorMsg);
                            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_id_card_camera);

        isFront = getIntent().getBooleanExtra("is_front", true);

        // View'ları initialize et
        initializeViews();

        // İzinleri kontrol et
        checkCameraPermission();
    }

    private void setupTextureViewListener() {
        try {
            if (textureView == null) return;

            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
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
            });
        } catch (Exception e) {
            Log.e(TAG, "TextureView listener ayarlanırken hata: " + e.getMessage());
        }
    }

    private void initializeViews() {
        try {
            TextView instructionText = findViewById(R.id.instruction_text);
            if (instructionText != null) {
                instructionText.setText(isFront ?
                        getString(R.string.kimlik_on_instruction) :
                        getString(R.string.kimlik_arka_instruction));
            }

            Button captureButton = findViewById(R.id.btn_capture);
            if (captureButton != null) {
                captureButton.setOnClickListener(v -> takePicture());
            }

            textureView = findViewById(R.id.camera_preview);
            if (textureView != null) {
                setupTextureViewListener();
            } else {
                Log.e(TAG, "camera_preview TextureView bulunamadı!");
                Toast.makeText(this, "Kamera arayüzü hazırlanamadı", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "View'lar başlatılırken hata: " + e.getMessage(), e);
            Toast.makeText(this, "Arayüz öğeleri yüklenirken hata oluştu", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission.launch(new String[]{Manifest.permission.CAMERA});
        } else if (textureView != null) {
            if (textureView.isAvailable()) {
                openCamera();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        if (textureView == null) {
            Log.e(TAG, "TextureView null, kamera açılamıyor!");
            return;
        }

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
        } catch (Exception e) {
            Log.e(TAG, "Kamera açılırken beklenmeyen hata: " + e.getMessage(), e);
            Toast.makeText(this, "Kamera başlatılırken hata oluştu", Toast.LENGTH_SHORT).show();
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
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
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
            if (textureView == null) {
                Log.e(TAG, "TextureView null, önizleme oluşturulamıyor!");
                return;
            }

            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null || imageDimension == null) {
                Log.e(TAG, getString(R.string.error_preview_texture));
                return;
            }

            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);

            if (cameraDevice == null) {
                Log.e(TAG, "CameraDevice null, capture request oluşturulamıyor!");
                return;
            }

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
        } catch (Exception e) {
            Log.e(TAG, "Kamera önizlemesi oluşturulurken beklenmeyen hata: " + e.getMessage(), e);
            Toast.makeText(this, "Kamera önizlemesi hazırlanamadı", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null || captureRequestBuilder == null || cameraCaptureSession == null) {
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

            ImageReader stillImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(stillImageReader.getSurface());

            if (textureView != null && textureView.getSurfaceTexture() != null) {
                outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            }

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(stillImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // Fotoğrafı kaydet ve MainActivity'ye geri dön
            final File file = createImageFile();
            ImageReader.OnImageAvailableListener readerListener = imageReader -> {
                try (Image image = imageReader.acquireLatestImage()) {
                    if (image != null) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        saveImageToFile(bytes, file);
                    }
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
        } catch (Exception e) {
            Log.e(TAG, "Fotoğraf çekerken beklenmeyen hata: " + e.getMessage(), e);
            Toast.makeText(this, "Fotoğraf çekilemedi. Tekrar deneyiniz.", Toast.LENGTH_SHORT).show();
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
        if (textureView != null) {
            if (textureView.isAvailable()) {
                openCamera();
            } else {
                setupTextureViewListener();
            }
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
            try {
                cameraCaptureSession.close();
            } catch (Exception e) {
                Log.e(TAG, "Kamera oturumu kapatılırken hata: " + e.getMessage(), e);
            } finally {
                cameraCaptureSession = null;
            }
        }
        if (cameraDevice != null) {
            try {
                cameraDevice.close();
            } catch (Exception e) {
                Log.e(TAG, "Kamera cihazı kapatılırken hata: " + e.getMessage(), e);
            } finally {
                cameraDevice = null;
            }
        }
        if (imageReader != null) {
            try {
                imageReader.close();
            } catch (Exception e) {
                Log.e(TAG, "ImageReader kapatılırken hata: " + e.getMessage(), e);
            } finally {
                imageReader = null;
            }
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
            try {
                Log.d(TAG, getString(R.string.background_thread_stopped));
                backgroundThread.quitSafely();
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