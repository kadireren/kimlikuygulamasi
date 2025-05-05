package com.example.kimlikuygulamasi;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormActivity extends AppCompatActivity {
    private TextInputEditText etFullName, etTcNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        ImageView ivFront = findViewById(R.id.iv_front);
        ImageView ivBack = findViewById(R.id.iv_back);
        etFullName = findViewById(R.id.et_full_name);
        etTcNo     = findViewById(R.id.et_tc_no);
        Button btnYazdir = findViewById(R.id.btn_yazdir);

        String frontPath = getIntent().getStringExtra("front_image_path");
        String backPath  = getIntent().getStringExtra("back_image_path");

        if (frontPath != null && new File(frontPath).exists()) {
            ivFront.setImageBitmap(BitmapFactory.decodeFile(frontPath));
        }
        if (backPath != null && new File(backPath).exists()) {
            ivBack.setImageBitmap(BitmapFactory.decodeFile(backPath));
        }

        btnYazdir.setOnClickListener(this::createPdf);
    }

    private void createPdf(View view) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(12 * getResources().getDisplayMetrics().density);

        String frontPath = getIntent().getStringExtra("front_image_path");
        String backPath  = getIntent().getStringExtra("back_image_path");

        Bitmap frontBmp = null;
        Bitmap backBmp  = null;
        if (frontPath != null && new File(frontPath).exists()) {
            frontBmp = BitmapFactory.decodeFile(frontPath);
        }
        if (backPath != null && new File(backPath).exists()) {
            backBmp = BitmapFactory.decodeFile(backPath);
        }

        if (frontBmp != null && !frontBmp.isRecycled()) {
            page.getCanvas().drawBitmap(frontBmp, 50, 50, null);
        }
        if (backBmp != null && !backBmp.isRecycled()) {
            page.getCanvas().drawBitmap(backBmp, 300, 50, null);
        }

        String fullName = etFullName.getText() != null ? etFullName.getText().toString() : "";
        String tcNo = etTcNo.getText() != null ? etTcNo.getText().toString() : "";
        page.getCanvas().drawText(
                "Ad Soyad: " + fullName,
                50, 300, paint
        );
        page.getCanvas().drawText(
                "T.C. Kimlik No: " + tcNo,
                50, 330, paint
        );

        document.finishPage(page);

        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File pdfDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "pdfs");
        if (!pdfDir.exists() && !pdfDir.mkdirs()) {
            Toast.makeText(this, "PDF dizini oluşturulamadı.", Toast.LENGTH_LONG).show();
            return;
        }
        File pdfFile = new File(pdfDir, "IDFORM_" + ts + ".pdf");

        try (FileOutputStream out = new FileOutputStream(pdfFile)) {
            document.writeTo(out);
            Toast.makeText(this,
                    "PDF kaydedildi: " + pdfFile.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this,
                    "PDF oluşturma hatası: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        } finally {
            document.close();
        }
    }
}