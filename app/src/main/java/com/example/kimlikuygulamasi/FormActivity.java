package com.example.kimlikuygulamasi;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class FormActivity extends AppCompatActivity {

    private EditText edtAdSoyad;
    private EditText edtTcNo;
    private EditText edtDogumTarihi;
    private EditText edtAdres;
    private EditText edtTelefon;

    private String kimlikOnUriString;
    private String kimlikArkaUriString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        // UI elemanlarını tanımla
        ImageView imgKimlikOn = findViewById(R.id.img_kimlik_on);
        ImageView imgKimlikArka = findViewById(R.id.img_kimlik_arka);
        edtAdSoyad = findViewById(R.id.edt_ad_soyad);
        edtTcNo = findViewById(R.id.edt_tc_no);
        edtDogumTarihi = findViewById(R.id.edt_dogum_tarihi);
        edtAdres = findViewById(R.id.edt_adres);
        edtTelefon = findViewById(R.id.edt_telefon);
        Button btnYazdir = findViewById(R.id.btn_yazdir);

        // Ana aktiviteden gelen verileri al
        Intent intent = getIntent();
        if (intent != null) {
            kimlikOnUriString = intent.getStringExtra("kimlik_on_uri");
            kimlikArkaUriString = intent.getStringExtra("kimlik_arka_uri");

            // Kimlik fotoğraflarını göster
            if (kimlikOnUriString != null && !kimlikOnUriString.isEmpty()) {
                imgKimlikOn.setImageURI(Uri.parse(kimlikOnUriString));
            }

            if (kimlikArkaUriString != null && !kimlikArkaUriString.isEmpty()) {
                imgKimlikArka.setImageURI(Uri.parse(kimlikArkaUriString));
            }
        }

        // Yazdır butonu tıklama olayı
        btnYazdir.setOnClickListener(v -> {
            if (validateForm()) {
                generatePdf();
            } else {
                Toast.makeText(FormActivity.this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateForm() {
        // Form alanlarının boş olup olmadığını kontrol et
        return !edtAdSoyad.getText().toString().trim().isEmpty() &&
                !edtTcNo.getText().toString().trim().isEmpty() &&
                !edtDogumTarihi.getText().toString().trim().isEmpty() &&
                !edtAdres.getText().toString().trim().isEmpty() &&
                !edtTelefon.getText().toString().trim().isEmpty();
    }

    private void generatePdf() {
        // PDF oluşturma ve yazdırma işlemini PdfActivity'ye yönlendir
        Intent intent = new Intent(FormActivity.this, PdfActivity.class);

        // Form bilgilerini gönder
        intent.putExtra("ad_soyad", edtAdSoyad.getText().toString());
        intent.putExtra("tc_no", edtTcNo.getText().toString());
        intent.putExtra("dogum_tarihi", edtDogumTarihi.getText().toString());
        intent.putExtra("adres", edtAdres.getText().toString());
        intent.putExtra("telefon", edtTelefon.getText().toString());

        // Kimlik fotoğraflarının URI'lerini gönder
        intent.putExtra("kimlik_on_uri", kimlikOnUriString);
        intent.putExtra("kimlik_arka_uri", kimlikArkaUriString);

        startActivity(intent);
    }
}