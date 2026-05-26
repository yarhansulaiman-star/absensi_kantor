package com.example.absensi_kantor.ui;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.Toast;

import com.example.absensi_kantor.R;
import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.api.SessionManager;
import com.example.absensi_kantor.databinding.ActivityMainBinding;
import com.example.absensi_kantor.model.absen.RiwayatResponse;
import com.example.absensi_kantor.ui.absen.AbsenActivity;
import com.example.absensi_kantor.ui.gaji.GajiActivity;
import com.example.absensi_kantor.ui.laporan.LaporanActivity;
import com.example.absensi_kantor.ui.auth.ProfilActivity;
import com.example.absensi_kantor.ui.laporan.RiwayatActivity;
import com.example.absensi_kantor.ui.izin.SuratIzinActivity;
import com.example.absensi_kantor.utils.DateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity {

    private ActivityMainBinding binding;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ── Status bar color ─────────────────────────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) controller.setSystemBarsAppearance(0, 0);
        } else {
            getWindow().setStatusBarColor(0xFF0F3460);
        }

        View header = findViewById(R.id.headerLayout);
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), statusBarHeight + 16,
                    v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        ApiClient.reset();
        ApiClient.init(this);
        session = new SessionManager(this);

        setSupportActionBar(binding.toolbar);

        binding.labelWelcome.setText("Selamat datang, " + session.getUsername() + "!");
        binding.labelRole.setText("Role: " + session.getRole());

        // ── Tampilkan menu Kelola Gaji hanya untuk HRD/admin ─────
        String role = session.getRole();
        if ("hrd".equals(role) || "admin".equals(role)) {
            binding.cardGaji.setVisibility(View.VISIBLE);
        } else {
            binding.cardGaji.setVisibility(View.GONE);
        }

        // ── Listener menu ────────────────────────────────────────
        binding.tombolAbsen.setOnClickListener(v ->
                startActivity(new Intent(this, AbsenActivity.class)));

        binding.tombolLaporan.setOnClickListener(v ->
                startActivity(new Intent(this, LaporanActivity.class)));

        binding.tombolRiwayat.setOnClickListener(v ->
                startActivity(new Intent(this, RiwayatActivity.class)));

        // Export slip gaji (dipindah dari GajiActivity)
        binding.tombolExportPdf.setOnClickListener(v -> exportSlipGaji());

        binding.tombolGaji.setOnClickListener(v ->
                startActivity(new Intent(this, GajiActivity.class)));

        binding.tombolSuratIzin.setOnClickListener(v ->
                startActivity(new Intent(this, SuratIzinActivity.class)));

        binding.tombolStatistik.setOnClickListener(v ->
                startActivity(new Intent(this, DashboardStatistikActivity.class)));

        // ✅ Kalender — buka KalenderActivity
        binding.tombolKalender.setOnClickListener(v ->
                startActivity(new Intent(this, com.example.absensi_kantor.ui.KalenderActivity.class)));

        // ── Bottom Navigation ────────────────────────────────────
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_riwayat) {
                startActivity(new Intent(this, RiwayatActivity.class));
                return true;
            } else if (id == R.id.nav_laporan) {
                startActivity(new Intent(this, LaporanActivity.class));
                return true;
            } else if (id == R.id.nav_profil) {
                startActivity(new Intent(this, ProfilActivity.class));
                return true;
            }
            return false;
        });

        // ── Cek status absen hari ini ────────────────────────────
        cekStatusAbsenHariIni();
    }

    private void cekStatusAbsenHariIni() {
        ApiClient.getService().riwayat().enqueue(new Callback<RiwayatResponse>() {
            @Override
            public void onResponse(Call<RiwayatResponse> call, Response<RiwayatResponse> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                List<RiwayatResponse.DataRiwayat> list = response.body().data;
                if (list == null || list.isEmpty()) return;

                String tanggalHariIni = DateUtils.getTanggalHariIni();

                for (RiwayatResponse.DataRiwayat item : list) {
                    if (tanggalHariIni.equals(item.tanggal)) {
                        binding.tvStatusAbsen.setText("Sudah Absen");
                        binding.tvStatusAbsen.setTextColor(0xFF4ADE80);

                        if (item.jam_masuk != null) {
                            binding.tvJamAbsen.setVisibility(View.VISIBLE);
                            binding.tvJamAbsen.setText("Masuk: " + item.jam_masuk);
                        }

                        binding.tombolAbsen.setAlpha(0.5f);
                        binding.tombolAbsen.setClickable(false);
                        return;
                    }
                }
            }

            @Override
            public void onFailure(Call<RiwayatResponse> call, Throwable t) {
                // Biarkan status default "Belum Absen"
            }
        });
    }

    // Export slip gaji (dipindah dari GajiActivity)
    private void exportSlipGaji() {
        int userId = session.getUserId();
        int bulan  = Calendar.getInstance().get(Calendar.MONTH) + 1;
        int tahun  = Calendar.getInstance().get(Calendar.YEAR);

        binding.tombolExportPdf.setEnabled(false);
        Toast.makeText(this, "⏳ Mengunduh slip gaji...", Toast.LENGTH_SHORT).show();

        ApiClient.getService().exportSlipGaji(userId, bulan, tahun)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call,
                                           Response<ResponseBody> response) {
                        binding.tombolExportPdf.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            String namaFile = "slip_gaji_" + bulan + "_" + tahun + ".pdf";
                            simpanPdf(response.body(), namaFile);
                        } else {
                            Toast.makeText(MainActivity.this,
                                    "Gagal download slip gaji!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        binding.tombolExportPdf.setEnabled(true);
                        Toast.makeText(MainActivity.this,
                                "Koneksi gagal: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void simpanPdf(ResponseBody body, String namaFile) {
        try {
            File dir  = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(dir, namaFile);

            InputStream is   = body.byteStream();
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int read;

            while ((read = is.read(buffer)) != -1) fos.write(buffer, 0, read);

            fos.flush();
            fos.close();
            is.close();

            Toast.makeText(this,
                    "✅ Slip gaji disimpan:\n" + file.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this,
                    "Gagal simpan PDF: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }
}