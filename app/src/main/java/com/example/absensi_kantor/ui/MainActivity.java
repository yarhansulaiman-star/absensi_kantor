package com.example.absensi_kantor.ui;

import androidx.appcompat.app.AppCompatActivity;
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
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ── Status bar color (antisipasi deprecated) ────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(0, 0);
            }
        } else {
            getWindow().setStatusBarColor(0xFF0F3460);
        }

        View header = findViewById(R.id.headerLayout);
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(
                    v.getPaddingLeft(),
                    statusBarHeight + 16,
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );
            return insets;
        });

        ApiClient.reset();
        ApiClient.init(this);
        session = new SessionManager(this);

        setSupportActionBar(binding.toolbar);

        binding.labelWelcome.setText("Selamat datang, " + session.getUsername() + "!");
        binding.labelRole.setText("Role: " + session.getRole());

        String role = session.getRole();
        if ("hrd".equals(role) || "admin".equals(role)) {
            binding.cardGaji.setVisibility(View.VISIBLE);
        } else {
            binding.cardGaji.setVisibility(View.GONE);
        }

        // ── Listener menu ──────────────────────────────────────────
        binding.tombolAbsen.setOnClickListener(v ->
                startActivity(new Intent(this, AbsenActivity.class)));

        binding.tombolLaporan.setOnClickListener(v ->
                startActivity(new Intent(this, LaporanActivity.class)));

        binding.tombolRiwayat.setOnClickListener(v ->
                startActivity(new Intent(this, RiwayatActivity.class)));

        binding.tombolExportPdf.setOnClickListener(v -> exportPdf());

        binding.tombolGaji.setOnClickListener(v ->
                startActivity(new Intent(this, GajiActivity.class)));

        // ── Surat Izin ──────────────────────────────────────────────
        binding.tombolSuratIzin.setOnClickListener(v ->
                startActivity(new Intent(this, SuratIzinActivity.class)));

        // ── Statistik ──────────────────────────────────────────────
        binding.tombolStatistik.setOnClickListener(v ->
                startActivity(new Intent(this, DashboardStatistikActivity.class)));

        // ── Bottom Navigation ───────────────────────────────────────
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

        // ── Cek status absen hari ini ───────────────────────────────
        cekStatusAbsenHariIni();
    }

    // ── Cek apakah sudah absen hari ini dari API ────────────────────
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

                        // Ubah status jadi sudah absen
                        binding.tvStatusAbsen.setText("Sudah Absen");
                        binding.tvStatusAbsen.setTextColor(0xFF4ADE80); // hijau

                        // Tampilkan jam masuk
                        if (item.jam_masuk != null) {
                            binding.tvJamAbsen.setVisibility(View.VISIBLE);
                            binding.tvJamAbsen.setText("Masuk: " + item.jam_masuk);
                        }

                        // Nonaktifkan tombol absen supaya tidak bisa double absen
                        binding.tombolAbsen.setAlpha(0.5f);
                        binding.tombolAbsen.setClickable(false);

                        return; // sudah ketemu, stop loop
                    }
                }
                // Tidak ketemu tanggal hari ini = belum absen, UI tetap default
            }

            @Override
            public void onFailure(Call<RiwayatResponse> call, Throwable t) {
                // Gagal koneksi, biarkan status default "Belum Absen"
            }
        });
    }

    private void exportPdf() {
        binding.tombolExportPdf.setEnabled(false);
        Toast.makeText(this, "⏳ Mengunduh PDF...", Toast.LENGTH_SHORT).show();

        String tanggal = DateUtils.getTanggalHariIni();

        ApiClient.getService().laporanPdf(tanggal)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call,
                                           Response<ResponseBody> response) {
                        binding.tombolExportPdf.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            simpanPdf(response.body(), tanggal);
                        } else {
                            Toast.makeText(MainActivity.this,
                                    "Gagal download PDF!", Toast.LENGTH_SHORT).show();
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

    private void simpanPdf(ResponseBody body, String tanggal) {
        try {
            File dir  = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(dir, "laporan_" + tanggal + ".pdf");

            InputStream is = body.byteStream();
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int read;

            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }

            fos.flush();
            fos.close();
            is.close();

            Toast.makeText(this,
                    "✅ PDF disimpan:\n" + file.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this,
                    "Gagal simpan PDF: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }
}