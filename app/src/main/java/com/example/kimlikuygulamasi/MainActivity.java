package com.example.kimlikuygulamasi;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private static final String TAG = "MainActivity";

    private Button btnKimlikOn;
    private Button btnKimlikArka;
    private Button btnFormaGec;

    private String kimlikOnPath;
    private String kimlikArkaPath;

    private static final int REQUEST_IMAGE_CAPTURE_FRONT = 1;
    private static final int REQUEST_IMAGE_CAPTURE_BACK = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        checkPermissions();
        setupButtonListeners();
        btnFormaGec.setEnabled(false);
    }

    private void initializeViews() {
        btnKimlikOn = findViewById(R.id.btn_kimlik_on);
        btnKimlikArka = findViewById(R.id.btn_kimlik_arka);
        btnFormaGec = findViewById(R.id.btn_forma_gec);
    }

    private void checkPermissions() {
        if (allPermissionsGranted()) {
            enableCameraFeatures();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void setupButtonListeners() {
        btnKimlikOn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, IdCardCameraActivity.class);
            intent.putExtra("is_front", true);
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE_FRONT);
        });

        btnKimlikArka.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, IdCardCameraActivity.class);
            intent.putExtra("is_front", false);
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE_BACK);
        });

        btnFormaGec.setOnClickListener(v -> navigateToForm());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            String originalPath = data.getStringExtra("image_path");
            boolean isFront = (requestCode == REQUEST_IMAGE_CAPTURE_FRONT);

            if (originalPath != null) {
                processImageResult(originalPath, isFront);
            } else {
                Toast.makeText(this, "Resim yolu alınamadı", Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Kamera işlemi iptal edildi", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Kamera hatası", Toast.LENGTH_SHORT).show();
        }
    }

    private void processImageResult(String originalPath, boolean isFront) {
        Bitmap originalBitmap = BitmapFactory.decodeFile(originalPath);
        if (originalBitmap != null) {
            Log.d(TAG, "Orijinal bitmap yüklendi: " + originalPath);

            // Basit merkezi kırpma işlemi uygula
            Bitmap croppedBitmap = autoCropIdCard(originalBitmap);

            if (croppedBitmap != null) {
                String croppedImagePath = saveCroppedImageToFile(croppedBitmap, isFront);

                if (croppedImagePath != null) {
                    if (isFront) {
                        kimlikOnPath = croppedImagePath;
                        btnKimlikOn.setText("Kimlik Ön Yüzü Çekildi");
                        Toast.makeText(this, "Kimlik ön yüzü kaydedildi", Toast.LENGTH_SHORT).show();
                    } else {
                        kimlikArkaPath = croppedImagePath;
                        btnKimlikArka.setText("Kimlik Arka Yüzü Çekildi");
                        Toast.makeText(this, "Kimlik arka yüzü kaydedildi", Toast.LENGTH_SHORT).show();
                    }
                    checkAndEnableFormButton();
                } else {
                    Toast.makeText(this, "Kırpılmış görüntü kaydedilemedi", Toast.LENGTH_SHORT).show();
                }

                if (croppedBitmap != originalBitmap) {
                    croppedBitmap.recycle();
                }
            } else {
                Toast.makeText(this, "Kırpma işlemi başarısız oldu", Toast.LENGTH_SHORT).show();
            }

            originalBitmap.recycle();
        } else {
            Toast.makeText(this, "Resim yüklenemedi", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap autoCropIdCard(Bitmap originalBitmap) {
        try {
            int originalWidth = originalBitmap.getWidth();
            int originalHeight = originalBitmap.getHeight();

            // Orijinal boyutun %80'i kadar merkezden kırpma
            int targetWidth = (int) (originalWidth * 0.8);
            int targetHeight = (int) (originalHeight * 0.8);

            // Kırpılacak alanın sol üst köşesinin koordinatları
            int left = (originalWidth - targetWidth) / 2;
            int top = (originalHeight - targetHeight) / 2;

            // Kırpma sınırlarının geçerli olduğundan emin ol
            if (left < 0 || top < 0 || targetWidth <= 0 || targetHeight <= 0 ||
                    left + targetWidth > originalWidth || top + targetHeight > originalHeight) {
                Log.e(TAG, "Geçersiz kırpma boyutları");
                return originalBitmap;
            }

            // Bitmap'in merkez kısmını kırp
            return Bitmap.createBitmap(originalBitmap, left, top, targetWidth, targetHeight);
        } catch (Exception e) {
            Log.e(TAG, "Bitmap kırpma hatası: " + e.getMessage(), e);
            return originalBitmap;
        }
    }

    private String saveCroppedImageToFile(Bitmap bitmap, boolean isFront) {
        File photoFile = null;
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = (isFront ? "CROPPED_FRONT_" : "CROPPED_BACK_") + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (storageDir == null) {
                Log.e(TAG, "Harici depolama alanı (Pictures) bulunamadı.");
                return null;
            }
            photoFile = File.createTempFile(imageFileName, ".jpg", storageDir);

            try (FileOutputStream out = new FileOutputStream(photoFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                Log.d(TAG, "Kırpılmış fotoğraf kaydedildi: " + photoFile.getAbsolutePath());
                return photoFile.getAbsolutePath();
            } catch (IOException e) {
                Log.e(TAG, "Kırpılmış fotoğraf dosyaya yazılamadı: " + e.getMessage(), e);
                return null;
            }
        } catch (IOException ex) {
            Log.e(TAG, "Kırpılmış fotoğraf dosyası oluşturulamadı: " + ex.getMessage(), ex);
            return null;
        }
    }

    private void navigateToForm() {
        if (kimlikOnPath != null && kimlikArkaPath != null) {
            Intent intent = new Intent(MainActivity.this, FormActivity.class);
            intent.putExtra("kimlik_on_path", kimlikOnPath);
            intent.putExtra("kimlik_arka_path", kimlikArkaPath);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Lütfen kimlik kartının her iki yüzünü de çekin", Toast.LENGTH_SHORT).show();
        }
    }

    private void enableCameraFeatures() {
        btnKimlikOn.setEnabled(true);
        btnKimlikArka.setEnabled(true);
        Log.d(TAG, "Kamera izinleri alındı");
    }

    private void checkAndEnableFormButton() {
        btnFormaGec.setEnabled(kimlikOnPath != null && kimlikArkaPath != null);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                enableCameraFeatures();
            } else {
                Toast.makeText(this, "Kamera izni reddedildi", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}