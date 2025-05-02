package com.example.kimlikuygulamasi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.Toast;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final int REQUEST_KIMLIK_ON   = 1;
    private static final int REQUEST_KIMLIK_ARKA = 2;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{android.Manifest.permission.CAMERA};

    private Button btnKimlikOn;
    private Button btnKimlikArka;
    private Button btnFormaGec;

    private Uri kimlikOnUri;
    private Uri kimlikArkaUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Butonları tanımla
        Button btnCekimler = findViewById(R.id.btn_cekimler);
        btnKimlikOn = findViewById(R.id.btn_kimlik_on);
        btnKimlikArka = findViewById(R.id.btn_kimlik_arka);
        btnFormaGec = findViewById(R.id.btn_forma_gec);

        // İzinleri kontrol et
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // Kimlik ön yüz butonu
        btnKimlikOn.setOnClickListener(v -> dispatchTakePictureIntent("kimlik_on"));

        // Kimlik arka yüz butonu
        btnKimlikArka.setOnClickListener(v -> dispatchTakePictureIntent("kimlik_arka"));

        // Forma geçiş butonu
        btnFormaGec.setOnClickListener(v -> {
            if (kimlikOnUri != null && kimlikArkaUri != null) {
                // Form sayfasına geçiş ve fotoğraf URI'lerini gönder
                Intent intent = new Intent(MainActivity.this, FormActivity.class);
                intent.putExtra("kimlik_on_uri", kimlikOnUri.toString());
                intent.putExtra("kimlik_arka_uri", kimlikArkaUri.toString());
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Lütfen önce kimlik kartının her iki yüzünü çekin", Toast.LENGTH_SHORT).show();
            }
        });

        // Çekimler butonu - önceki çekimleri görmek için
        btnCekimler.setOnClickListener(v -> {
            // Önceki çekimlerin bulunduğu galeriye gitmek için kod eklenebilir
            Toast.makeText(MainActivity.this, "Çekimler özelliği geliştirme aşamasında", Toast.LENGTH_SHORT).show();
        });
    }

    private void dispatchTakePictureIntent(String imageType) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile(imageType);

            // Burayı değiştiriyoruz:
            Uri photoURI = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",  // aynı authorities
                    photoFile
            );

            // Kamera uygulamasına yazma izni ver
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

            // Doğru request code ile başlat
            int requestCode = imageType.equals("kimlik_on")
                    ? REQUEST_KIMLIK_ON
                    : REQUEST_KIMLIK_ARKA;
            startActivityForResult(takePictureIntent, requestCode);

            // URI’yi kaydet
            if (imageType.equals("kimlik_on")) kimlikOnUri = photoURI;
            else                         kimlikArkaUri = photoURI;
        }
    }

    private File createImageFile(String filePrefix) {
        // Dosya adında zaman damgası kullan
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis());
        String imageFileName = filePrefix + "_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        return new File(storageDir, imageFileName + ".jpg");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_KIMLIK_ON) {
                Toast.makeText(this, "Kimlik ön yüzü kaydedildi", Toast.LENGTH_SHORT).show();
                btnKimlikOn.setText("Kimlik Ön Yüzü (✓)");
            } else if (requestCode == REQUEST_KIMLIK_ARKA) {
                Toast.makeText(this, "Kimlik arka yüzü kaydedildi", Toast.LENGTH_SHORT).show();
                btnKimlikArka.setText("Kimlik Arka Yüzü (✓)");
            }
            if (kimlikOnUri != null && kimlikArkaUri != null) {
                btnFormaGec.setEnabled(true);
            }
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startCamera() {
        // Kamera başlatma kodu (bu kısım basitleştirilebilir)
        Toast.makeText(this, "Kamera izni verildi", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Kamera izni verilmedi.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}