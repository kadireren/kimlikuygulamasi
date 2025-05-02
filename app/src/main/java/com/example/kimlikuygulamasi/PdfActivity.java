package com.example.kimlikuygulamasi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PdfActivity extends AppCompatActivity {

    private static final int REQUEST_WRITE_STORAGE = 112;

    private String adSoyad, tcNo, dogumTarihi, adres, telefon;
    private String kimlikOnUriString, kimlikArkaUriString;
    private File pdfDosyasi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        // UI elemanlarını tanımla
        Button btnYazdir = findViewById(R.id.btn_pdf_yazdir);
        Button btnPdfKaydet = findViewById(R.id.btn_pdf_kaydet);
        Button btnAnasayfayaDon = findViewById(R.id.btn_anasayfaya_don);

        // FormActivity'den gelen verileri al
        Intent intent = getIntent();
        if (intent != null) {
            adSoyad = intent.getStringExtra("ad_soyad");
            tcNo = intent.getStringExtra("tc_no");
            dogumTarihi = intent.getStringExtra("dogum_tarihi");
            adres = intent.getStringExtra("adres");
            telefon = intent.getStringExtra("telefon");
            kimlikOnUriString = intent.getStringExtra("kimlik_on_uri");
            kimlikArkaUriString = intent.getStringExtra("kimlik_arka_uri");
        }

        // İzinleri kontrol et
        checkPermissions();

        // PDF oluştur
        createPdf();

        // Yazdır butonu tıklama olayı
        btnYazdir.setOnClickListener(v -> {
            if (pdfDosyasi != null && pdfDosyasi.exists()) {
                printPdf();
            } else {
                Toast.makeText(PdfActivity.this, "PDF dosyası bulunamadı, lütfen önce kaydedin", Toast.LENGTH_SHORT).show();
            }
        });

        // PDF Kaydet butonu tıklama olayı
        btnPdfKaydet.setOnClickListener(v -> {
            if (pdfDosyasi != null && pdfDosyasi.exists()) {
                Toast.makeText(PdfActivity.this, "PDF dosyası zaten kaydedildi: " + pdfDosyasi.getAbsolutePath(), Toast.LENGTH_LONG).show();
                openPdf();
            } else {
                createPdf();
                Toast.makeText(PdfActivity.this, "PDF oluşturuldu ve kaydedildi", Toast.LENGTH_SHORT).show();
            }
        });

        // Ana sayfaya dön butonu
        btnAnasayfayaDon.setOnClickListener(v -> {
            Intent intent1 = new Intent(PdfActivity.this, MainActivity.class);
            intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent1);
        });
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
    }

    private void createPdf() {
        // PDF dokümantasyonu oluştur
        PdfDocument document = new PdfDocument();

        // A4 sayfa boyutu
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        // Başlık
        paint.setTextSize(16);
        canvas.drawText("KİMLİK KAYIT FORMU", 250, 50, paint);

        // Kimlik fotoğrafları
        if (kimlikOnUriString != null && kimlikArkaUriString != null) {
            try {
                Bitmap kimlikOn = BitmapFactory.decodeStream(getContentResolver().openInputStream(Uri.parse(kimlikOnUriString)));
                Bitmap kimlikArka = BitmapFactory.decodeStream(getContentResolver().openInputStream(Uri.parse(kimlikArkaUriString)));

                // Resimlerin boyutunu ayarla
                kimlikOn = Bitmap.createScaledBitmap(kimlikOn, 250, 150, false);
                kimlikArka = Bitmap.createScaledBitmap(kimlikArka, 250, 150, false);

                // Resimleri çiz
                canvas.drawBitmap(kimlikOn, 50, 100, null);
                canvas.drawBitmap(kimlikArka, 320, 100, null);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Kişisel bilgiler
        paint.setTextSize(14);
        int y = 300;

        canvas.drawText("Ad Soyad: " + adSoyad, 50, y, paint);
        y += 30;

        canvas.drawText("TC Kimlik No: " + tcNo, 50, y, paint);
        y += 30;

        canvas.drawText("Doğum Tarihi: " + dogumTarihi, 50, y, paint);
        y += 30;

        canvas.drawText("Telefon: " + telefon, 50, y, paint);
        y += 30;

        // Adres için satır satır yazdırma
        canvas.drawText("Adres:", 50, y, paint);
        y += 20;

        // Adres metnini 50 karakterden sonra böl
        String[] adresLines = formatText(adres);
        for (String line : adresLines) {
            canvas.drawText(line, 70, y, paint);
            y += 20;
        }

        // Tarih ekle
        String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        canvas.drawText("Tarih: " + currentDate, 50, y + 40, paint);

        document.finishPage(page);

        // PDF dosyasını kaydet
        File pdfFolder = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "KimlikUygulamasi");
        if (!pdfFolder.exists()) {
            pdfFolder.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis());
        pdfDosyasi = new File(pdfFolder, "Kimlik_" + timeStamp + ".pdf");

        try {
            document.writeTo(new FileOutputStream(pdfDosyasi));
            Toast.makeText(this, "PDF oluşturuldu", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "PDF oluşturulurken hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        document.close();
    }

    private String[] formatText(String text) {
        if (text.length() <= 70) {
            return new String[] { text };
        }

        int numLines = (int) Math.ceil((double) text.length() / 70);
        String[] lines = new String[numLines];

        for (int i = 0; i < numLines; i++) {
            int start = i * 70;
            int end = Math.min(start + 70, text.length());
            lines[i] = text.substring(start, end);
        }

        return lines;
    }

    private void printPdf() {
        PrintManager printManager = (PrintManager) getSystemService(PRINT_SERVICE);

        try {
            FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", pdfDosyasi);

            PrintDocumentAdapter printAdapter = new PdfDocumentAdapter(this, pdfDosyasi.getAbsolutePath());

            String jobName = getString(R.string.app_name) + " Kimlik Kayıt";

            printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());

        } catch (Exception e) {
            Toast.makeText(this, "Yazdırma hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openPdf() {
        Uri pdfUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", pdfDosyasi);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(pdfUri, "application/pdf");
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "PDF gösterici uygulaması bulunamadı", Toast.LENGTH_SHORT).show();
        }
    }
}