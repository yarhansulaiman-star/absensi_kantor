package com.example.absensi_kantor.ui;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.databinding.ActivityDashboardStatistikBinding;
import com.example.absensi_kantor.model.absen.RiwayatResponse;

import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardStatistikActivity extends AppCompatActivity {

    private ActivityDashboardStatistikBinding binding;
    private int bulanDipilih;
    private int tahunDipilih;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardStatistikBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("📊 Statistik Kehadiran");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Calendar cal = Calendar.getInstance();
        bulanDipilih = cal.get(Calendar.MONTH) + 1;
        tahunDipilih = cal.get(Calendar.YEAR);

        setupSpinnerBulan();
        setupSpinnerTahun();
        muatStatistik();

        binding.tombolRefresh.setOnClickListener(v -> muatStatistik());
    }

    private void setupSpinnerBulan() {
        String[] bulan = {"Januari","Februari","Maret","April","Mei","Juni",
                "Juli","Agustus","September","Oktober","November","Desember"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, bulan);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerBulan.setAdapter(adapter);
        binding.spinnerBulan.setSelection(bulanDipilih - 1);
        binding.spinnerBulan.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                bulanDipilih = pos + 1;
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupSpinnerTahun() {
        int sekarang = Calendar.getInstance().get(Calendar.YEAR);
        String[] tahun = {
                String.valueOf(sekarang),
                String.valueOf(sekarang - 1),
                String.valueOf(sekarang - 2)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, tahun);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerTahun.setAdapter(adapter);
        binding.spinnerTahun.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                tahunDipilih = Integer.parseInt(tahun[pos]);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void muatStatistik() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.layoutKonten.setVisibility(View.GONE);
        binding.labelKosong.setVisibility(View.GONE);

        ApiClient.getService().riwayat().enqueue(new Callback<RiwayatResponse>() {
            @Override
            public void onResponse(Call<RiwayatResponse> call,
                                   Response<RiwayatResponse> response) {
                binding.progressBar.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null
                        || !response.body().sukses) {
                    binding.labelKosong.setText("Gagal memuat data");
                    binding.labelKosong.setVisibility(View.VISIBLE);
                    return;
                }

                List<RiwayatResponse.DataRiwayat> data = response.body().data;
                if (data == null || data.isEmpty()) {
                    binding.labelKosong.setText("Belum ada data absensi");
                    binding.labelKosong.setVisibility(View.VISIBLE);
                    return;
                }

                hitungDanTampilkan(data);

            }

            @Override
            public void onFailure(Call<RiwayatResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(DashboardStatistikActivity.this,
                        "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hitungDanTampilkan(List<RiwayatResponse.DataRiwayat> data) {
        for (RiwayatResponse.DataRiwayat item : data) {
            android.util.Log.d("DEBUG_STAT", "tanggal=[" + item.tanggal + "] status=[" + item.status + "]");
        }
        int hadir     = 0;
        int terlambat = 0;
        int alpha     = 0;
        int izin      = 0;

        // Filter format: "yyyy-MM" contoh "2026-05"
        String targetBulan = String.format("%04d-%02d", tahunDipilih, bulanDipilih);

        for (RiwayatResponse.DataRiwayat item : data) {
            // Skip data di luar bulan yang dipilih
            if (item.tanggal == null || !item.tanggal.startsWith(targetBulan)) continue;
            if (item.status  == null) continue;

            // Normalisasi status: lowercase + trim untuk menghindari mismatch
            String status = item.status.toLowerCase().trim();

            switch (status) {
                case "hadir":
                case "tepat_waktu":
                case "tepat waktu":
                    hadir++;
                    break;
                case "terlambat":
                    hadir++;      // terlambat tetap dihitung hadir
                    terlambat++;
                    break;
                case "alpha":
                case "tidak hadir":
                    alpha++;
                    break;
                case "izin":
                case "sakit":
                    izin++;
                    break;
            }
        }

        int totalHari   = hadir + alpha + izin;
        int persenHadir = totalHari > 0 ? (hadir * 100 / totalHari) : 0;

        binding.nilaiHadir.setText(String.valueOf(hadir));
        binding.nilaiTerlambat.setText(String.valueOf(terlambat));
        binding.nilaiAlpha.setText(String.valueOf(alpha));
        binding.nilaiIzin.setText(String.valueOf(izin));
        binding.nilaiPersenHadir.setText(persenHadir + "%");
        binding.nilaiTotalHari.setText(totalHari + " hari kerja");
        binding.progressKehadiran.setProgress(persenHadir);

        // Pesan motivasi berdasarkan persentase
        String pesan;
        if      (persenHadir >= 95) pesan = "🌟 Kehadiran sangat baik! Pertahankan!";
        else if (persenHadir >= 80) pesan = "👍 Kehadiran cukup baik.";
        else if (persenHadir >= 60) pesan = "⚠️ Kehadiran perlu ditingkatkan.";
        else if (totalHari   == 0)  pesan = "📭 Belum ada data bulan ini.";
        else                         pesan = "❌ Kehadiran rendah. Harap perhatikan!";

        binding.labelPesanMotivasi.setText(pesan);

        // Tampilkan konten, sembunyikan label kosong
        binding.layoutKonten.setVisibility(View.VISIBLE);
        binding.labelKosong.setVisibility(View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}