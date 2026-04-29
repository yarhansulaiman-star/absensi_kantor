package com.example.absensi_kantor.ui.absen;

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
import android.graphics.Rect;
import android.location.Location;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.api.SessionManager;
import com.example.absensi_kantor.databinding.ActivityAbsenBinding;
import com.example.absensi_kantor.model.absen.AbsenResponse;
import com.example.absensi_kantor.utils.ImageUtils;
import com.example.absensi_kantor.utils.NotificationHelper;
import com.google.android.gms.location.*;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AbsenActivity extends AppCompatActivity {

    private static final String TAG               = "AbsenActivity";
    private static final int    CAMERA_PERMISSION  = 100;
    private static final int    LOCATION_PERMISSION = 101;

    private static final long  RECOGNITION_INTERVAL_MS = 2000;

    // ✅ FIX: Threshold keyakinan minimum di sisi Android
    // Real-time preview: 65% (lebih longgar, hanya tampilan)
    // Absen sungguhan: 75% (ketat, untuk keamanan)
    private static final float MIN_CONFIDENCE_PREVIEW = 65f;
    private static final float MIN_CONFIDENCE_ABSEN   = 75f;

    private ActivityAbsenBinding        binding;
    private SessionManager              session;
    private ImageCapture                imageCapture;
    private ImageAnalysis               imageAnalysis;
    private ExecutorService             executor;
    private boolean                     pakaiKameraDepan = false;

    private FaceOverlayView faceOverlay;

    private FusedLocationProviderClient fusedLocation;
    private Double currentLat = null;
    private Double currentLon = null;

    private FaceDetector faceDetector;

    private final Handler       recognitionHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean isRecognizing      = new AtomicBoolean(false);
    private       Runnable      recognitionRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding  = ActivityAbsenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ApiClient.init(this);
        session  = new SessionManager(this);
        executor = Executors.newSingleThreadExecutor();

        fusedLocation = LocationServices.getFusedLocationProviderClient(this);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        faceDetector = FaceDetection.getClient(options);

        faceOverlay = binding.faceOverlay;

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

                imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
                    @Override
                    @androidx.camera.core.ExperimentalGetImage
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        deteksiWajahRealtime(imageProxy);
                    }
                });

                CameraSelector selector = pilihKamera(provider);
                faceOverlay.setFrontCamera(pakaiKameraDepan);
                provider.unbindAll();
                provider.bindToLifecycle(this, selector,
                        preview, imageCapture, imageAnalysis);

                mulaiRealtimeRecognition();

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

    // ── Real-time Deteksi Wajah ──────────────────────────────────────────────

    @androidx.camera.core.ExperimentalGetImage
    private void deteksiWajahRealtime(ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                mediaImage, imageProxy.getImageInfo().getRotationDegrees());

        int imgW = imageProxy.getWidth();
        int imgH = imageProxy.getHeight();

        faceDetector.process(image)
                .addOnSuccessListener(faces ->
                        faceOverlay.setFaces(faces, imgW, imgH))
                .addOnFailureListener(e -> faceOverlay.clear())
                .addOnCompleteListener(task -> imageProxy.close());
    }

    // ── Real-time Recognition ────────────────────────────────────────────────

    private void mulaiRealtimeRecognition() {
        recognitionRunnable = new Runnable() {
            @Override
            public void run() {
                if (imageCapture != null && !isRecognizing.get()) {
                    ambilFotoUntukRecognition();
                }
                recognitionHandler.postDelayed(this, RECOGNITION_INTERVAL_MS);
            }
        };
        recognitionHandler.postDelayed(recognitionRunnable, RECOGNITION_INTERVAL_MS);
    }

    private void ambilFotoUntukRecognition() {
        isRecognizing.set(true);

        File fotoFile = new File(getCacheDir(), "recognition_foto.jpg");
        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(fotoFile).build();

        imageCapture.takePicture(options, executor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Bitmap bitmap = BitmapFactory.decodeFile(fotoFile.getAbsolutePath());
                        if (bitmap == null) {
                            isRecognizing.set(false);
                            return;
                        }
                        Bitmap fixed = ImageUtils.fixBitmap(bitmap, pakaiKameraDepan);
                        kirimUntukRecognition(fixed);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        isRecognizing.set(false);
                        Log.e(TAG, "Recognition foto error: " + e.getMessage());
                    }
                });
    }

    private void kirimUntukRecognition(Bitmap bitmap) {
        String base64 = ImageUtils.bitmapToBase64(bitmap);

        Map<String, Object> body = new HashMap<>();
        body.put("gambar", base64);

        ApiClient.getService().kenali(body)
                .enqueue(new Callback<AbsenResponse>() {
                    @Override
                    public void onResponse(Call<AbsenResponse> call,
                                           Response<AbsenResponse> response) {
                        isRecognizing.set(false);
                        if (!response.isSuccessful() || response.body() == null) return;

                        AbsenResponse hasil = response.body();
                        runOnUiThread(() -> {
                            // ✅ FIX: Filter keyakinan minimum untuk preview
                            if (hasil.sukses
                                    && hasil.nama != null
                                    && hasil.keyakinan >= MIN_CONFIDENCE_PREVIEW) {
                                faceOverlay.showRecognitionResult(
                                        hasil.nama, (float) hasil.keyakinan);
                            } else {
                                // Keyakinan rendah → jangan tampilkan nama siapapun
                                faceOverlay.hideRecognitionResult();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<AbsenResponse> call, Throwable t) {
                        isRecognizing.set(false);
                        Log.e(TAG, "Recognition gagal: " + t.getMessage());
                    }
                });
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
                        Log.d(TAG, "Foto processed, size: "
                                + fixed.getWidth() + "x" + fixed.getHeight());
                        deteksiWajahLaluKirim(fixed);
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

    // ── ML Kit: Deteksi Wajah ────────────────────────────────────────────────

    private void deteksiWajahLaluKirim(Bitmap bitmap) {
        runOnUiThread(() -> setStatus("🔍 Mendeteksi wajah..."));

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        faceDetector.process(image)
                .addOnSuccessListener(faces -> {

                    if (faces.isEmpty()) {
                        runOnUiThread(() -> {
                            setStatus("⚠️ Wajah tidak terdeteksi!\nPastikan wajah terlihat jelas dan pencahayaan cukup.");
                            binding.tombolAbsen.setEnabled(true);
                        });
                        return;
                    }

                    Face face = faces.get(0);

                    Rect bounds = face.getBoundingBox();
                    Log.d(TAG, "Wajah → x=" + bounds.left + ", y=" + bounds.top
                            + ", w=" + bounds.width() + ", h=" + bounds.height());

                    Float leftEye  = face.getLeftEyeOpenProbability();
                    Float rightEye = face.getRightEyeOpenProbability();
                    Log.d(TAG, "Mata kiri=" + leftEye + ", Mata kanan=" + rightEye);

                    if (leftEye != null && rightEye != null) {
                        if (leftEye < 0.3f && rightEye < 0.3f) {
                            runOnUiThread(() -> {
                                setStatus("⚠️ Mata tertutup!\nBuka mata dan coba lagi.");
                                binding.tombolAbsen.setEnabled(true);
                            });
                            return;
                        }
                    }

                    float rotY = face.getHeadEulerAngleY();
                    float rotZ = face.getHeadEulerAngleZ();
                    Log.d(TAG, "Rotasi → Y=" + rotY + ", Z=" + rotZ);

                    if (Math.abs(rotY) > 30 || Math.abs(rotZ) > 30) {
                        runOnUiThread(() -> {
                            setStatus("⚠️ Wajah terlalu miring!\nHadapkan wajah ke kamera.");
                            binding.tombolAbsen.setEnabled(true);
                        });
                        return;
                    }

                    runOnUiThread(() -> setStatus("✅ Wajah terdeteksi, mengirim..."));

                    String base64 = ImageUtils.bitmapToBase64(bitmap);
                    Log.d(TAG, "Base64 length: " + base64.length());
                    kirimAbsen(base64);
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        setStatus("❌ Gagal deteksi wajah: " + e.getMessage());
                        binding.tombolAbsen.setEnabled(true);
                    });
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

        if (currentLat != null) body.put("latitude",  currentLat);
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
                                // ✅ FIX: Double-check keyakinan di sisi Android
                                // Meski server sudah filter, Android ikut validasi
                                if (hasil.keyakinan < MIN_CONFIDENCE_ABSEN) {
                                    setStatus("⚠️ Wajah tidak dikenali dengan pasti.\n"
                                            + "Keyakinan: " + String.format("%.1f", hasil.keyakinan) + "%\n"
                                            + "Coba lagi dengan pencahayaan lebih baik.");
                                    return;
                                }

                                tampilkanHasil(hasil);

                                String waktuSekarang = new SimpleDateFormat(
                                        "HH:mm", Locale.getDefault()).format(new Date());
                                String jenisAbsen = "keluar".equals(hasil.tipe) ? "Pulang" : "Masuk";
                                NotificationHelper.tampilkanAbsenBerhasil(
                                        AbsenActivity.this, jenisAbsen, waktuSekarang);

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
                        "👤 Nama       : " + hasil.nama       + "\n" +
                        "💼 Jabatan    : " + hasil.jabatan    + "\n" +
                        "🏬 Departemen : " + hasil.departemen + "\n" +
                        "🎯 Keyakinan  : " + String.format("%.1f", hasil.keyakinan) + "%\n" +
                        "📌 Tipe       : " + labelTipe        + "\n" +
                        "🔖 Status     : " + emojiStatus + " " + labelStatus + "\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "📍 Lokasi:\n"     + lokasiTampil
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
    protected void onPause() {
        super.onPause();
        if (recognitionRunnable != null)
            recognitionHandler.removeCallbacks(recognitionRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (recognitionRunnable != null)
            recognitionHandler.postDelayed(recognitionRunnable, RECOGNITION_INTERVAL_MS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recognitionRunnable != null)
            recognitionHandler.removeCallbacks(recognitionRunnable);
        executor.shutdown();
        faceDetector.close();
    }
}