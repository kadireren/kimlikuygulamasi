package com.example.kimlikuygulamasi;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class IdCardCameraActivity extends AppCompatActivity {
    private static final String TAG = "IdCardCameraActivity";
    private PreviewView previewView;
    private View overlayFrame;
    private ImageCapture imageCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_id_card_camera);

        previewView = findViewById(R.id.previewView);
        overlayFrame = findViewById(R.id.overlayFrame);
        Button captureButton = findViewById(R.id.captureButton);

        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);

        startCamera();

        captureButton.setOnClickListener(v -> overlayFrame.post(this::takePhoto));
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Kamera başlatma hatası", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        File photoFile = new File(getExternalFilesDir(null), "id_photo_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        processCapturedImage(photoFile);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(IdCardCameraActivity.this,
                                "Fotoğraf çekilemedi: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Fotoğraf hatası", exception);
                    }
                }
        );
    }

    private void processCapturedImage(File photoFile) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            Bitmap originalBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath(), options);

            Bitmap rotatedBitmap = rotateBitmapIfRequired(originalBitmap, photoFile);

            Rect cropRect = calculateExactOverlayRect(rotatedBitmap);
            Bitmap croppedBitmap = Bitmap.createBitmap(
                    rotatedBitmap,
                    cropRect.left,
                    cropRect.top,
                    cropRect.width(),
                    cropRect.height()
            );

            File croppedFile = new File(getExternalFilesDir(null),
                    "cropped_" + System.currentTimeMillis() + ".jpg");
            saveBitmapToFile(croppedBitmap, croppedFile);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("image_path", croppedFile.getAbsolutePath());
            setResult(RESULT_OK, resultIntent);
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Görüntü işlenirken hata", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Görüntü işleme hatası", e);
        }
    }

    private Bitmap rotateBitmapIfRequired(Bitmap bitmap, File imageFile) throws IOException {
        ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
        int orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private Rect calculateExactOverlayRect(Bitmap bitmap) {
        // Gerçek bitmap çözünürlüğü
        int imageWidth = bitmap.getWidth();
        int imageHeight = bitmap.getHeight();

        // PreviewView ve overlay boyutları
        int viewWidth = previewView.getWidth();
        int viewHeight = previewView.getHeight();
        int overlayWidth = overlayFrame.getWidth();
        int overlayHeight = overlayFrame.getHeight();

        // Görüntüdeki oran
        float viewAspect = (float) viewWidth / viewHeight;
        float imageAspect = (float) imageWidth / imageHeight;

        int displayedWidth, displayedHeight;
        float scale;

        if (imageAspect > viewAspect) {
            // Görüntü yatay taşmış (letterbox üstten-alttan)
            scale = (float) viewWidth / imageWidth;
            displayedWidth = viewWidth;
            displayedHeight = (int) (imageHeight * scale);
        } else {
            // Görüntü dikey taşmış (letterbox sağdan-soldan)
            scale = (float) viewHeight / imageHeight;
            displayedHeight = viewHeight;
            displayedWidth = (int) (imageWidth * scale);
        }

        // Görüntünün ekranda ortalanmasından kaynaklı offset
        int offsetX = (viewWidth - displayedWidth) / 2;
        int offsetY = (viewHeight - displayedHeight) / 2;

        // overlayFrame’in PreviewView içindeki konumu
        int overlayLeft = overlayFrame.getLeft() - previewView.getLeft() - offsetX;
        int overlayTop = overlayFrame.getTop() - previewView.getTop() - offsetY;

        // bitmap üzerindeki koordinatlara çevir
        float inverseScale = 1 / scale;
        int cropLeft = (int) (overlayLeft * inverseScale);
        int cropTop = (int) (overlayTop * inverseScale);
        int cropWidth = (int) (overlayWidth * inverseScale);
        int cropHeight = (int) (overlayHeight * inverseScale);

        // Sınırları aşmasın
        cropLeft = Math.max(0, cropLeft);
        cropTop = Math.max(0, cropTop);
        cropWidth = Math.min(cropWidth, imageWidth - cropLeft);
        cropHeight = Math.min(cropHeight, imageHeight - cropTop);

        return new Rect(cropLeft, cropTop, cropLeft + cropWidth, cropTop + cropHeight);
    }


    private void saveBitmapToFile(Bitmap bitmap, File file) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
        }
    }
}
