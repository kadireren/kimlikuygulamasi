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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

    // ActivityResultLauncher için tanımlama
    private final ActivityResultLauncher<Intent> frontCardLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    processImageResult(result.getData().getStringExtra("image_path"), true);
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, R.string.camera_cancelled, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.error_camera_generic, Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Intent> backCardLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    processImageResult(result.getData().getStringExtra("image_path"), false);
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, R.string.camera_cancelled, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.error_camera_generic, Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String[]> requestMultiplePermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                boolean allGranted = true;
                for (Boolean isGranted : permissions.values()) {
                    if (!isGranted) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted) {
                    enableCameraFeatures();
                } else {
                    Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
    );

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
            requestMultiplePermissions.launch(REQUIRED_PERMISSIONS);
        }
    }

    private void setupButtonListeners() {
        btnKimlikOn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, IdCardCameraActivity.class);
            intent.putExtra("is_front", true);
            frontCardLauncher.launch(intent);
        });

        btnKimlikArka.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, IdCardCameraActivity.class);
            intent.putExtra("is_front", false);
            backCardLauncher.launch(intent);
        });

        btnFormaGec.setOnClickListener(v -> navigateToForm());
    }

    private void processImageResult(String originalPath, boolean isFront) {
        Bitmap originalBitmap = BitmapFactory.decodeFile(originalPath);
        if (originalBitmap != null) {
            Log.d(TAG, getString(R.string.photo_saved) + " " + originalPath);

            // Basit merkezi kırpma işlemi uygula
            Bitmap croppedBitmap = autoCropIdCard(originalBitmap);

            if (croppedBitmap != null) {
                String croppedImagePath = saveCroppedImageToFile(croppedBitmap, isFront);

                if (croppedImagePath != null) {
                    if (isFront) {
                        kimlikOnPath = croppedImagePath;
                        btnKimlikOn.setText(R.string.kimlik_on_button_text);
                        Toast.makeText(this, R.string.kimlik_on_saved, Toast.LENGTH_SHORT).show();
                    } else {
                        kimlikArkaPath = croppedImagePath;
                        btnKimlikArka.setText(R.string.kimlik_arka_button_text);
                        Toast.makeText(this, R.string.kimlik_arka_saved, Toast.LENGTH_SHORT).show();
                    }
                    checkAndEnableFormButton();
                } else {
                    Toast.makeText(this, R.string.error_saving_cropped_image, Toast.LENGTH_SHORT).show();
                }

                if (croppedBitmap != originalBitmap) {
                    croppedBitmap.recycle();
                }
            } else {
                Toast.makeText(this, R.string.crop_error_generic, Toast.LENGTH_SHORT).show();
            }

            originalBitmap.recycle();
        } else {
            Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
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
                Log.e(TAG, getString(R.string.crop_error_generic));
                return originalBitmap;
            }

            // Bitmap'in merkez kısmını kırp
            return Bitmap.createBitmap(originalBitmap, left, top, targetWidth, targetHeight);
        } catch (Exception e) {
            Log.e(TAG, getString(R.string.crop_error_generic) + " " + e.getMessage(), e);
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
                Log.e(TAG, getString(R.string.file_creation_error));
                return null;
            }
            photoFile = File.createTempFile(imageFileName, ".jpg", storageDir);

            try (FileOutputStream out = new FileOutputStream(photoFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                Log.d(TAG, getString(R.string.image_saved_successfully) + " " + photoFile.getAbsolutePath());
                return photoFile.getAbsolutePath();
            } catch (IOException e) {
                Log.e(TAG, getString(R.string.error_saving_image_file) + " " + e.getMessage(), e);
                return null;
            }
        } catch (IOException ex) {
            Log.e(TAG, getString(R.string.file_creation_error) + " " + ex.getMessage(), ex);
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
            Toast.makeText(this, R.string.form_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void enableCameraFeatures() {
        btnKimlikOn.setEnabled(true);
        btnKimlikArka.setEnabled(true);
        Log.d(TAG, getString(R.string.camera_permission_granted));
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
}