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
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String[] REQUIRED_PERMISSIONS = { Manifest.permission.CAMERA };

    private Button btnFront, btnBack, btnToForm;
    private String frontPath, backPath;

    private final ActivityResultLauncher<Intent> frontLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleImage(result.getData().getStringExtra("image_path"), true);
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, R.string.camera_cancelled, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.error_camera_generic, Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Intent> backLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleImage(result.getData().getStringExtra("image_path"), false);
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, R.string.camera_cancelled, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.error_camera_generic, Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String[]> requestPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean granted = true;
                for (Boolean ok : result.values()) {
                    if (!ok) { granted = false; break; }
                }
                if (granted) initButtons();
                else {
                    Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnFront  = findViewById(R.id.btn_kimlik_on);
        btnBack   = findViewById(R.id.btn_kimlik_arka);
        btnToForm = findViewById(R.id.btn_forma_gec);

        btnFront.setEnabled(false);
        btnBack.setEnabled(false);
        btnToForm.setEnabled(false);

        if (allPermissionsGranted()) initButtons();
        else requestPermission.launch(REQUIRED_PERMISSIONS);
    }

    private void initButtons() {
        btnFront.setEnabled(true);
        btnBack.setEnabled(true);

        btnFront.setOnClickListener(v -> {
            Intent i = new Intent(this, IdCardCameraActivity.class);
            i.putExtra("is_front", true);
            frontLauncher.launch(i);
        });
        btnBack.setOnClickListener(v -> {
            Intent i = new Intent(this, IdCardCameraActivity.class);
            i.putExtra("is_front", false);
            backLauncher.launch(i);
        });
        btnToForm.setOnClickListener(v -> {
            Intent i = new Intent(this, FormActivity.class);
            // Yeni key'ler FormActivity ile uyumlu
            i.putExtra("front_image_path", frontPath);
            i.putExtra("back_image_path",  backPath);
            startActivity(i);
        });
    }

    private void handleImage(String path, boolean isFront) {
        Log.d(TAG, "Original path: " + path);
        Bitmap bm = BitmapFactory.decodeFile(path);
        if (bm == null) {
            Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap cropped = autoCrop(bm);
        String out = saveCropped(cropped, isFront);
        if (out == null) {
            Toast.makeText(this, R.string.error_saving_cropped_image, Toast.LENGTH_SHORT).show();
        } else {
            if (isFront) {
                frontPath = out;
                btnFront.setText(R.string.kimlik_on_button_text);
                Toast.makeText(this, R.string.kimlik_on_saved, Toast.LENGTH_SHORT).show();
            } else {
                backPath = out;
                btnBack.setText(R.string.kimlik_arka_button_text);
                Toast.makeText(this, R.string.kimlik_arka_saved, Toast.LENGTH_SHORT).show();
            }
        }
        if (cropped != bm) bm.recycle();
        btnToForm.setEnabled(frontPath != null && backPath != null);
    }

    private Bitmap autoCrop(Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        float ratio = 85.6f/54f;
        int tw, th;
        if ((float)w/h > ratio) {
            th = (int)(h*0.7f);
            tw = (int)(th*ratio);
        } else {
            tw = (int)(w*0.85f);
            th = (int)(tw/ratio);
        }
        int left = (w-tw)/2, top = (h-th)/2;
        if (left<0||top<0||tw<=0||th<=0||left+tw>w||top+th>h) return src;
        return Bitmap.createBitmap(src, left, top, tw, th);
    }

    private String saveCropped(Bitmap bmp, boolean front) {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String name = (front?"CROPPED_FRONT_":"CROPPED_BACK_") + ts + "_";
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (dir == null) return null;
        if (!dir.exists() && !dir.mkdirs()) return null;
        try {
            File f = File.createTempFile(name, ".jpg", dir);
            try (FileOutputStream out = new FileOutputStream(f)) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 95, out);
            }
            return f.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Save failed", e);
            return null;
        }
    }

    private boolean allPermissionsGranted() {
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }
}