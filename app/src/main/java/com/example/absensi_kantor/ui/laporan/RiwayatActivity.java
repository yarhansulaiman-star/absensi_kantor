package com.example.absensi_kantor.ui.laporan;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.example.absensi_kantor.R;
import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.model.absen.RiwayatResponse;
import com.example.absensi_kantor.ui.MainActivity;
import com.example.absensi_kantor.ui.auth.ProfilActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RiwayatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar  progressBar;
    private TextView     labelKosong, labelNama;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riwayat);

        ApiClient.init(this);

        recyclerView = findViewById(R.id.recyclerRiwayat);
        progressBar  = findViewById(R.id.progressBar);
        labelKosong  = findViewById(R.id.labelKosong);
        labelNama    = findViewById(R.id.labelNama);

        // ── Sesuaikan posisi judul dengan status bar HP nyata ──────────
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content), (v, insets) -> {
                    int statusBarHeight = insets.getInsets(
                            WindowInsetsCompat.Type.statusBars()).top;
                    findViewById(R.id.textJudul).setPadding(0, statusBarHeight + 16, 0, 0);
                    return insets;
                }
        );

        muatRiwayat();

        // ── Bottom Navigation ───────────────────────────────────────
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_riwayat);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_riwayat) {
                return true;
            } else if (id == R.id.nav_laporan) {
                startActivity(new Intent(this, LaporanActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profil) {
                startActivity(new Intent(this, ProfilActivity.class));
                finish();
                return true;
            }

            return false;
        });
    }

    private void muatRiwayat() {
        progressBar.setVisibility(View.VISIBLE);

        ApiClient.getService().riwayat()
                .enqueue(new Callback<RiwayatResponse>() {
                    @Override
                    public void onResponse(Call<RiwayatResponse> call,
                                           Response<RiwayatResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            RiwayatResponse data = response.body();
                            labelNama.setText("Riwayat: " + data.nama);
                            if (data.data != null && !data.data.isEmpty()) {
                                recyclerView.setLayoutManager(
                                        new LinearLayoutManager(RiwayatActivity.this));
                                recyclerView.setAdapter(new RiwayatAdapter(data.data));
                                recyclerView.setVisibility(View.VISIBLE);
                            } else {
                                labelKosong.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<RiwayatResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(RiwayatActivity.this,
                                "Gagal memuat riwayat!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Adapter ───────────────────────────────────────────────────────────────
    static class RiwayatAdapter extends RecyclerView.Adapter<RiwayatAdapter.VH> {
        private final List<RiwayatResponse.DataRiwayat> data;

        RiwayatAdapter(List<RiwayatResponse.DataRiwayat> data) { this.data = data; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_riwayat, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            RiwayatResponse.DataRiwayat item = data.get(pos);
            h.tanggal.setText(" " + item.tanggal);
            h.masuk.setText("Masuk : " + (item.jam_masuk  != null ? item.jam_masuk  : "-"));
            h.keluar.setText("Keluar: " + (item.jam_keluar != null ? item.jam_keluar : "-"));

            String emoji = "tepat_waktu".equals(item.status) ? "✅" : "⚠️";
            String label = "tepat_waktu".equals(item.status) ? "Tepat Waktu" : "Terlambat";
            h.status.setText(emoji + " " + label);
            h.status.setTextColor("tepat_waktu".equals(item.status)
                    ? 0xFF388E3C : 0xFFF57C00);
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tanggal, masuk, keluar, status;
            VH(View v) {
                super(v);
                tanggal = v.findViewById(R.id.textTanggal);
                masuk   = v.findViewById(R.id.textMasuk);
                keluar  = v.findViewById(R.id.textKeluar);
                status  = v.findViewById(R.id.textStatus);
            }
        }
    }
}