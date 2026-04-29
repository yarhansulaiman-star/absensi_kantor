package com.example.absensi_kantor.ui.izin;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.absensi_kantor.R;
import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.model.BaseResponse;
import com.example.absensi_kantor.model.izin.SuratIzinResponse;
import com.example.absensi_kantor.ui.izin.adapter.SuratIzinAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApproveSuratIzinActivity extends AppCompatActivity
        implements SuratIzinAdapter.OnIzinActionListener {

    private RecyclerView rvSuratIzin;
    private ProgressBar progressBar;
    private TextView    tvEmpty;
    private SuratIzinAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approve_surat_izin);

        rvSuratIzin = findViewById(R.id.rvSuratIzin);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty     = findViewById(R.id.tvEmpty);

        rvSuratIzin.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SuratIzinAdapter(new ArrayList<>(), true); // true = mode HRD
        adapter.setOnIzinActionListener(this);
        rvSuratIzin.setAdapter(adapter);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Kelola Surat Izin");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        loadSemuaIzin();
    }

    // ── LOAD DATA ────────────────────────────────────────────────────────

    private void loadSemuaIzin() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        ApiClient.getService().getAllSuratIzin()
                .enqueue(new Callback<SuratIzinResponse>() {
                    @Override
                    public void onResponse(Call<SuratIzinResponse> call,
                                           Response<SuratIzinResponse> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null
                                && response.body().getData() != null) {
                            adapter.updateData(response.body().getData());
                            tvEmpty.setVisibility(
                                    response.body().getData().isEmpty()
                                            ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<SuratIzinResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ApproveSuratIzinActivity.this,
                                "Gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── AKSI HRD ─────────────────────────────────────────────────────────

    @Override
    public void onSetujui(SuratIzinResponse.Data item) {
        tampilkanDialog(item, "disetujui");
    }

    @Override
    public void onTolak(SuratIzinResponse.Data item) {
        tampilkanDialog(item, "ditolak");
    }

    private void tampilkanDialog(SuratIzinResponse.Data item, String statusBaru) {
        // Input catatan HRD
        final EditText etCatatan = new EditText(this);
        etCatatan.setHint("Catatan untuk karyawan (opsional)");
        etCatatan.setPadding(40, 20, 40, 20);

        String judul = "disetujui".equals(statusBaru) ? "✓ Setujui Izin" : "✗ Tolak Izin";

        new AlertDialog.Builder(this)
                .setTitle(judul)
                .setMessage(item.getJenisIzin() + " - " + item.getNamaKaryawan()
                        + "\n" + item.getTanggalMulai() + " s/d " + item.getTanggalSelesai())
                .setView(etCatatan)
                .setPositiveButton(judul, (d, w) -> {
                    String catatan = etCatatan.getText().toString().trim();
                    updateStatus(item.getId(), statusBaru, catatan);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void updateStatus(int id, String status, String catatan) {
        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> body = new HashMap<>();
        body.put("id",          id);
        body.put("status",      status);
        body.put("catatan_hrd", catatan);

        ApiClient.getService().updateStatusIzin(body)
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> call,
                                           Response<BaseResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            String pesan = "disetujui".equals(status)
                                    ? "Izin berhasil disetujui"
                                    : "Izin berhasil ditolak";
                            Toast.makeText(ApproveSuratIzinActivity.this,
                                    pesan, Toast.LENGTH_SHORT).show();
                            loadSemuaIzin(); // refresh list
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ApproveSuratIzinActivity.this,
                                "Gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}