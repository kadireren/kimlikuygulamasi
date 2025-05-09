package com.example.kimlikuygulamasi;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

public class FormActivity extends AppCompatActivity {

    private TextInputEditText etTcNo, etFullName, etBirthDate, etBirthPlace, etMotherName, etFatherName,
            etAddress, etPhone, etJob, etEmail;
    private Spinner spDocType;
    private String frontPath, backPath;
    private String selectedDocType = "Kimlik Kartı"; // Default value

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        ImageView ivFront = findViewById(R.id.iv_front);
        ImageView ivBack = findViewById(R.id.iv_back);

        spDocType = findViewById(R.id.sp_doc_type);
        etTcNo = findViewById(R.id.et_tc_no);
        etFullName = findViewById(R.id.et_full_name);
        etBirthDate = findViewById(R.id.et_birth_date);
        etBirthPlace = findViewById(R.id.et_birth_place);
        etMotherName = findViewById(R.id.et_mother_name);
        etFatherName = findViewById(R.id.et_father_name);
        etAddress = findViewById(R.id.et_address);
        etPhone = findViewById(R.id.et_phone);
        etJob = findViewById(R.id.et_job);
        etEmail = findViewById(R.id.et_email);

        Button btnYazdir = findViewById(R.id.btn_yazdir);

        // Spinner setup
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.document_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDocType.setAdapter(adapter);

        spDocType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDocType = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDocType = "Kimlik Kartı"; // Default value
            }
        });

        etBirthDate.addTextChangedListener(new DateTextWatcher());
        etPhone.addTextChangedListener(new PhoneNumberTextWatcher());

        frontPath = getIntent().getStringExtra("front_image_path");
        backPath = getIntent().getStringExtra("back_image_path");

        if (frontPath != null && !frontPath.isEmpty() && new File(frontPath).exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(frontPath);
            ivFront.setImageBitmap(bitmap);
        }

        if (backPath != null && !backPath.isEmpty() && new File(backPath).exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(backPath);
            ivBack.setImageBitmap(bitmap);
        }

        btnYazdir.setOnClickListener(this::onPrintClicked);
    }

    private void onPrintClicked(View view) {
        Intent intent = new Intent(FormActivity.this, PdfActivity.class);
        intent.putExtra("kimlik_on_uri", frontPath != null ? frontPath : "");
        intent.putExtra("kimlik_arka_uri", backPath != null ? backPath : "");
        intent.putExtra("ad_soyad", etFullName.getText() != null ? etFullName.getText().toString() : "");
        intent.putExtra("tc_no", etTcNo.getText() != null ? etTcNo.getText().toString() : "");
        intent.putExtra("dogum_tarihi", etBirthDate.getText() != null ? etBirthDate.getText().toString() : "");
        intent.putExtra("dogum_yeri", etBirthPlace.getText() != null ? etBirthPlace.getText().toString() : "");
        intent.putExtra("anne_adi", etMotherName.getText() != null ? etMotherName.getText().toString() : "");
        intent.putExtra("baba_adi", etFatherName.getText() != null ? etFatherName.getText().toString() : "");
        intent.putExtra("adres", etAddress.getText() != null ? etAddress.getText().toString() : "");
        intent.putExtra("telefon", etPhone.getText() != null ? etPhone.getText().toString() : "");
        intent.putExtra("meslek", etJob.getText() != null ? etJob.getText().toString() : "");
        intent.putExtra("email", etEmail.getText() != null ? etEmail.getText().toString() : "");
        intent.putExtra("belge_turu", selectedDocType);
        startActivity(intent);
    }

    private class DateTextWatcher implements TextWatcher {
        private boolean isFormatting;
        private String lastFormatted;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (isFormatting) {
                return;
            }

            isFormatting = true;

            String digits = s.toString().replaceAll("[^0-9]", "");

            if (digits.length() > 8) {
                digits = digits.substring(0, 8);
            }

            StringBuilder formatted = new StringBuilder();

            if (digits.length() > 0) {
                formatted.append(digits.substring(0, Math.min(2, digits.length())));

                if (digits.length() > 2) {
                    formatted.append(".").append(digits.substring(2, Math.min(4, digits.length())));

                    if (digits.length() > 4) {
                        formatted.append(".").append(digits.substring(4, Math.min(8, digits.length())));
                    }
                }
            }

            String result = formatted.toString();
            if (!result.equals(lastFormatted)) {
                s.replace(0, s.length(), result);
                lastFormatted = result;
            }

            isFormatting = false;
        }
    }

    private class PhoneNumberTextWatcher implements TextWatcher {
        private boolean isFormatting;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (isFormatting) {
                return;
            }

            isFormatting = true;

            String digits = s.toString().replaceAll("[^0-9]", "");

            if (digits.length() > 0 && !digits.startsWith("0")) {
                digits = "0" + digits;
            }

            if (digits.length() > 1 && digits.charAt(1) != '5') {
                digits = "05" + (digits.length() > 2 ? digits.substring(2) : "");
            }

            if (digits.length() > 11) {
                digits = digits.substring(0, 11);
            }

            StringBuilder formatted = new StringBuilder();

            if (digits.length() > 0) {
                formatted.append(digits.charAt(0));

                if (digits.length() > 1) {
                    formatted.append("(").append(digits.charAt(1));

                    if (digits.length() > 2) {
                        formatted.append(digits.substring(2, Math.min(4, digits.length())));

                        if (digits.length() > 4) {
                            formatted.append(") ").append(digits.substring(4, Math.min(7, digits.length())));

                            if (digits.length() > 7) {
                                formatted.append(" ").append(digits.substring(7, Math.min(9, digits.length())));

                                if (digits.length() > 9) {
                                    formatted.append(" ").append(digits.substring(9, Math.min(11, digits.length())));
                                }
                            }
                        }
                    }
                }
            }

            s.replace(0, s.length(), formatted);
            isFormatting = false;
        }
    }
}