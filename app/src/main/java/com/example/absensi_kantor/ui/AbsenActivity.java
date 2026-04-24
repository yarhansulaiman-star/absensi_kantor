package com.example.absensi_kantor.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.api.SessionManager;  // ✅ package benar
import com.example.absensi_kantor.databinding.ActivityAbsenBinding;
import com.example.absensi_kantor.model.AbsenResponse;
import com.example.absensi_kantor.utils.ImageUtils;
import com.google.android.gms.location.*;
import com.google.common.util.concurrent.ListenableFuture;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AbsenActivity extends AppCompatActivity {

    private static final String TAG              = "AbsenActivity";
    private static final int    CAMERA_PERMISSION   = 100;
    private static final int    LOCATION_PERMISSION = 101;

    private ActivityAbsenBinding binding;
    private SessionManager       session;
    private ImageCapture         imageCapture;
    private ExecutorService      executor;
    private boolean              pakaiKameraDepan = false;

    private FusedLocationProviderClient fusedLocation;
    private Double currentLat = null;
    private Double currentLon = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding  = ActivityAbsenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ✅ Init ApiClient agar token bisa diinject otomatis
        ApiClient.init(this);

        session  = new SessionManager(this);
        executor = Executors.newSingleThreadExecutor();

        fusedLocation = LocationServices.getFusedLocationProviderClient(this);

        Log.d(TAG, "Token: " + session.getToken());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            nyalakanKamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            ambilLokasi();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION);
        }

        binding.tombolAbsen.setOnClickListener(v -> ambilFotoDanAbsen());
        binding.tombolKembali.setOnClickListener(v -> finish());
    }

    // ── Lokasi GPS ───────────────────────────────────────────────────────────

    private void ambilLokasi() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        binding.labelLokasi.setText("📍 Mendapatkan lokasi...");

        fusedLocation.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null && currentLat == null) {
                currentLat = loc.getLatitude();
                currentLon = loc.getLongitude();
                binding.labelLokasi.setText(
                        "📍 " + String.format("%.5f, %.5f", currentLat, currentLon));
                Log.d(TAG, "Lokasi (last known): " + currentLat + ", " + currentLon);
            }
        });

        LocationRequest req = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMaxUpdates(1)
                .build();

        fusedLocation.requestLocationUpdates(req, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc != null) {
                    currentLat = loc.getLatitude();
                    currentLon = loc.getLongitude();
                    runOnUiThread(() ->
                            binding.labelLokasi.setText(
                                    "📍 " + String.format("%.5f, %.5f", currentLat, currentLon))
                    );
                    Log.d(TAG, "Lokasi (update): " + currentLat + ", " + currentLon);
                }
            }
        }, Looper.getMainLooper());
    }

    // ── Kamera ───────────────────────────────────────────────────────────────

    private void nyalakanKamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewKamera.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector selector = pilihKamera(provider);
                provider.unbindAll();
                provider.bindToLifecycle(this, selector, preview, imageCapture);

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Kamera gagal dibuka: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private CameraSelector pilihKamera(ProcessCameraProvider provider) {
        try {
            CameraSelector front = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
            if (provider.hasCamera(front)) {
                pakaiKameraDepan = true;
                return front;
            }
        } catch (Exception ignored) {}

        try {
            CameraSelector back = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
            if (provider.hasCamera(back)) {
                pakaiKameraDepan = false;
                return back;
            }
        } catch (Exception ignored) {}

        return new CameraSelector.Builder().build();
    }

    // ── Foto & Absen ─────────────────────────────────────────────────────────

    private void ambilFotoDanAbsen() {
        if (imageCapture == null) {
            Toast.makeText(this, "Kamera belum siap!", Toast.LENGTH_SHORT).show();
            return;
        }

        setStatus("📸 Mengambil foto...");
        binding.tombolAbsen.setEnabled(false);

        File fotoFile = new File(getCacheDir(), "absen_foto.jpg");
        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(fotoFile).build();

        imageCapture.takePicture(options, executor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Bitmap bitmap = BitmapFactory.decodeFile(fotoFile.getAbsolutePath());
                        if (bitmap == null) {
                            runOnUiThread(() -> {
                                setStatus("❌ Gagal membaca foto!");
                                binding.tombolAbsen.setEnabled(true);
                            });
                            return;
                        }

                        Bitmap fixed = ImageUtils.fixBitmap(bitmap, pakaiKameraDepan);

                        String base64 = ImageUtils.bitmapToBase64(fixed);

                        Log.d(TAG, "Foto processed, base64 length: " + base64.length());

                        kirimAbsen(base64);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        runOnUiThread(() -> {
                            setStatus("❌ Gagal ambil foto: " + e.getMessage());
                            binding.tombolAbsen.setEnabled(true);
                        });
                    }
                });
    }

    private Bitmap flipHorizontal(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.preScale(-1f, 1f);
        return Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    // ── Kirim ke Server ──────────────────────────────────────────────────────

    private void kirimAbsen(String base64) {
        runOnUiThread(() -> {
            setStatus("🔄 Memproses wajah...");
            binding.progressBar.setVisibility(View.VISIBLE);
        });

        Map<String, Object> body = new HashMap<>();
        body.put("gambar", base64);

        if (currentLat != null) body.put("latitude", currentLat);
        if (currentLon != null) body.put("longitude", currentLon);

        ApiClient.getService().absen(body)
                .enqueue(new Callback<AbsenResponse>() {

                    @Override
                    public void onResponse(Call<AbsenResponse> call,
                                           Response<AbsenResponse> response) {

                        runOnUiThread(() -> {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.tombolAbsen.setEnabled(true);

                            if (!response.isSuccessful()) {
                                setStatus("❌ Server error: " + response.code());
                                return;
                            }

                            AbsenResponse hasil = response.body();

                            if (hasil == null) {
                                setStatus("❌ Response kosong dari server");
                                return;
                            }

                            if (hasil.sukses) {
                                tampilkanHasil(hasil);
                            } else {
                                setStatus("⚠️ " + hasil.pesan);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<AbsenResponse> call, Throwable t) {
                        runOnUiThread(() -> {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.tombolAbsen.setEnabled(true);
                            setStatus("❌ Gagal konek: " + t.getMessage());
                        });
                    }
                });
    }

    // ── Tampilkan Hasil Absen ─────────────────────────────────────────────────

    private void tampilkanHasil(AbsenResponse hasil) {
        String emojiStatus;
        String labelStatus;
        String labelTipe;

        if ("masuk".equals(hasil.tipe)) {
            labelTipe = "🏢 Absen Masuk";
            if ("tepat_waktu".equals(hasil.status)) {
                emojiStatus = "✅";
                labelStatus = "Tepat Waktu";
            } else {
                emojiStatus = "⚠️";
                labelStatus = "Terlambat";
            }
        } else if ("keluar".equals(hasil.tipe)) {
            labelTipe   = "🚪 Absen Keluar";
            emojiStatus = "✅";
            labelStatus = "Sudah Keluar";
        } else {
            labelTipe   = "📋 Absen";
            emojiStatus = "✅";
            labelStatus = hasil.status != null ? hasil.status : "-";
        }

        String lokasiTampil = (hasil.alamat != null && !hasil.alamat.isEmpty())
                ? hasil.alamat : "Lokasi tidak tersedia";

        setStatus(
                hasil.pesan + "\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "👤 Nama       : " + hasil.nama + "\n" +
                        "💼 Jabatan    : " + hasil.jabatan + "\n" +
                        "🏬 Departemen : " + hasil.departemen + "\n" +
                        "🎯 Keyakinan  : " + String.format("%.1f", hasil.keyakinan) + "%\n" +
                        "📌 Tipe       : " + labelTipe + "\n" +
                        "🔖 Status     : " + emojiStatus + " " + labelStatus + "\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "📍 Lokasi:\n" + lokasiTampil
        );

        if (hasil.alamat != null && !hasil.alamat.isEmpty()) {
            binding.labelLokasi.setText("📍 " + hasil.alamat);
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void setStatus(String msg) {
        binding.labelStatus.setText(msg);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                nyalakanKamera();
            } else {
                Toast.makeText(this, "Permission kamera ditolak!", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ambilLokasi();
            } else {
                binding.labelLokasi.setText("📍 Lokasi tidak tersedia");
                Toast.makeText(this, "Lokasi tidak diizinkan, absen tanpa lokasi",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}