package com.example.absensi_kantor.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.absensi_kantor.R;
import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.api.SessionManager;
import com.example.absensi_kantor.model.GajiResponse;
import com.example.absensi_kantor.model.GajiResponse.GajiData;
import com.example.absensi_kantor.model.KaryawanListResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GajiActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────
    private TextView     tvNamaKaryawan, tvJabatan, tvPeriode;
    private Spinner      spinnerBulan, spinnerTahun, spinnerKaryawan;
    private LinearLayout layoutSpinnerKaryawan;
    private Button       btnCariGaji, btnExportSlip, btnInputGaji;
    private ProgressBar  progressBar;

    private TextView tvGajiPokok, tvTunjanganTransport, tvTunjanganMakan;
    private TextView tvTunjanganJabatan, tvUangLembur, tvTotalPenghasilan;
    private TextView tvPotonganTerlambat, tvPotonganAlpha;
    private TextView tvBpjsKesehatan, tvBpjsTk, tvPph21, tvTotalPotongan;
    private TextView tvGajiBersih;
    private LinearLayout layoutDetailTerlambat;

    // ── Data ──────────────────────────────────────────────────────────
    private SessionManager sessionManager;
    private GajiData       currentGajiData;
    private List<KaryawanListResponse.Item> karyawanList = new ArrayList<>();
    private int selectedKaryawanId = -1;

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

        String role = sessionManager.getRole();
        if (role.equals("hrd") || role.equals("admin")) {
            btnInputGaji.setVisibility(View.VISIBLE);
            layoutSpinnerKaryawan.setVisibility(View.VISIBLE);
            loadKaryawanList();
        } else {
            btnInputGaji.setVisibility(View.GONE);
            layoutSpinnerKaryawan.setVisibility(View.GONE);
            selectedKaryawanId = sessionManager.getUserId();
            int bulan = Calendar.getInstance().get(Calendar.MONTH) + 1;
            int tahun = Calendar.getInstance().get(Calendar.YEAR);
            spinnerBulan.setSelection(bulan - 1);
            loadGaji(bulan, tahun);
        }
    }

    // ── Init views ────────────────────────────────────────────────────
    private void initViews() {
        tvNamaKaryawan        = findViewById(R.id.tvNamaKaryawan);
        tvJabatan             = findViewById(R.id.tvJabatan);
        tvPeriode             = findViewById(R.id.tvPeriode);
        spinnerBulan          = findViewById(R.id.spinnerBulan);
        spinnerTahun          = findViewById(R.id.spinnerTahun);
        spinnerKaryawan       = findViewById(R.id.spinnerKaryawan);
        layoutSpinnerKaryawan = findViewById(R.id.layoutSpinnerKaryawan);
        btnCariGaji           = findViewById(R.id.btnCariGaji);
        btnInputGaji          = findViewById(R.id.btnInputGaji);
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

            String role = sessionManager.getRole();
            if ((role.equals("hrd") || role.equals("admin")) && !karyawanList.isEmpty()) {
                selectedKaryawanId = karyawanList.get(spinnerKaryawan.getSelectedItemPosition()).id;
            }

            if (selectedKaryawanId <= 0) {
                Toast.makeText(this, "Pilih karyawan terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }
            loadGaji(bulan, tahun);
        });

        btnExportSlip.setOnClickListener(v -> exportSlipGaji());

        btnInputGaji.setOnClickListener(v ->
                startActivity(new Intent(GajiActivity.this, SetGajiActivity.class))
        );
    }

    // ── Load daftar karyawan untuk HRD ───────────────────────────────
    private void loadKaryawanList() {
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
                            GajiActivity.this,
                            android.R.layout.simple_spinner_dropdown_item,
                            namaList);
                    spinnerKaryawan.setAdapter(adapter);

                    if (!karyawanList.isEmpty()) {
                        selectedKaryawanId = karyawanList.get(0).id;
                        int bulan = Calendar.getInstance().get(Calendar.MONTH) + 1;
                        int tahun = Calendar.getInstance().get(Calendar.YEAR);
                        spinnerBulan.setSelection(bulan - 1);
                        loadGaji(bulan, tahun);
                    }
                } else {
                    Toast.makeText(GajiActivity.this,
                            "Gagal memuat daftar karyawan", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<KaryawanListResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(GajiActivity.this,
                        "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Load data gaji ────────────────────────────────────────────────
    private void loadGaji(int bulan, int tahun) {
        progressBar.setVisibility(View.VISIBLE);
        btnCariGaji.setEnabled(false);

        ApiClient.getService().getGaji(
                selectedKaryawanId, bulan, tahun
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
                    String pesan = "Gagal memuat data gaji";
                    if (response.body() != null && response.body().pesan != null) {
                        pesan = response.body().pesan;
                    }
                    Toast.makeText(GajiActivity.this, pesan, Toast.LENGTH_SHORT).show();
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

        // Tampilkan tombol export setelah data berhasil dimuat
        btnExportSlip.setVisibility(View.VISIBLE);
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

        GajiData d = currentGajiData;
        String namaFile = "slip_gaji_" + d.nama.replace(" ", "_")
                + "_" + d.periode.replace(" ", "_") + ".pdf";

        // Buat dokumen PDF
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // ── Paint ──
        Paint paintJudul = new Paint();
        paintJudul.setTextSize(18f);
        paintJudul.setFakeBoldText(true);
        paintJudul.setColor(Color.BLACK);

        Paint paintSubJudul = new Paint();
        paintSubJudul.setTextSize(13f);
        paintSubJudul.setFakeBoldText(true);
        paintSubJudul.setColor(Color.parseColor("#1565C0"));

        Paint paintNormal = new Paint();
        paintNormal.setTextSize(11f);
        paintNormal.setColor(Color.BLACK);

        Paint paintGaris = new Paint();
        paintGaris.setColor(Color.LTGRAY);
        paintGaris.setStrokeWidth(1f);

        Paint paintTotal = new Paint();
        paintTotal.setTextSize(11f);
        paintTotal.setFakeBoldText(true);
        paintTotal.setColor(Color.parseColor("#1565C0"));

        Paint paintBersih = new Paint();
        paintBersih.setTextSize(14f);
        paintBersih.setFakeBoldText(true);
        paintBersih.setColor(Color.WHITE);

        Paint paintBgBersih = new Paint();
        paintBgBersih.setColor(Color.parseColor("#388E3C"));

        int margin = 40;
        int y = 50;
        int col1 = margin;
        int col2 = 350;

        // ── Header ──
        canvas.drawText("SLIP GAJI KARYAWAN", margin, y, paintJudul);
        y += 6;
        canvas.drawLine(margin, y, 555, y, paintGaris);
        y += 20;

        canvas.drawText("Nama    : " + d.nama, margin, y, paintNormal);
        y += 18;
        canvas.drawText("Jabatan : " + d.jabatan, margin, y, paintNormal);
        y += 18;
        canvas.drawText("Periode : " + d.periode, margin, y, paintNormal);
        y += 10;
        canvas.drawLine(margin, y, 555, y, paintGaris);
        y += 20;

        // ── Penghasilan ──
        canvas.drawText("PENGHASILAN", margin, y, paintSubJudul);
        y += 18;
        canvas.drawText("Gaji Pokok", col1, y, paintNormal);
        canvas.drawText(formatRupiah(d.gaji_pokok), col2, y, paintNormal);
        y += 16;
        canvas.drawText("Tunjangan Transport", col1, y, paintNormal);
        canvas.drawText(formatRupiah(d.tunjangan_transport), col2, y, paintNormal);
        y += 16;
        canvas.drawText("Tunjangan Makan", col1, y, paintNormal);
        canvas.drawText(formatRupiah(d.tunjangan_makan), col2, y, paintNormal);
        y += 16;
        canvas.drawText("Tunjangan Jabatan", col1, y, paintNormal);
        canvas.drawText(formatRupiah(d.tunjangan_jabatan), col2, y, paintNormal);
        y += 16;
        canvas.drawText("Uang Lembur", col1, y, paintNormal);
        canvas.drawText(formatRupiah(d.uang_lembur), col2, y, paintNormal);
        y += 10;
        canvas.drawLine(margin, y, 555, y, paintGaris);
        y += 14;
        canvas.drawText("Total Penghasilan", col1, y, paintTotal);
        canvas.drawText(formatRupiah(d.total_penghasilan), col2, y, paintTotal);
        y += 20;

        // ── Potongan ──
        canvas.drawText("POTONGAN", margin, y, paintSubJudul);
        y += 18;
        canvas.drawText("Keterlambatan", col1, y, paintNormal);
        canvas.drawText("- " + formatRupiah(d.potongan_terlambat), col2, y, paintNormal);
        y += 16;
        canvas.drawText("Alpha (" + d.jumlah_hari_alpha + " hari)", col1, y, paintNormal);
        canvas.drawText("- " + formatRupiah(d.potongan_alpha), col2, y, paintNormal);
        y += 16;
        canvas.drawText("BPJS Kesehatan (1%)", col1, y, paintNormal);
        canvas.drawText("- " + formatRupiah(d.bpjs_kesehatan), col2, y, paintNormal);
        y += 16;
        canvas.drawText("BPJS Ketenagakerjaan (2%)", col1, y, paintNormal);
        canvas.drawText("- " + formatRupiah(d.bpjs_tk), col2, y, paintNormal);
        y += 16;
        canvas.drawText("PPh 21 (5%)", col1, y, paintNormal);
        canvas.drawText("- " + formatRupiah(d.pph21), col2, y, paintNormal);
        y += 10;
        canvas.drawLine(margin, y, 555, y, paintGaris);
        y += 14;
        canvas.drawText("Total Potongan", col1, y, paintTotal);
        canvas.drawText("- " + formatRupiah(d.total_potongan), col2, y, paintTotal);
        y += 24;

        // ── Gaji Bersih ──
        canvas.drawRect(margin, y - 18, 555, y + 10, paintBgBersih);
        canvas.drawText("GAJI BERSIH", margin + 10, y, paintBersih);
        canvas.drawText(formatRupiah(d.gaji_bersih), col2, y, paintBersih);

        pdfDocument.finishPage(page);

        // ── Simpan file ──
        try {
            OutputStream outputStream;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ pakai MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, namaFile);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                outputStream = getContentResolver().openOutputStream(uri);
            } else {
                // Android 9 ke bawah
                File folder = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);
                File file = new File(folder, namaFile);
                outputStream = new FileOutputStream(file);
            }

            pdfDocument.writeTo(outputStream);
            outputStream.close();
            pdfDocument.close();

            Toast.makeText(this, "Slip disimpan di Downloads:\n" + namaFile, Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            pdfDocument.close();
            Toast.makeText(this, "Gagal menyimpan PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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