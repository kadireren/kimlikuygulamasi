package com.example.kimlikuygulamasi;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

public class FormActivity extends AppCompatActivity {

    private static final String TAG = "FormActivity"; // Loglama için TAG eklendi

    private EditText edtAdSoyad;
    private EditText edtTcNo;
    private EditText edtDogumTarihi;
    private EditText edtAdres;
    private EditText edtTelefon;

    private String kimlikOnPath; // Değişken adı düzeltildi
    private String kimlikArkaPath; // Değişken adı düzeltildi

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        // UI elemanlarını tanımla
        // ImageView'ları sınıf seviyesinde tanımla
        ImageView imgKimlikOn = findViewById(R.id.img_kimlik_on); // XML ID'nizle eşleştiğinden emin olun
        // ImageView'ları sınıf seviyesinde tanımla
        ImageView imgKimlikArka = findViewById(R.id.img_kimlik_arka); // XML ID'nizle eşleştiğinden emin olun
        edtAdSoyad = findViewById(R.id.edt_ad_soyad);
        edtTcNo = findViewById(R.id.edt_tc_no);
        edtDogumTarihi = findViewById(R.id.edt_dogum_tarihi);
        edtAdres = findViewById(R.id.edt_adres);
        edtTelefon = findViewById(R.id.edt_telefon);
        Button btnYazdir = findViewById(R.id.btn_yazdir);

        // Ana aktiviteden gelen verileri al
        Intent intent = getIntent();
        if (intent != null) {
            // Doğru anahtarları kullanarak dosya yollarını al
            kimlikOnPath = intent.getStringExtra("kimlik_on_path");
            kimlikArkaPath = intent.getStringExtra("kimlik_arka_path");

            Log.d(TAG, "Alınan Ön Kimlik Yolu: " + kimlikOnPath);
            Log.d(TAG, "Alınan Arka Kimlik Yolu: " + kimlikArkaPath);

            // Kimlik fotoğraflarını göster
            loadImageDirect(kimlikOnPath, imgKimlikOn);
            loadImageDirect(kimlikArkaPath, imgKimlikArka);
        } else {
            Log.e(TAG, "Intent null geldi.");
            Toast.makeText(this, "Veri alınamadı.", Toast.LENGTH_SHORT).show(); // Kullanıcıya bilgi ver
        }

        // Yazdır butonu tıklama olayı
        btnYazdir.setOnClickListener(v -> {
            if (validateForm()) {
                generatePdf();
            } else {
                // strings.xml kullanmak daha iyi bir pratiktir
                Toast.makeText(FormActivity.this, R.string.form_validation_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Resmi ImageView'a yükleyen metod
    private void loadImageDirect(String imagePath, ImageView targetView) {
        if (imagePath == null || imagePath.isEmpty()) {
            Log.e(TAG, "Resim yolu null veya boş.");
            // Opsiyonel: Placeholder veya hata mesajı gösterilebilir
            targetView.setImageResource(android.R.drawable.ic_menu_gallery); // Örnek placeholder
            return;
        }

        try {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Log.d(TAG, "Resim dosyası mevcut: " + imagePath);

                // Bitmap'i yükle
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

                if (bitmap != null) {
                    // ImageView'a bitmap'i ayarla
                    targetView.setImageBitmap(bitmap);
                    Log.d(TAG, "Resim başarıyla yüklendi: " + imagePath);
                } else {
                    Log.e(TAG, "Bitmap yüklenemedi: " + imagePath);
                    targetView.setImageResource(android.R.drawable.stat_notify_error); // Hata ikonu
                }
            } else {
                Log.e(TAG, "Resim dosyası bulunamadı: " + imagePath);
                targetView.setImageResource(android.R.drawable.stat_notify_error); // Hata ikonu
            }
        } catch (Exception e) {
            Log.e(TAG, "Resim yükleme hatası: " + imagePath, e);
            targetView.setImageResource(android.R.drawable.stat_notify_error); // Hata ikonu
        }
    }

    // Form alanlarının geçerliliğini kontrol eden metod
    private boolean validateForm() {
        // Form alanlarının boş olup olmadığını kontrol et
        return !edtAdSoyad.getText().toString().trim().isEmpty() &&
                !edtTcNo.getText().toString().trim().isEmpty() &&
                !edtDogumTarihi.getText().toString().trim().isEmpty() &&
                !edtAdres.getText().toString().trim().isEmpty() &&
                !edtTelefon.getText().toString().trim().isEmpty();
    }

    // PDF oluşturma aktivitesine yönlendiren metod
    private void generatePdf() {
        Intent pdfIntent = new Intent(FormActivity.this, PdfActivity.class);

        // Form bilgilerini gönder
        pdfIntent.putExtra("ad_soyad", edtAdSoyad.getText().toString());
        pdfIntent.putExtra("tc_no", edtTcNo.getText().toString());
        pdfIntent.putExtra("dogum_tarihi", edtDogumTarihi.getText().toString());
        pdfIntent.putExtra("adres", edtAdres.getText().toString());
        pdfIntent.putExtra("telefon", edtTelefon.getText().toString());

        // Kimlik fotoğraflarının yollarını doğru anahtarlarla gönder
        pdfIntent.putExtra("kimlik_on_path", kimlikOnPath);
        pdfIntent.putExtra("kimlik_arka_path", kimlikArkaPath);

        startActivity(pdfIntent);
    }
}