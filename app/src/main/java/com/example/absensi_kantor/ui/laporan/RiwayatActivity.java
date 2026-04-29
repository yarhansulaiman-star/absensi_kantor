package com.example.absensi_kantor.ui.laporan;

import androidx.appcompat.app.AppCompatActivity;
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

        // ✅ Init ApiClient agar token otomatis diinject
        ApiClient.init(this);

        recyclerView = findViewById(R.id.recyclerRiwayat);
        progressBar  = findViewById(R.id.progressBar);
        labelKosong  = findViewById(R.id.labelKosong);
        labelNama    = findViewById(R.id.labelNama);

        findViewById(R.id.tombolKembali).setOnClickListener(v -> finish());
        muatRiwayat();
    }

    private void muatRiwayat() {
        progressBar.setVisibility(View.VISIBLE);

        // ✅ Hapus session.getToken() — token sudah otomatis dari interceptor
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
            h.tanggal.setText("📅 " + item.tanggal);
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