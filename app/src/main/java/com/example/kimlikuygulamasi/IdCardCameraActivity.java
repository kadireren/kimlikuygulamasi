package com.example.kimlikuygulamasi;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import androidx.camera.core.ImageCapture.OutputFileOptions;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

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

        previewView   = findViewById(R.id.previewView);
        overlayFrame  = findViewById(R.id.overlayFrame);
        Button captureButton = findViewById(R.id.captureButton);

        // Programatik olarak FIT_CENTER ölçeğini ve kompatible modu uygula
        previewView.setImplementationMode(
                PreviewView.ImplementationMode.COMPATIBLE
        );
        previewView.setScaleType(
                PreviewView.ScaleType.FIT_CENTER
        );

        startCamera();

        captureButton.setOnClickListener(v ->
                overlayFrame.post(this::takePhoto)
        );
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> camProviderFut =
                ProcessCameraProvider.getInstance(this);

        camProviderFut.addListener(() -> {
            try {
                ProcessCameraProvider cp = camProviderFut.get();
                Preview preview     = new Preview.Builder().build();
                imageCapture       = new ImageCapture.Builder().build();
                CameraSelector sel = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                cp.unbindAll();
                cp.bindToLifecycle(this, sel, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Kamera başlatılamadı", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        File origFile = new File(getExternalFilesDir(null),
                "orig_" + System.currentTimeMillis() + ".jpg");
        OutputFileOptions opts = new OutputFileOptions.Builder(origFile).build();

        imageCapture.takePicture(opts, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults out) {
                        Bitmap fullBmp = BitmapFactory.decodeFile(origFile.getAbsolutePath());
                        Rect cropRect  = calculateCropRect(fullBmp);
                        if (cropRect.width() <= 0 || cropRect.height() <= 0) {
                            Toast.makeText(
                                    IdCardCameraActivity.this,
                                    "Kırpma bölgesi geçersiz", Toast.LENGTH_SHORT
                            ).show();
                            Log.e(TAG, "Invalid cropRect: " + cropRect);
                            return;
                        }
                        Bitmap cropped = Bitmap.createBitmap(
                                fullBmp,
                                cropRect.left, cropRect.top,
                                cropRect.width(), cropRect.height()
                        );

                        File cropFile = new File(getExternalFilesDir(null),
                                "crop_" + System.currentTimeMillis() + ".jpg");
                        try (FileOutputStream fos = new FileOutputStream(cropFile)) {
                            cropped.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                        } catch (IOException e) {
                            Log.e(TAG, "Cropped save failed", e);
                        }

                        Intent result = new Intent();
                        result.putExtra("image_path", cropFile.getAbsolutePath());
                        setResult(RESULT_OK, result);
                        finish();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Toast.makeText(
                                IdCardCameraActivity.this,
                                "Fotoğraf alınırken hata: " + exc.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                        Log.e(TAG, "Photo capture failed", exc);
                    }
                }
        );
    }

    private Rect calculateCropRect(Bitmap bmp) {
        int vw = previewView.getWidth();
        int vh = previewView.getHeight();
        int iw = bmp.getWidth();
        int ih = bmp.getHeight();

        float scale = Math.min((float) iw / vw, (float) ih / vh);
        float scaledW = vw * scale;
        float scaledH = vh * scale;

        float dx = (iw - scaledW) / 2f;
        float dy = (ih - scaledH) / 2f;

        float ovlL = overlayFrame.getLeft();
        float ovlT = overlayFrame.getTop();
        float ovlW = overlayFrame.getWidth();
        float ovlH = overlayFrame.getHeight();

        int left   = Math.max(0, (int)(ovlL * scale + dx));
        int top    = Math.max(0, (int)(ovlT * scale + dy));
        int width  = (int)(ovlW * scale);
        int height = (int)(ovlH * scale);

        int right  = Math.min(iw,  left + width);
        int bottom = Math.min(ih, top + height);

        return new Rect(left, top, right, bottom);
    }
}
