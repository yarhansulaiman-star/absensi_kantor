package com.example.absensi_kantor.ui.absen;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("UastIncorrectHttpHeaderInspection")
@androidx.annotation.OptIn(markerClass = ExperimentalGetImage.class)
public class AbsenActivity extends AppCompatActivity {

    private static final String TAG                 = "AbsenActivity";
    private static final int    CAMERA_PERMISSION   = 100;
    private static final int    LOCATION_PERMISSION = 101;

    private static final long  RECOGNITION_INTERVAL_MS = 2000;
    private static final float MIN_CONFIDENCE_PREVIEW  = 50f;
    private static final float MIN_CONFIDENCE_ABSEN    = 50f;

    // ✅ LIVENESS: Konstanta threshold & timeout untuk deteksi kedipan
    private static final float EYE_OPEN_THRESHOLD   = 0.6f;   // mata dianggap terbuka
    private static final float EYE_CLOSED_THRESHOLD = 0.25f;  // mata dianggap tertutup
    private static final long  LIVENESS_TIMEOUT_MS   = 8000;  // batas waktu 1 siklus liveness
    private static final long  LIVENESS_HOLD_OPEN_MS = 400;   // wajib "kebuka" dulu sesaat sblm mulai

    // ✅ LIVENESS: State machine untuk challenge-response kedipan mata
    private enum LivenessState {
        NUNGGU_WAJAH,           // belum ada wajah / mata belum kebuka stabil
        MATA_TERBUKA_AWAL,      // mata sudah kebuka, siap nunggu kedipan
        NUNGGU_MATA_TERTUTUP,   // instruksi: silakan berkedip
        NUNGGU_MATA_TERBUKA_LAGI, // mata sudah kedeteksi tertutup, tunggu kebuka lagi
        LIVENESS_OK             // lolos! boleh absen
    }

    private volatile LivenessState livenessState   = LivenessState.NUNGGU_WAJAH;
    private volatile long          livenessStartTs = 0L;
    private volatile long          eyesOpenSinceTs = 0L;

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

        // ✅ LIVENESS: tombol absen dikunci sampai liveness lolos
        binding.tombolAbsen.setEnabled(false);
        resetLiveness();

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

    // ── Lokasi GPS ────────────────────────────────────────────────────────────

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
                    runOnUiThread(() -> binding.labelLokasi.setText(
                            "📍 " + String.format("%.5f, %.5f", currentLat, currentLon)));
                }
            }
        }, Looper.getMainLooper());
    }

    // ── Kamera ────────────────────────────────────────────────────────────────

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

                imageAnalysis.setAnalyzer(executor, this::deteksiWajahRealtime);

                CameraSelector selector = pilihKamera(provider);
                faceOverlay.setFrontCamera(pakaiKameraDepan);
                provider.unbindAll();
                provider.bindToLifecycle(this, selector,
                        preview, imageCapture, imageAnalysis);

                mulaiRealtimeRecognition();

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Kamera gagal dibuka: " + e.getMessage(),
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

    // ── Real-time Deteksi Wajah + LIVENESS ────────────────────────────────────

    @androidx.annotation.OptIn(markerClass = ExperimentalGetImage.class)
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
                .addOnSuccessListener(faces -> {
                    faceOverlay.setFaces(faces, imgW, imgH);
                    prosesLiveness(faces); // ✅ LIVENESS: cek tiap frame
                })
                .addOnFailureListener(e -> {
                    faceOverlay.clear();
                    // ✅ LIVENESS: gak ada wajah kedeteksi → anggap terputus, reset
                    handleWajahHilang();
                })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    // ✅ LIVENESS: Ambil wajah terbesar dari daftar hasil deteksi
    private Face ambilWajahTerbesarUntukLiveness(List<Face> faces) {
        if (faces == null || faces.isEmpty()) return null;
        Face terbesar = null;
        long luasTerbesar = 0;
        for (Face f : faces) {
            Rect b = f.getBoundingBox();
            long luas = (long) b.width() * b.height();
            if (luas > luasTerbesar) {
                luasTerbesar = luas;
                terbesar = f;
            }
        }
        return terbesar;
    }

    // ✅ LIVENESS: Inti state machine challenge-response kedipan mata.
    // Alur: wajah harus kebuka mata dulu (stabil) -> instruksi kedip ->
    // terdeteksi tertutup -> terdeteksi terbuka lagi -> LIVENESS_OK.
    // Semua harus terjadi dalam LIVENESS_TIMEOUT_MS, kalau lewat -> reset dari awal.
    private void prosesLiveness(List<Face> faces) {
        Face face = ambilWajahTerbesarUntukLiveness(faces);
        long now = System.currentTimeMillis();

        if (face == null) {
            handleWajahHilang();
            return;
        }

        Float leftEye  = face.getLeftEyeOpenProbability();
        Float rightEye = face.getRightEyeOpenProbability();

        // Kalau ML Kit gak kasih data probability mata (kadang null), skip frame ini
        if (leftEye == null || rightEye == null) return;

        boolean mataTerbuka   = leftEye > EYE_OPEN_THRESHOLD && rightEye > EYE_OPEN_THRESHOLD;
        boolean mataTertutup  = leftEye < EYE_CLOSED_THRESHOLD && rightEye < EYE_CLOSED_THRESHOLD;

        // Kalau sudah lolos, gak perlu diproses ulang sampai direset (misal setelah kirim absen)
        if (livenessState == LivenessState.LIVENESS_OK) return;

        // Cek timeout siklus liveness yang sedang berjalan
        if (livenessStartTs > 0 && (now - livenessStartTs) > LIVENESS_TIMEOUT_MS
                && livenessState != LivenessState.NUNGGU_WAJAH) {
            Log.d(TAG, "Liveness timeout, reset.");
            resetLivenessKeAwal("⏳ Waktu habis, silakan coba lagi.");
            return;
        }

        switch (livenessState) {
            case NUNGGU_WAJAH:
                if (mataTerbuka) {
                    if (eyesOpenSinceTs == 0) eyesOpenSinceTs = now;
                    if (now - eyesOpenSinceTs >= LIVENESS_HOLD_OPEN_MS) {
                        livenessState   = LivenessState.MATA_TERBUKA_AWAL;
                        livenessStartTs = now;
                        setStatusUi("👁️ Wajah terdeteksi. Bersiap untuk kedip...");
                    }
                } else {
                    eyesOpenSinceTs = 0;
                }
                break;

            case MATA_TERBUKA_AWAL:
                livenessState = LivenessState.NUNGGU_MATA_TERTUTUP;
                setStatusUi("😉 Silakan BERKEDIP untuk verifikasi...");
                break;

            case NUNGGU_MATA_TERTUTUP:
                if (mataTertutup) {
                    livenessState = LivenessState.NUNGGU_MATA_TERBUKA_LAGI;
                    setStatusUi("👀 Bagus! Sekarang buka mata lagi...");
                }
                break;

            case NUNGGU_MATA_TERBUKA_LAGI:
                if (mataTerbuka) {
                    livenessState = LivenessState.LIVENESS_OK;
                    setStatusUi("✅ Verifikasi wajah asli berhasil! Silakan tekan Absen.");
                    setTombolAbsenEnabled(true);
                }
                break;

            default:
                break;
        }
    }

    // ✅ LIVENESS: Dipanggil kalau wajah gak kedeteksi sama sekali di 1 frame.
    // Kalau state udah di tengah proses kedipan dan wajah hilang, reset ke awal
    // (supaya orang gak bisa "curang" ganti dari foto ke wajah asli di tengah proses).
    private void handleWajahHilang() {
        if (livenessState != LivenessState.NUNGGU_WAJAH
                && livenessState != LivenessState.LIVENESS_OK) {
            resetLivenessKeAwal("⚠️ Wajah hilang dari kamera, ulangi verifikasi.");
        } else if (livenessState == LivenessState.NUNGGU_WAJAH) {
            eyesOpenSinceTs = 0;
        }
    }

    private void resetLivenessKeAwal(String pesan) {
        livenessState   = LivenessState.NUNGGU_WAJAH;
        livenessStartTs = 0;
        eyesOpenSinceTs = 0;
        setTombolAbsenEnabled(false);
        setStatusUi(pesan);
    }

    // ✅ LIVENESS: Reset total, dipanggil saat activity pertama dibuka
    // atau setelah absen berhasil/gagal terkirim (wajib verifikasi ulang tiap sesi)
    private void resetLiveness() {
        livenessState   = LivenessState.NUNGGU_WAJAH;
        livenessStartTs = 0;
        eyesOpenSinceTs = 0;
        setTombolAbsenEnabled(false);
        setStatusUi("👁️ Posisikan wajah Anda di depan kamera...");
    }

    private void setTombolAbsenEnabled(boolean enabled) {
        runOnUiThread(() -> binding.tombolAbsen.setEnabled(enabled));
    }

    private void setStatusUi(String pesan) {
        runOnUiThread(() -> setStatus(pesan));
    }

    // ── Real-time Recognition (preview nama, terpisah dari liveness) ──────────

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
                    public void onResponse(@NonNull Call<AbsenResponse> call,
                                           @NonNull Response<AbsenResponse> response) {
                        isRecognizing.set(false);
                        if (!response.isSuccessful() || response.body() == null) return;

                        AbsenResponse hasil = response.body();
                        runOnUiThread(() -> {
                            if (hasil.sukses
                                    && hasil.nama != null
                                    && hasil.keyakinan >= MIN_CONFIDENCE_PREVIEW) {
                                faceOverlay.showRecognitionResult(
                                        hasil.nama, (float) hasil.keyakinan);
                            } else {
                                faceOverlay.hideRecognitionResult();
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull Call<AbsenResponse> call, @NonNull Throwable t) {
                        isRecognizing.set(false);
                        Log.e(TAG, "Recognition gagal: " + t.getMessage());
                    }
                });
    }

    // ── Foto & Absen ──────────────────────────────────────────────────────────

    private void ambilFotoDanAbsen() {
        // ✅ LIVENESS: Gerbang utama — gak boleh absen kalau liveness belum lolos.
        // Ini jaga-jaga tambahan; tombol seharusnya sudah disabled duluan,
        // tapi dicek lagi di sini untuk keamanan (defense in depth).
        if (livenessState != LivenessState.LIVENESS_OK) {
            Toast.makeText(this, "Verifikasi wajah (kedip) belum selesai!", Toast.LENGTH_SHORT).show();
            return;
        }

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
                                // ✅ LIVENESS: kalau gagal, wajib verifikasi ulang, jangan asal re-enable tombol
                                resetLiveness();
                            });
                            return;
                        }
                        Bitmap fixed = ImageUtils.fixBitmap(bitmap, pakaiKameraDepan);
                        deteksiWajahLaluKirim(fixed);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        runOnUiThread(() -> {
                            setStatus("❌ Gagal ambil foto: " + e.getMessage());
                            resetLiveness();
                        });
                    }
                });
    }

    // ── ML Kit: Deteksi Wajah pada foto final sebelum kirim ────────────────────

    private void deteksiWajahLaluKirim(Bitmap bitmap) {
        runOnUiThread(() -> setStatus("🔍 Mendeteksi wajah..."));

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.isEmpty()) {
                        runOnUiThread(() -> {
                            setStatus("⚠️ Wajah tidak terdeteksi!\nPastikan wajah terlihat jelas.");
                            resetLiveness();
                        });
                        return;
                    }

                    Face face = faces.get(0);
                    Rect bounds = face.getBoundingBox();
                    Log.d(TAG, "Wajah → " + bounds.width() + "x" + bounds.height());

                    Float leftEye  = face.getLeftEyeOpenProbability();
                    Float rightEye = face.getRightEyeOpenProbability();
                    if (leftEye != null && rightEye != null
                            && leftEye < 0.3f && rightEye < 0.3f) {
                        runOnUiThread(() -> {
                            setStatus("⚠️ Mata tertutup! Buka mata dan coba lagi.");
                            resetLiveness();
                        });
                        return;
                    }

                    float rotY = face.getHeadEulerAngleY();
                    float rotZ = face.getHeadEulerAngleZ();
                    if (Math.abs(rotY) > 30 || Math.abs(rotZ) > 30) {
                        runOnUiThread(() -> {
                            setStatus("⚠️ Wajah terlalu miring! Hadapkan ke kamera.");
                            resetLiveness();
                        });
                        return;
                    }

                    runOnUiThread(() -> setStatus("✅ Wajah terdeteksi, mengirim..."));
                    String base64 = ImageUtils.bitmapToBase64(bitmap);
                    kirimAbsen(base64);
                })
                .addOnFailureListener(e -> runOnUiThread(() -> {
                    setStatus("❌ Gagal deteksi wajah: " + e.getMessage());
                    resetLiveness();
                }));
    }

    // ── Kirim ke Server ───────────────────────────────────────────────────────

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
                    public void onResponse(@NonNull Call<AbsenResponse> call,
                                           @NonNull Response<AbsenResponse> response) {
                        runOnUiThread(() -> {
                            binding.progressBar.setVisibility(View.GONE);
                            // ✅ LIVENESS: apapun hasilnya, wajib verifikasi ulang untuk absen berikutnya
                            resetLiveness();

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
                                if (hasil.keyakinan < MIN_CONFIDENCE_ABSEN) {
                                    setStatus("⚠️ Keyakinan terlalu rendah: "
                                            + String.format("%.1f", hasil.keyakinan) + "%\n"
                                            + "Coba lagi dengan pencahayaan lebih baik.");
                                    return;
                                }
                                tampilkanHasil(hasil);

                                String waktu = new SimpleDateFormat(
                                        "HH:mm", Locale.getDefault()).format(new Date());
                                String jenis = "keluar".equals(hasil.tipe) ? "Pulang" : "Masuk";
                                NotificationHelper.tampilkanAbsenBerhasil(
                                        AbsenActivity.this, jenis, waktu);
                            } else {
                                setStatus("⚠️ " + hasil.pesan);
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull Call<AbsenResponse> call, @NonNull Throwable t) {
                        runOnUiThread(() -> {
                            binding.progressBar.setVisibility(View.GONE);
                            resetLiveness();
                            setStatus("❌ Gagal konek: " + t.getMessage());
                        });
                    }
                });
    }

    // ── Tampilkan Hasil Absen ─────────────────────────────────────────────────

    private void tampilkanHasil(AbsenResponse hasil) {
        String emojiStatus, labelStatus, labelTipe;

        if ("masuk".equals(hasil.tipe)) {
            labelTipe = "🏢 Absen Masuk";
            if ("tepat_waktu".equals(hasil.status)) {
                emojiStatus = "✅"; labelStatus = "Tepat Waktu";
            } else {
                emojiStatus = "⚠️"; labelStatus = "Terlambat";
            }
        } else if ("keluar".equals(hasil.tipe)) {
            labelTipe = "🚪 Absen Keluar";
            emojiStatus = "✅"; labelStatus = "Sudah Keluar";
        } else {
            labelTipe = "📋 Absen";
            emojiStatus = "✅";
            labelStatus = hasil.status != null ? hasil.status : "-";
        }

        String lokasi = (hasil.alamat != null && !hasil.alamat.isEmpty())
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
                        "📍 Lokasi:\n"     + lokasi
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
                Toast.makeText(this, "Absen tanpa lokasi", Toast.LENGTH_SHORT).show();
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
        // ✅ LIVENESS: setiap kembali ke activity ini, wajib verifikasi ulang dari nol
        resetLiveness();
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