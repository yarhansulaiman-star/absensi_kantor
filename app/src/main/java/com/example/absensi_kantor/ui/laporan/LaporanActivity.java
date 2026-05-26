package com.example.absensi_kantor.ui.laporan;

import com.example.absensi_kantor.R;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.databinding.ActivityLaporanBinding;
import com.example.absensi_kantor.model.laporan.LaporanResponse;
import com.example.absensi_kantor.ui.MainActivity;
import com.example.absensi_kantor.ui.auth.ProfilActivity;
import com.example.absensi_kantor.utils.DateUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LaporanActivity extends AppCompatActivity {

    private static final String TAG = "LaporanActivity";
    private ActivityLaporanBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLaporanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ApiClient.init(this);

        // ── Sesuaikan posisi header dengan status bar HP nyata ──────
        ViewCompat.setOnApplyWindowInsetsListener(
                binding.getRoot(), (v, insets) -> {
                    int statusBarHeight = insets.getInsets(
                            WindowInsetsCompat.Type.statusBars()).top;
                    binding.layoutHeader.setPadding(
                            binding.layoutHeader.getPaddingLeft(),
                            statusBarHeight + 16,
                            binding.layoutHeader.getPaddingRight(),
                            binding.layoutHeader.getPaddingBottom()
                    );
                    return insets;
                }
        );

        binding.labelTanggal.setText("📅 " + DateUtils.getTanggalTampil());
        muatLaporan(DateUtils.getTanggalHariIni());

        binding.tombolRefresh.setOnClickListener(v ->
                muatLaporan(DateUtils.getTanggalHariIni()));

        binding.tombolKembali.setOnClickListener(v -> finish());

        // ── Bottom Navigation ───────────────────────────────────────
        BottomNavigationView bottomNav = binding.bottomNav;
        bottomNav.setSelectedItemId(R.id.nav_laporan);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_riwayat) {
                startActivity(new Intent(this, RiwayatActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_laporan) {
                return true; // sudah di halaman ini
            } else if (id == R.id.nav_profil) {
                startActivity(new Intent(this, ProfilActivity.class));
                finish();
                return true;
            }

            return false;
        });
    }

    private void muatLaporan(String tanggal) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerLaporan.setVisibility(View.GONE);
        binding.labelKosong.setVisibility(View.GONE);

        ApiClient.getService()
                .laporan(tanggal)
                .enqueue(new Callback<LaporanResponse>() {
                    @Override
                    public void onResponse(Call<LaporanResponse> call,
                                           Response<LaporanResponse> response) {
                        binding.progressBar.setVisibility(View.GONE);
                        Log.d(TAG, "Laporan code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            LaporanResponse data = response.body();
                            binding.labelTotal.setText(
                                    "Total Hadir: " + data.total + " orang");

                            if (data.data != null && !data.data.isEmpty()) {
                                binding.recyclerLaporan.setLayoutManager(
                                        new LinearLayoutManager(LaporanActivity.this));
                                binding.recyclerLaporan.setAdapter(
                                        new LaporanAdapter(data.data));
                                binding.recyclerLaporan.setVisibility(View.VISIBLE);
                            } else {
                                binding.labelKosong.setVisibility(View.VISIBLE);
                            }
                        } else {
                            try {
                                String err = response.errorBody() != null
                                        ? response.errorBody().string() : "";
                                Log.e(TAG, "Error laporan: " + err);
                            } catch (Exception ignored) {}
                            Toast.makeText(LaporanActivity.this,
                                    "Gagal memuat laporan (kode " + response.code() + ")",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<LaporanResponse> call, Throwable t) {
                        binding.progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Laporan failure: " + t.getMessage());
                        Toast.makeText(LaporanActivity.this,
                                "Gagal memuat laporan!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}