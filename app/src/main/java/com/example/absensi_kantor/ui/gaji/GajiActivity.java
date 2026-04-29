package com.example.absensi_kantor.ui.gaji;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.absensi_kantor.R;
import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.api.ApiService;
import com.example.absensi_kantor.api.SessionManager;
import com.example.absensi_kantor.model.gaji.GajiResponse;
import com.example.absensi_kantor.model.gaji.GajiResponse.GajiData;
import com.example.absensi_kantor.model.laporan.KaryawanListResponse;

import java.text.NumberFormat;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GajiActivity extends AppCompatActivity {

    private ApiService api;
    private SessionManager session;

    private Spinner spinnerBulan, spinnerTahun, spinnerKaryawan;
    private LinearLayout layoutSpinnerKaryawan;
    private Button btnCariGaji, btnInputGaji;
    private ProgressBar progressBar;

    private TextView tvNamaKaryawan, tvJabatan, tvPeriode;
    private TextView tvGajiPokok, tvTunjanganTransport, tvTunjanganMakan;
    private TextView tvTunjanganJabatan, tvUangLembur, tvTotalPenghasilan;
    private TextView tvPotonganTerlambat, tvPotonganAlpha;
    private TextView tvBpjsKesehatan, tvBpjsTk, tvPph21, tvTotalPotongan;
    private TextView tvGajiBersih;

    private List<KaryawanListResponse.Item> karyawanList = new ArrayList<>();
    private int selectedKaryawanId = -1;

    private final String[] BULAN = {
            "Januari","Februari","Maret","April","Mei","Juni",
            "Juli","Agustus","September","Oktober","November","Desember"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gaji);

        ApiClient.init(this);
        api = ApiClient.getService();
        session = new SessionManager(this);

        initView();
        setupSpinner();
        setupListener();

        String role = session.getRole();

        if (role.equals("admin") || role.equals("hrd")) {
            layoutSpinnerKaryawan.setVisibility(View.VISIBLE);
            btnInputGaji.setVisibility(View.VISIBLE);
            loadKaryawanList();
        } else {
            layoutSpinnerKaryawan.setVisibility(View.GONE);
            btnInputGaji.setVisibility(View.GONE);

            selectedKaryawanId = session.getUserId();

            int bulan = Calendar.getInstance().get(Calendar.MONTH) + 1;
            int tahun = Calendar.getInstance().get(Calendar.YEAR);

            spinnerBulan.setSelection(bulan - 1);
            loadGaji(bulan, tahun);
        }
    }

    private void initView() {
        spinnerBulan = findViewById(R.id.spinnerBulan);
        spinnerTahun = findViewById(R.id.spinnerTahun);
        spinnerKaryawan = findViewById(R.id.spinnerKaryawan);

        layoutSpinnerKaryawan = findViewById(R.id.layoutSpinnerKaryawan);
        btnCariGaji = findViewById(R.id.btnCariGaji);
        btnInputGaji = findViewById(R.id.btnInputGaji);
        progressBar = findViewById(R.id.progressBar);

        tvNamaKaryawan = findViewById(R.id.tvNamaKaryawan);
        tvJabatan = findViewById(R.id.tvJabatan);
        tvPeriode = findViewById(R.id.tvPeriode);

        tvGajiPokok = findViewById(R.id.tvGajiPokok);
        tvTunjanganTransport = findViewById(R.id.tvTunjanganTransport);
        tvTunjanganMakan = findViewById(R.id.tvTunjanganMakan);
        tvTunjanganJabatan = findViewById(R.id.tvTunjanganJabatan);
        tvUangLembur = findViewById(R.id.tvUangLembur);
        tvTotalPenghasilan = findViewById(R.id.tvTotalPenghasilan);

        tvPotonganTerlambat = findViewById(R.id.tvPotonganTerlambat);
        tvPotonganAlpha = findViewById(R.id.tvPotonganAlpha);
        tvBpjsKesehatan = findViewById(R.id.tvBpjsKesehatan);
        tvBpjsTk = findViewById(R.id.tvBpjsTk);
        tvPph21 = findViewById(R.id.tvPph21);
        tvTotalPotongan = findViewById(R.id.tvTotalPotongan);

        tvGajiBersih = findViewById(R.id.tvGajiBersih);
    }

    private void setupSpinner() {
        spinnerBulan.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, BULAN));

        int tahunNow = Calendar.getInstance().get(Calendar.YEAR);
        String[] tahunList = {String.valueOf(tahunNow), String.valueOf(tahunNow - 1)};
        spinnerTahun.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tahunList));
    }

    private void setupListener() {
        btnCariGaji.setOnClickListener(v -> {
            int bulan = spinnerBulan.getSelectedItemPosition() + 1;
            int tahun = Integer.parseInt(spinnerTahun.getSelectedItem().toString());

            if (selectedKaryawanId <= 0) {
                Toast.makeText(this, "Pilih karyawan dulu", Toast.LENGTH_SHORT).show();
                return;
            }

            loadGaji(bulan, tahun);
        });

        btnInputGaji.setOnClickListener(v ->
                startActivity(new Intent(this, SetGajiActivity.class))
        );
    }

    private void loadKaryawanList() {
        progressBar.setVisibility(View.VISIBLE);

        api.getKaryawanList().enqueue(new Callback<KaryawanListResponse>() {
            @Override
            public void onResponse(Call<KaryawanListResponse> call, Response<KaryawanListResponse> response) {
                progressBar.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null) return;

                karyawanList = response.body().data;

                List<String> listNama = new ArrayList<>();
                for (KaryawanListResponse.Item k : karyawanList) {
                    listNama.add(k.nama + " - " + k.jabatan);
                }

                spinnerKaryawan.setAdapter(new ArrayAdapter<>(GajiActivity.this,
                        android.R.layout.simple_spinner_item, listNama));

                if (!karyawanList.isEmpty()) {
                    selectedKaryawanId = karyawanList.get(0).id;

                    int bulan = Calendar.getInstance().get(Calendar.MONTH) + 1;
                    int tahun = Calendar.getInstance().get(Calendar.YEAR);

                    loadGaji(bulan, tahun);
                }
            }

            @Override
            public void onFailure(Call<KaryawanListResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(GajiActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadGaji(int bulan, int tahun) {
        progressBar.setVisibility(View.VISIBLE);

        api.getGaji(selectedKaryawanId, bulan, tahun)
                .enqueue(new Callback<GajiResponse>() {
                    @Override
                    public void onResponse(Call<GajiResponse> call, Response<GajiResponse> response) {
                        progressBar.setVisibility(View.GONE);

                        if (!response.isSuccessful() || response.body() == null) return;

                        GajiData d = response.body().data;

                        // 🔥 SIMPAN KE SESSION (INI YANG MASUK KE PROFILE)
                        session.simpanDataGaji(
                                d.gaji_pokok,
                                d.tunjangan_transport,
                                d.tunjangan_makan,
                                d.tunjangan_jabatan,
                                d.potongan_terlambat,
                                d.potongan_alpha
                        );

                        tampilkan(d);
                    }

                    @Override
                    public void onFailure(Call<GajiResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(GajiActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void tampilkan(GajiData d) {
        tvNamaKaryawan.setText(d.nama);
        tvJabatan.setText(d.jabatan);
        tvPeriode.setText("Periode: " + d.periode);

        tvGajiPokok.setText(format(d.gaji_pokok));
        tvTotalPenghasilan.setText(format(d.total_penghasilan));
        tvGajiBersih.setText(format(d.gaji_bersih));
    }

    private String format(double val) {
        return "Rp " + NumberFormat.getNumberInstance(new Locale("id", "ID"))
                .format((long) val);
    }
}