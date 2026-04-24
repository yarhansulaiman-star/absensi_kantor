package com.example.absensi_kantor.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.absensi_kantor.R;
import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.api.ApiService;
import com.example.absensi_kantor.api.SessionManager;
import com.example.absensi_kantor.model.GajiResponse;
import com.example.absensi_kantor.model.GajiResponse.GajiData;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GajiActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────
    private TextView    tvNamaKaryawan, tvJabatan, tvPeriode;
    private Spinner     spinnerBulan, spinnerTahun;
    private Button      btnCariGaji, btnExportSlip, btnInputGaji;  // ← tambah btnInputGaji
    private ProgressBar progressBar;

    private TextView tvGajiPokok, tvTunjanganTransport, tvTunjanganMakan;
    private TextView tvTunjanganJabatan, tvUangLembur, tvTotalPenghasilan;
    private TextView tvPotonganTerlambat, tvPotonganAlpha;
    private TextView tvBpjsKesehatan, tvBpjsTk, tvPph21, tvTotalPotongan;
    private TextView tvGajiBersih;
    private LinearLayout layoutDetailTerlambat;

    // ── Data ──────────────────────────────────────────────────────────
    private SessionManager sessionManager;
    private GajiData       currentGajiData;

    private final String[] NAMA_BULAN = {
            "Januari","Februari","Maret","April","Mei","Juni",
            "Juli","Agustus","September","Oktober","November","Desember"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gaji);

        ApiClient.init(this);
        sessionManager = new SessionManager(this);

        initViews();
        setupSpinner();
        setupListeners();

        // Tampilkan tombol Input Gaji hanya untuk HRD/admin
        String role = sessionManager.getRole();
        if (role.equals("hrd") || role.equals("admin")) {
            btnInputGaji.setVisibility(View.VISIBLE);
        } else {
            btnInputGaji.setVisibility(View.GONE);
        }

        int bulan = Calendar.getInstance().get(Calendar.MONTH) + 1;
        int tahun = Calendar.getInstance().get(Calendar.YEAR);
        spinnerBulan.setSelection(bulan - 1);
        loadGaji(bulan, tahun);
    }

    // ── Init views ────────────────────────────────────────────────────
    private void initViews() {
        tvNamaKaryawan        = findViewById(R.id.tvNamaKaryawan);
        tvJabatan             = findViewById(R.id.tvJabatan);
        tvPeriode             = findViewById(R.id.tvPeriode);
        spinnerBulan          = findViewById(R.id.spinnerBulan);
        spinnerTahun          = findViewById(R.id.spinnerTahun);
        btnCariGaji           = findViewById(R.id.btnCariGaji);
        btnInputGaji          = findViewById(R.id.btnInputGaji);   // ← tambah
        progressBar           = findViewById(R.id.progressBar);
        tvGajiPokok           = findViewById(R.id.tvGajiPokok);
        tvTunjanganTransport  = findViewById(R.id.tvTunjanganTransport);
        tvTunjanganMakan      = findViewById(R.id.tvTunjanganMakan);
        tvTunjanganJabatan    = findViewById(R.id.tvTunjanganJabatan);
        tvUangLembur          = findViewById(R.id.tvUangLembur);
        tvTotalPenghasilan    = findViewById(R.id.tvTotalPenghasilan);
        tvPotonganTerlambat   = findViewById(R.id.tvPotonganTerlambat);
        tvPotonganAlpha       = findViewById(R.id.tvPotonganAlpha);
        tvBpjsKesehatan       = findViewById(R.id.tvBpjsKesehatan);
        tvBpjsTk              = findViewById(R.id.tvBpjsTk);
        tvPph21               = findViewById(R.id.tvPph21);
        tvTotalPotongan       = findViewById(R.id.tvTotalPotongan);
        tvGajiBersih          = findViewById(R.id.tvGajiBersih);
        btnExportSlip         = findViewById(R.id.btnExportSlip);
        layoutDetailTerlambat = findViewById(R.id.layoutDetailTerlambat);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapterBulan = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, NAMA_BULAN);
        adapterBulan.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBulan.setAdapter(adapterBulan);

        int tahunSekarang = Calendar.getInstance().get(Calendar.YEAR);
        String[] tahunList = {
                String.valueOf(tahunSekarang),
                String.valueOf(tahunSekarang - 1),
                String.valueOf(tahunSekarang - 2)
        };
        ArrayAdapter<String> adapterTahun = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, tahunList);
        adapterTahun.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTahun.setAdapter(adapterTahun);
    }

    private void setupListeners() {
        btnCariGaji.setOnClickListener(v -> {
            int bulan = spinnerBulan.getSelectedItemPosition() + 1;
            int tahun = Integer.parseInt(spinnerTahun.getSelectedItem().toString());
            loadGaji(bulan, tahun);
        });

        btnExportSlip.setOnClickListener(v -> exportSlipGaji());

        // ← tambah listener Input Gaji
        btnInputGaji.setOnClickListener(v ->
                startActivity(new Intent(GajiActivity.this, SetGajiActivity.class))
        );
    }

    // ── Load data ─────────────────────────────────────────────────────
    private void loadGaji(int bulan, int tahun) {
        progressBar.setVisibility(View.VISIBLE);
        btnCariGaji.setEnabled(false);

        ApiClient.getService().getGaji(
                sessionManager.getUserId(), bulan, tahun,
                "Bearer " + sessionManager.getToken()
        ).enqueue(new Callback<GajiResponse>() {

            @Override
            public void onResponse(Call<GajiResponse> call, Response<GajiResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnCariGaji.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && response.body().sukses) {

                    if (response.body().data == null) {
                        Toast.makeText(GajiActivity.this,
                                "Data kosong", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    currentGajiData = response.body().data;
                    tampilkanGaji(currentGajiData);

                } else {
                    Toast.makeText(GajiActivity.this,
                            "Gagal memuat data gaji", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GajiResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnCariGaji.setEnabled(true);
                Toast.makeText(GajiActivity.this,
                        "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Tampilkan data ────────────────────────────────────────────────
    private void tampilkanGaji(GajiData d) {
        tvNamaKaryawan.setText(d.nama);
        tvJabatan.setText(d.jabatan);
        tvPeriode.setText("Periode: " + d.periode);

        tvGajiPokok.setText(formatRupiah(d.gaji_pokok));
        tvTunjanganTransport.setText(formatRupiah(d.tunjangan_transport));
        tvTunjanganMakan.setText(formatRupiah(d.tunjangan_makan));
        tvTunjanganJabatan.setText(formatRupiah(d.tunjangan_jabatan));
        tvUangLembur.setText(formatRupiah(d.uang_lembur));
        tvTotalPenghasilan.setText(formatRupiah(d.total_penghasilan));

        tvPotonganTerlambat.setText("- " + formatRupiah(d.potongan_terlambat));
        tvPotonganAlpha.setText("- " + formatRupiah(d.potongan_alpha)
                + " (" + d.jumlah_hari_alpha + " hari)");
        tvBpjsKesehatan.setText("- " + formatRupiah(d.bpjs_kesehatan));
        tvBpjsTk.setText("- " + formatRupiah(d.bpjs_tk));
        tvPph21.setText("- " + formatRupiah(d.pph21));
        tvTotalPotongan.setText("- " + formatRupiah(d.total_potongan));
        tvGajiBersih.setText(formatRupiah(d.gaji_bersih));

        tampilkanDetailTerlambat(d);
    }

    private void tampilkanDetailTerlambat(GajiData d) {
        layoutDetailTerlambat.removeAllViews();
        if (d.detail_terlambat == null || d.detail_terlambat.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Tidak ada keterlambatan bulan ini");
            tv.setPadding(0, 8, 0, 8);
            layoutDetailTerlambat.addView(tv);
            return;
        }
        for (GajiResponse.DetailTerlambat item : d.detail_terlambat) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 6, 0, 6);
            row.addView(buatTextView(item.tanggal, 1));
            row.addView(buatTextView(item.jam_masuk, 1));
            row.addView(buatTextView(item.menit_terlambat + " mnt", 1));
            row.addView(buatTextView("- " + formatRupiah(item.potongan), 1));
            layoutDetailTerlambat.addView(row);
        }
    }

    // ── Export slip ───────────────────────────────────────────────────
    private void exportSlipGaji() {
        if (currentGajiData == null) {
            Toast.makeText(this, "Data gaji belum dimuat", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("SLIP GAJI\n=================================\n");
        sb.append("Nama    : ").append(currentGajiData.nama).append("\n");
        sb.append("Jabatan : ").append(currentGajiData.jabatan).append("\n");
        sb.append("Periode : ").append(currentGajiData.periode).append("\n");
        sb.append("=================================\n\nPENGHASILAN\n");
        sb.append("Gaji Pokok          : ").append(formatRupiah(currentGajiData.gaji_pokok)).append("\n");
        sb.append("Tunjangan Transport : ").append(formatRupiah(currentGajiData.tunjangan_transport)).append("\n");
        sb.append("Tunjangan Makan     : ").append(formatRupiah(currentGajiData.tunjangan_makan)).append("\n");
        sb.append("Tunjangan Jabatan   : ").append(formatRupiah(currentGajiData.tunjangan_jabatan)).append("\n");
        sb.append("Uang Lembur         : ").append(formatRupiah(currentGajiData.uang_lembur)).append("\n");
        sb.append("Total Penghasilan   : ").append(formatRupiah(currentGajiData.total_penghasilan)).append("\n\n");
        sb.append("POTONGAN\n");
        sb.append("Keterlambatan       : ").append(formatRupiah(currentGajiData.potongan_terlambat)).append("\n");
        sb.append("Alpha               : ").append(formatRupiah(currentGajiData.potongan_alpha)).append("\n");
        sb.append("BPJS Kesehatan      : ").append(formatRupiah(currentGajiData.bpjs_kesehatan)).append("\n");
        sb.append("BPJS TK             : ").append(formatRupiah(currentGajiData.bpjs_tk)).append("\n");
        sb.append("PPh 21              : ").append(formatRupiah(currentGajiData.pph21)).append("\n");
        sb.append("Total Potongan      : ").append(formatRupiah(currentGajiData.total_potongan)).append("\n\n");
        sb.append("=================================\n");
        sb.append("GAJI BERSIH         : ").append(formatRupiah(currentGajiData.gaji_bersih)).append("\n");
        sb.append("=================================\n");

        Toast.makeText(this, "Slip gaji berhasil diexport!", Toast.LENGTH_SHORT).show();
    }

    // ── Helper ────────────────────────────────────────────────────────
    private String formatRupiah(long nominal) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("id", "ID"));
        return "Rp " + nf.format(nominal);
    }

    private TextView buatTextView(String text, int weight) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(12f);
        tv.setPadding(0, 0, 8, 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
        tv.setLayoutParams(params);
        return tv;
    }
}