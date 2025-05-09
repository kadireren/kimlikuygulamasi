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
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PdfActivity extends AppCompatActivity {

    private static final int REQUEST_WRITE_STORAGE = 112;
    private static final String TAG = "PdfActivity";

    private String adSoyad, tcNo, dogumTarihi, dogumYeri, anneAdi, babaAdi, adres, telefon, meslek, email, belgeTuru;
    private String kimlikOnUriString, kimlikArkaUriString;
    private File pdfDosyasi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        Button btnYazdir = findViewById(R.id.btn_pdf_yazdir);
        Button btnPdfKaydet = findViewById(R.id.btn_pdf_kaydet);
        Button btnAnasayfayaDon = findViewById(R.id.btn_anasayfaya_don);

        Intent intent = getIntent();
        if (intent != null) {
            adSoyad = intent.getStringExtra("ad_soyad");
            tcNo = intent.getStringExtra("tc_no");
            dogumTarihi = intent.getStringExtra("dogum_tarihi");
            dogumYeri = intent.getStringExtra("dogum_yeri");
            anneAdi = intent.getStringExtra("anne_adi");
            babaAdi = intent.getStringExtra("baba_adi");
            adres = intent.getStringExtra("adres");
            telefon = intent.getStringExtra("telefon");
            meslek = intent.getStringExtra("meslek");
            email = intent.getStringExtra("email");
            belgeTuru = intent.getStringExtra("belge_turu");
            kimlikOnUriString = intent.getStringExtra("kimlik_on_uri");
            kimlikArkaUriString = intent.getStringExtra("kimlik_arka_uri");
        }

        checkPermissions();
        createPdf();

        btnYazdir.setOnClickListener(v -> {
            if (pdfDosyasi != null && pdfDosyasi.exists()) {
                printPdf();
            } else {
                Toast.makeText(this, "PDF dosyası bulunamadı", Toast.LENGTH_SHORT).show();
            }
        });

        btnPdfKaydet.setOnClickListener(v -> {
            if (pdfDosyasi != null && pdfDosyasi.exists()) {
                openPdf();
            } else {
                Toast.makeText(this, "PDF dosyası bulunamadı", Toast.LENGTH_SHORT).show();
            }
        });

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
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        paint.setTextSize(16);
        canvas.drawText("KİMLİK KAYIT FORMU", 200, 50, paint);

        if (kimlikOnUriString != null && kimlikArkaUriString != null) {
            try {
                Bitmap kimlikOnBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(Uri.parse(kimlikOnUriString)));
                Bitmap kimlikArkaBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(Uri.parse(kimlikArkaUriString)));

                if (kimlikOnBitmap != null) {
                    canvas.drawBitmap(Bitmap.createScaledBitmap(kimlikOnBitmap, 200, 120, false), 50, 80, paint);
                }
                if (kimlikArkaBitmap != null) {
                    canvas.drawBitmap(Bitmap.createScaledBitmap(kimlikArkaBitmap, 200, 120, false), 300, 80, paint);
                }
            } catch (IOException e) {
                Log.e(TAG, "Kimlik fotoğrafları yüklenirken hata oluştu: " + e.getMessage(), e);
            }
        }

        paint.setTextSize(14);
        int y = 230;

        canvas.drawText("Kimlik Belgesi Türü: " + belgeTuru, 50, y, paint); y += 20;
        canvas.drawText("T.C. Kimlik No: " + tcNo, 50, y, paint); y += 20;
        canvas.drawText("Ad Soyad: " + adSoyad, 50, y, paint); y += 20;
        canvas.drawText("Doğum Tarihi: " + dogumTarihi, 50, y, paint); y += 20;
        canvas.drawText("Doğum Yeri: " + dogumYeri, 50, y, paint); y += 20;
        canvas.drawText("Anne Adı: " + anneAdi, 50, y, paint); y += 20;
        canvas.drawText("Baba Adı: " + babaAdi, 50, y, paint); y += 20;
        canvas.drawText("Telefon: " + telefon, 50, y, paint); y += 20;
        canvas.drawText("Meslek: " + meslek, 50, y, paint); y += 20;
        canvas.drawText("Email: " + email, 50, y, paint); y += 20;

        canvas.drawText("Adres:", 50, y, paint); y += 20;
        for (String line : formatText(adres)) {
            canvas.drawText(line, 70, y, paint);
            y += 20;
        }

        String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        canvas.drawText("Tarih: " + currentDate, 50, y + 40, paint);
        canvas.drawRect(350, y + 10, 545, y + 70, paint);
        paint.setColor(0xFFFFFFFF);
        canvas.drawText("İmza", 430, y + 45, paint);

        document.finishPage(page);

        File pdfFolder = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "KimlikUygulamasi");
        if (!pdfFolder.exists()) {
            boolean dirCreated = pdfFolder.mkdirs();
            if (!dirCreated) {
                Log.e(TAG, "PDF klasörü oluşturulamadı");
                return;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis());
        pdfDosyasi = new File(pdfFolder, "Kimlik_" + timeStamp + ".pdf");

        try (FileOutputStream outputStream = new FileOutputStream(pdfDosyasi)) {
            document.writeTo(outputStream);
            Log.d(TAG, "PDF başarıyla kaydedildi: " + pdfDosyasi.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "PDF kaydedilirken hata oluştu: " + e.getMessage(), e);
            Toast.makeText(this, "PDF kaydedilirken hata oluştu", Toast.LENGTH_SHORT).show();
        }

        document.close();
    }

    private String[] formatText(String text) {
        if (text == null) return new String[]{""};
        if (text.length() <= 70) return new String[]{text};

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
            PrintDocumentAdapter printAdapter = new PdfDocumentAdapter(pdfDosyasi.getAbsolutePath());
            printManager.print("Kimlik PDF", printAdapter, null);
        } catch (Exception e) {
            Log.e(TAG, "PDF yazdırma hatası: " + e.getMessage(), e);
            Toast.makeText(this, "PDF yazdırma hatası", Toast.LENGTH_SHORT).show();
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
            Log.e(TAG, "PDF gösterici uygulaması bulunamadı: " + e.getMessage(), e);
            Toast.makeText(this, "PDF gösterici uygulaması bulunamadı", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createPdf();
            } else {
                Toast.makeText(this, "PDF oluşturmak için depolama izni gerekli", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
