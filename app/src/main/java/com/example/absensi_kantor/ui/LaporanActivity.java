package com.example.absensi_kantor.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.databinding.ActivityLaporanBinding;
import com.example.absensi_kantor.model.LaporanResponse;
import com.example.absensi_kantor.utils.DateUtils;
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

        // ✅ Init ApiClient agar token otomatis diinject
        ApiClient.init(this);

        binding.labelTanggal.setText("📅 " + DateUtils.getTanggalTampil());
        muatLaporan(DateUtils.getTanggalHariIni());

        binding.tombolRefresh.setOnClickListener(v ->
                muatLaporan(DateUtils.getTanggalHariIni()));

        binding.tombolKembali.setOnClickListener(v -> finish());
    }

    private void muatLaporan(String tanggal) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerLaporan.setVisibility(View.GONE);
        binding.labelKosong.setVisibility(View.GONE);

        // ✅ Hapus session.getToken() — token sudah otomatis dari interceptor
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