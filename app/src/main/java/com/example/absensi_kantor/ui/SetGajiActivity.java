package com.example.absensi_kantor.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.absensi_kantor.R;
import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.api.SessionManager;
import com.example.absensi_kantor.model.BaseResponse;
import com.example.absensi_kantor.model.KaryawanListResponse;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SetGajiActivity extends AppCompatActivity {

    private Spinner     spinnerKaryawan, spinnerBulan, spinnerTahun;
    private EditText    etGajiPokok, etTunjanganTransport, etTunjanganMakan, etTunjanganJabatan;
    private Button      btnSimpanGaji;
    private ProgressBar progressBar;

    private List<KaryawanListResponse.Item> karyawanList = new ArrayList<>();
    private SessionManager sessionManager;

    private final String[] NAMA_BULAN = {
            "Januari","Februari","Maret","April","Mei","Juni",
            "Juli","Agustus","September","Oktober","November","Desember"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_gaji);

        ApiClient.init(this);
        sessionManager = new SessionManager(this);

        // Cek role — hanya HRD/admin
        String role = sessionManager.getRole();
        if (!role.equals("hrd") && !role.equals("admin")) {
            Toast.makeText(this, "Akses ditolak", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupSpinnerBulan();
        setupSpinnerTahun();
        loadKaryawan();

        btnSimpanGaji.setOnClickListener(v -> simpanGaji());
    }

    private void initViews() {
        spinnerKaryawan      = findViewById(R.id.spinnerKaryawan);
        spinnerBulan         = findViewById(R.id.spinnerBulan);
        spinnerTahun         = findViewById(R.id.spinnerTahun);
        etGajiPokok          = findViewById(R.id.etGajiPokok);
        etTunjanganTransport = findViewById(R.id.etTunjanganTransport);
        etTunjanganMakan     = findViewById(R.id.etTunjanganMakan);
        etTunjanganJabatan   = findViewById(R.id.etTunjanganJabatan);
        btnSimpanGaji        = findViewById(R.id.btnSimpanGaji);
        progressBar          = findViewById(R.id.progressBar);
    }

    private void setupSpinnerBulan() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, NAMA_BULAN);
        spinnerBulan.setAdapter(adapter);
        spinnerBulan.setSelection(Calendar.getInstance().get(Calendar.MONTH));
    }

    private void setupSpinnerTahun() {
        int tahunSekarang = Calendar.getInstance().get(Calendar.YEAR);
        String[] tahunList = {
                String.valueOf(tahunSekarang),
                String.valueOf(tahunSekarang - 1),
                String.valueOf(tahunSekarang + 1)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, tahunList);
        spinnerTahun.setAdapter(adapter);
        spinnerTahun.setSelection(0); // default tahun sekarang
    }

    private void loadKaryawan() {
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.getService().getKaryawanList().enqueue(new Callback<KaryawanListResponse>() {
            @Override
            public void onResponse(Call<KaryawanListResponse> call,
                                   Response<KaryawanListResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null
                        && response.body().sukses) {
                    karyawanList = response.body().data;
                    List<String> namaList = new ArrayList<>();
                    for (KaryawanListResponse.Item item : karyawanList) {
                        namaList.add(item.nama + " — " + item.jabatan);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            SetGajiActivity.this,
                            android.R.layout.simple_spinner_dropdown_item,
                            namaList);
                    spinnerKaryawan.setAdapter(adapter);
                } else {
                    Toast.makeText(SetGajiActivity.this,
                            "Gagal memuat daftar karyawan", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<KaryawanListResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SetGajiActivity.this,
                        "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void simpanGaji() {
        if (karyawanList == null || karyawanList.isEmpty()) {
            Toast.makeText(this, "Data karyawan belum dimuat", Toast.LENGTH_SHORT).show();
            return;
        }

        String strGajiPokok   = etGajiPokok.getText().toString().trim();
        String strTransport   = etTunjanganTransport.getText().toString().trim();
        String strMakan       = etTunjanganMakan.getText().toString().trim();
        String strJabatan     = etTunjanganJabatan.getText().toString().trim();

        if (strGajiPokok.isEmpty()) {
            etGajiPokok.setError("Gaji pokok wajib diisi");
            etGajiPokok.requestFocus();
            return;
        }

        int selectedIdx  = spinnerKaryawan.getSelectedItemPosition();
        int karyawanId   = karyawanList.get(selectedIdx).id;
        int bulan        = spinnerBulan.getSelectedItemPosition() + 1;
        int tahun        = Integer.parseInt(spinnerTahun.getSelectedItem().toString());

        progressBar.setVisibility(View.VISIBLE);
        btnSimpanGaji.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("karyawan_id",         karyawanId);
        body.put("gaji_pokok",          Long.parseLong(strGajiPokok));
        body.put("tunjangan_transport", strTransport.isEmpty() ? 0 : Long.parseLong(strTransport));
        body.put("tunjangan_makan",     strMakan.isEmpty()     ? 0 : Long.parseLong(strMakan));
        body.put("tunjangan_jabatan",   strJabatan.isEmpty()   ? 0 : Long.parseLong(strJabatan));
        body.put("bulan",               bulan);
        body.put("tahun",               tahun);

        ApiClient.getService().setGaji(body).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnSimpanGaji.setEnabled(true);
                if (response.isSuccessful() && response.body() != null
                        && response.body().sukses) {
                    Toast.makeText(SetGajiActivity.this,
                            "Gaji berhasil disimpan!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(SetGajiActivity.this,
                            "Gagal menyimpan gaji", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSimpanGaji.setEnabled(true);
                Toast.makeText(SetGajiActivity.this,
                        "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}