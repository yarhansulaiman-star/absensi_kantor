package com.example.absensi_kantor.ui.auth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.InputStream;
import java.util.*;

import retrofit2.*;

public class RegisterActivity extends AppCompatActivity {

    private static final int    REQ_FOTO = 101, PERM = 200;
    private static final String TAG      = "RegisterActivity";

    // ✅ VALIDASI: threshold ML Kit — dilonggarkan sedikit dari versi awal
    // agar tidak terlalu sering menolak foto yang sebenarnya masih wajar.
    private static final float EYE_OPEN_THRESHOLD = 0.35f;
    private static final float SUDUT_MAKS_Y       = 20f; // toleransi hadap kiri/kanan (yaw)
    private static final float SUDUT_MAKS_Z       = 25f; // toleransi kepala miring (roll)

    // ✅ VALIDASI TAMBAHAN: pencahayaan
    // Rata-rata luminance (0-255) dari sampling piksel. Di bawah ini dianggap terlalu gelap.
    private static final double LUMINANCE_MIN = 60.0;
    private static final int    BRIGHTNESS_SAMPLE_STEP = 15; // makin kecil = makin teliti tapi lebih lambat

    private EditText    etUsername, etPassword, etEmail, etJabatan, etDepartemen;
    private ImageView   imgFoto;
    private TextView    tvStatus;
    private ProgressBar progressBar;
    private Button      btnDaftar;

    private Bitmap bitmapFoto = null;
    private Uri    uriFoto;
    private boolean fotoValid = false;

    private FaceDetector faceDetector;

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
        imgFoto      = findViewById(R.id.imgFoto);
        tvStatus     = findViewById(R.id.tvStatus);
        progressBar  = findViewById(R.id.progressBar);
        btnDaftar    = findViewById(R.id.btnDaftar);

        // ✅ VALIDASI: FaceDetector untuk cek wajah tunggal, mata, & sudut kepala
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        faceDetector = FaceDetection.getClient(options);

        // Kunci tombol daftar sampai foto lolos validasi
        btnDaftar.setEnabled(false);

        findViewById(R.id.btnFoto).setOnClickListener(v -> ambilFoto());
        btnDaftar.setOnClickListener(v -> register());
    }

    // ── Ambil foto dari kamera ─────────────────────────────────────
    private void ambilFoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, PERM);
            return;
        }
        try {
            File file = File.createTempFile("foto_wajah", ".jpg",
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            uriFoto = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uriFoto);
            startActivityForResult(intent, REQ_FOTO);
        } catch (Exception e) {
            Toast.makeText(this, "Error kamera: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_FOTO || resultCode != RESULT_OK) return;

        try {
            InputStream is  = getContentResolver().openInputStream(uriFoto);
            Bitmap      raw = BitmapFactory.decodeStream(is);

            // ✅ TAMBAHAN: Koreksi rotasi dari EXIF SEBELUM diproses lebih lanjut.
            // Kamera bawaan (MediaStore.ACTION_IMAGE_CAPTURE) sering menyimpan
            // piksel foto tidak tegak, dengan info rotasi yang benar hanya ada
            // di metadata EXIF. Kalau ini dilewatkan, wajah yang sebenarnya lurus
            // bisa terbaca "miring" oleh ML Kit, atau bahkan gagal terdeteksi sama
            // sekali kalau ditambah sedikit kemiringan kepala asli.
            int rotasi = ImageUtils.bacaRotasiExif(this, uriFoto);
            raw = ImageUtils.rotateBitmap(raw, rotasi);

            Bitmap    fixed = ImageUtils.fixBitmap(raw, true); // resize 640px di sini

            imgFoto.setImageBitmap(fixed);
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText("🔍 Memvalidasi foto...");
            tvStatus.setTextColor(Color.parseColor("#757575"));
            fotoValid = false;
            btnDaftar.setEnabled(false);

            validasiFoto(fixed);

        } catch (Exception e) {
            Toast.makeText(this, "Gagal load foto", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ VALIDASI TAMBAHAN: cek rata-rata pencahayaan bitmap dengan sampling piksel.
    // Dijalankan SEBELUM ML Kit supaya kalau memang gelap, pesannya jelas menyebut
    // itu masalah cahaya — bukan disalahartikan sebagai "wajah tidak terdeteksi".
    private double hitungLuminance(Bitmap bmp) {
        long total = 0;
        int  count = 0;
        int  w = bmp.getWidth();
        int  h = bmp.getHeight();

        for (int x = 0; x < w; x += BRIGHTNESS_SAMPLE_STEP) {
            for (int y = 0; y < h; y += BRIGHTNESS_SAMPLE_STEP) {
                int pixel = bmp.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                // Rumus luminance standar (persepsi mata manusia thd RGB)
                total += Math.round(0.299 * r + 0.587 * g + 0.114 * b);
                count++;
            }
        }
        return count == 0 ? 0 : (double) total / count;
    }

    // ✅ VALIDASI: Cek pencahayaan → wajah tunggal → mata → sudut kepala.
    // Semua masalah yang terdeteksi dikumpulkan jadi satu pesan, jadi user
    // tidak perlu coba berkali-kali satu per satu.
    private void validasiFoto(Bitmap bitmap) {

        double luminance = hitungLuminance(bitmap);
        if (luminance < LUMINANCE_MIN) {
            tandaiGagal("❌ Pencahayaan terlalu redup. Cari tempat lebih terang lalu ambil ulang.");
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.isEmpty()) {
                        tandaiGagal("❌ Wajah tidak terdeteksi. Pastikan wajah menghadap kamera dengan jelas.");
                        return;
                    }
                    if (faces.size() > 1) {
                        tandaiGagal("❌ Terdeteksi lebih dari 1 wajah. Pastikan hanya Anda di foto.");
                        return;
                    }

                    Face face = faces.get(0);
                    List<String> masalah = new ArrayList<>();

                    Float leftEye  = face.getLeftEyeOpenProbability();
                    Float rightEye = face.getRightEyeOpenProbability();
                    if (leftEye != null && rightEye != null
                            && (leftEye < EYE_OPEN_THRESHOLD || rightEye < EYE_OPEN_THRESHOLD)) {
                        masalah.add("mata tertutup/tidak jelas");
                    }

                    float sudutY = face.getHeadEulerAngleY();
                    float sudutZ = face.getHeadEulerAngleZ();

                    if (Math.abs(sudutY) > SUDUT_MAKS_Y) {
                        masalah.add("wajah belum lurus menghadap depan");
                    }
                    if (Math.abs(sudutZ) > SUDUT_MAKS_Z) {
                        masalah.add("kepala terlalu miring");
                    }

                    if (!masalah.isEmpty()) {
                        tandaiGagal("❌ " + capitalize(String.join(", ", masalah)) + ". Coba ambil ulang.");
                        return;
                    }

                    // Lolos semua pengecekan
                    bitmapFoto = bitmap;
                    fotoValid  = true;
                    tvStatus.setVisibility(View.VISIBLE);
                    tvStatus.setText("✅ Foto valid");
                    tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                    btnDaftar.setEnabled(true);
                })
                .addOnFailureListener(e ->
                        tandaiGagal("❌ Gagal memproses foto: " + e.getMessage()));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void tandaiGagal(String pesan) {
        bitmapFoto = null;
        fotoValid  = false;
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(pesan);
        tvStatus.setTextColor(Color.parseColor("#C62828"));
        btnDaftar.setEnabled(false);
    }

    // ── Register ───────────────────────────────────────────────────
    private void register() {
        if (bitmapFoto == null || !fotoValid) {
            Toast.makeText(this, "Ambil foto yang valid dulu!", Toast.LENGTH_SHORT).show();
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
            String b64 = ImageUtils.bitmapToBase64(bitmapFoto);
            Log.d(TAG, "Base64 length: " + b64.length()); // pantau ukuran di logcat

            // ✅ Tetap kirim sebagai list "fotos" berisi 1 elemen,
            // supaya endpoint /register/multi & skema backend tidak perlu diubah
            List<String> fotos = new ArrayList<>();
            fotos.add(b64);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        faceDetector.close();
    }
}