package com.example.absensi_kantor.ui.auth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.absensi_kantor.R;
import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.model.auth.RegisterResponse;
import com.example.absensi_kantor.utils.ImageUtils;

import java.io.File;
import java.io.InputStream;
import java.util.*;

import retrofit2.*;

public class RegisterActivity extends AppCompatActivity {

    private static final int    REQ1 = 101, REQ2 = 102, REQ3 = 103, PERM = 200;
    private static final String TAG  = "RegisterActivity";

    private EditText    etUsername, etPassword, etEmail, etJabatan, etDepartemen;
    private ImageView   img1, img2, img3;
    private ProgressBar progressBar;
    private Button      btnDaftar;

    private final Bitmap[] bitmaps = new Bitmap[3];
    private final Uri[]    uris    = new Uri[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ApiClient.init(this);

        etUsername   = findViewById(R.id.etUsername);
        etPassword   = findViewById(R.id.etPassword);
        etEmail      = findViewById(R.id.etEmail);
        etJabatan    = findViewById(R.id.etJabatan);
        etDepartemen = findViewById(R.id.etDepartemen);
        img1         = findViewById(R.id.imgFoto1);
        img2         = findViewById(R.id.imgFoto2);
        img3         = findViewById(R.id.imgFoto3);
        progressBar  = findViewById(R.id.progressBar);
        btnDaftar    = findViewById(R.id.btnDaftar);

        findViewById(R.id.btnFoto1).setOnClickListener(v -> ambilFoto(0, REQ1));
        findViewById(R.id.btnFoto2).setOnClickListener(v -> ambilFoto(1, REQ2));
        findViewById(R.id.btnFoto3).setOnClickListener(v -> ambilFoto(2, REQ3));
        btnDaftar.setOnClickListener(v -> register());
    }

    // ── Ambil foto dari kamera ─────────────────────────────────────
    private void ambilFoto(int index, int code) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, PERM);
            return;
        }
        try {
            File file = File.createTempFile("foto_" + index, ".jpg",
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            uris[index] = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uris[index]);
            startActivityForResult(intent, code);
        } catch (Exception e) {
            Toast.makeText(this, "Error kamera: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        int index = requestCode == REQ1 ? 0 : requestCode == REQ2 ? 1 : 2;
        try {
            InputStream is  = getContentResolver().openInputStream(uris[index]);
            Bitmap      raw = BitmapFactory.decodeStream(is);
            Bitmap    fixed = ImageUtils.fixBitmap(raw, true); // resize 640px di sini
            bitmaps[index]  = fixed;
            if (index == 0) img1.setImageBitmap(fixed);
            if (index == 1) img2.setImageBitmap(fixed);
            if (index == 2) img3.setImageBitmap(fixed);
        } catch (Exception e) {
            Toast.makeText(this, "Gagal load foto", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Register ───────────────────────────────────────────────────
    private void register() {
        if (bitmaps[0] == null || bitmaps[1] == null || bitmaps[2] == null) {
            Toast.makeText(this, "Ambil 3 foto dulu!", Toast.LENGTH_SHORT).show();
            return;
        }

        String username   = etUsername.getText().toString().trim();
        String password   = etPassword.getText().toString().trim();
        String email      = etEmail.getText().toString().trim();
        String jabatan    = etJabatan.getText().toString().trim();
        String departemen = etDepartemen.getText().toString().trim();

        if (username.isEmpty())   { etUsername.setError("Wajib diisi"); etUsername.requestFocus(); return; }
        if (password.isEmpty())   { etPassword.setError("Wajib diisi"); etPassword.requestFocus(); return; }
        if (email.isEmpty())      { etEmail.setError("Wajib diisi");    etEmail.requestFocus();    return; }
        if (jabatan.isEmpty())    { etJabatan.setError("Wajib diisi");  etJabatan.requestFocus();  return; }

        progressBar.setVisibility(View.VISIBLE);
        btnDaftar.setEnabled(false);

        // Encode base64 di background thread agar UI tidak freeze saat kompresi
        new Thread(() -> {
            List<String> fotos = new ArrayList<>();
            for (Bitmap b : bitmaps) {
                String b64 = ImageUtils.bitmapToBase64(b);
                Log.d(TAG, "Base64 length: " + b64.length()); // pantau ukuran di logcat
                fotos.add(b64);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("username",   username);
            body.put("password",   password);
            body.put("email",      email);
            body.put("jabatan",    jabatan);
            body.put("departemen", departemen.isEmpty() ? "IT" : departemen);
            body.put("fotos",      fotos);

            runOnUiThread(() ->
                    // ✅ Pakai getServiceRegister() — timeout 120 detik
                    ApiClient.getServiceRegister()
                            .registerMulti(body)
                            .enqueue(new Callback<RegisterResponse>() {

                                @Override
                                public void onResponse(Call<RegisterResponse> call,
                                                       Response<RegisterResponse> response) {
                                    progressBar.setVisibility(View.GONE);
                                    btnDaftar.setEnabled(true);

                                    if (response.isSuccessful() && response.body() != null) {
                                        Toast.makeText(RegisterActivity.this,
                                                response.body().pesan, Toast.LENGTH_LONG).show();
                                        if (response.body().sukses) finish();
                                    } else {
                                        Toast.makeText(RegisterActivity.this,
                                                "Registrasi gagal, coba lagi", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<RegisterResponse> call, Throwable t) {
                                    progressBar.setVisibility(View.GONE);
                                    btnDaftar.setEnabled(true);
                                    Log.e(TAG, "onFailure: " + t.getMessage(), t);
                                    Toast.makeText(RegisterActivity.this,
                                            "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            })
            );
        }).start();
    }
}