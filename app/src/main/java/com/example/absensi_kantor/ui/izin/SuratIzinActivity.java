package com.example.absensi_kantor.ui.izin;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.absensi_kantor.R;
import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.api.SessionManager;
import com.example.absensi_kantor.model.BaseResponse;
import com.example.absensi_kantor.model.izin.SuratIzinResponse;
import com.example.absensi_kantor.ui.izin.adapter.SuratIzinAdapter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SuratIzinActivity extends AppCompatActivity
        implements SuratIzinAdapter.OnIzinActionListener {

    // Form
    private Spinner  spinnerJenisIzin;
    private EditText etTanggalMulai, etTanggalSelesai, etKeterangan;
    private ImageView imgPreviewBukti;
    private Button   btnPilihFoto, btnKirim;
    private ProgressBar progressBar;

    // Riwayat izin sendiri
    private RecyclerView rvRiwayatIzin;
    private SuratIzinAdapter adapterSaya;

    // Semua izin (HRD) — untuk approve/tolak
    private RecyclerView rvSemuaIzin;
    private SuratIzinAdapter adapterSemua;
    private TextView tvLabelSemua;

    private SessionManager session;
    private boolean isHrd;
    private String fotoBuktiBase64 = null;
    private final Calendar calMulai   = Calendar.getInstance();
    private final Calendar calSelesai = Calendar.getInstance();

    private final String[] jenisIzinOptions = {
            "Sakit", "Izin Pribadi", "Cuti", "Duka", "Keperluan Keluarga", "Lainnya"
    };

    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surat_izin);

        ApiClient.init(this);
        session = new SessionManager(this);
        isHrd   = "hrd".equals(session.getRole()) || "admin".equals(session.getRole());

        initViews();
        setupSpinner();
        setupDatePickers();
        setupImagePicker();

        btnPilihFoto.setOnClickListener(v -> bukaGaleri());
        btnKirim.setOnClickListener(v -> validasiDanKirim());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Surat Izin");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        loadRiwayatSaya();

        // HRD juga load semua izin untuk di-approve
        if (isHrd) {
            tvLabelSemua.setVisibility(View.VISIBLE);
            rvSemuaIzin.setVisibility(View.VISIBLE);
            loadSemuaIzin();
        }
    }

    // ── INIT ─────────────────────────────────────────────────────────────

    private void initViews() {
        spinnerJenisIzin = findViewById(R.id.spinnerJenisIzin);
        etTanggalMulai   = findViewById(R.id.etTanggalMulai);
        etTanggalSelesai = findViewById(R.id.etTanggalSelesai);
        etKeterangan     = findViewById(R.id.etKeterangan);
        imgPreviewBukti  = findViewById(R.id.imgPreviewBukti);
        btnPilihFoto     = findViewById(R.id.btnPilihFoto);
        btnKirim         = findViewById(R.id.btnKirim);
        progressBar      = findViewById(R.id.progressBar);

        // RecyclerView izin sendiri
        rvRiwayatIzin = findViewById(R.id.rvRiwayatIzin);
        rvRiwayatIzin.setLayoutManager(new LinearLayoutManager(this));
        rvRiwayatIzin.setNestedScrollingEnabled(false);
        adapterSaya = new SuratIzinAdapter(new ArrayList<>(), false);
        rvRiwayatIzin.setAdapter(adapterSaya);

        // RecyclerView semua izin (HRD) — awalnya gone
        tvLabelSemua = findViewById(R.id.tvLabelSemuaIzin);
        rvSemuaIzin  = findViewById(R.id.rvSemuaIzin);
        rvSemuaIzin.setLayoutManager(new LinearLayoutManager(this));
        rvSemuaIzin.setNestedScrollingEnabled(false);
        adapterSemua = new SuratIzinAdapter(new ArrayList<>(), true); // mode HRD
        adapterSemua.setOnIzinActionListener(this);
        rvSemuaIzin.setAdapter(adapterSemua);

        tvLabelSemua.setVisibility(View.GONE);
        rvSemuaIzin.setVisibility(View.GONE);
    }

    private void setupSpinner() {
        ArrayAdapter<String> sa = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, jenisIzinOptions);
        sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerJenisIzin.setAdapter(sa);
    }

    private void setupDatePickers() {
        etTanggalMulai.setFocusable(false);
        etTanggalSelesai.setFocusable(false);

        etTanggalMulai.setOnClickListener(v ->
                new DatePickerDialog(this, (view, y, m, d) -> {
                    calMulai.set(y, m, d);
                    etTanggalMulai.setText(formatTanggal(calMulai));
                }, calMulai.get(Calendar.YEAR),
                        calMulai.get(Calendar.MONTH),
                        calMulai.get(Calendar.DAY_OF_MONTH)).show());

        etTanggalSelesai.setOnClickListener(v ->
                new DatePickerDialog(this, (view, y, m, d) -> {
                    calSelesai.set(y, m, d);
                    etTanggalSelesai.setText(formatTanggal(calSelesai));
                }, calSelesai.get(Calendar.YEAR),
                        calSelesai.get(Calendar.MONTH),
                        calSelesai.get(Calendar.DAY_OF_MONTH)).show());
    }

    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        try {
                            Bitmap bmp = MediaStore.Images.Media
                                    .getBitmap(getContentResolver(), uri);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                            fotoBuktiBase64 = Base64.encodeToString(
                                    baos.toByteArray(), Base64.NO_WRAP);
                            imgPreviewBukti.setImageBitmap(bmp);
                            imgPreviewBukti.setVisibility(View.VISIBLE);
                            btnPilihFoto.setText("Ganti Foto");
                        } catch (IOException e) {
                            Toast.makeText(this, "Gagal memuat foto",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // ── LOAD DATA ─────────────────────────────────────────────────────────

    /** Izin milik sendiri — semua role termasuk HRD */
    private void loadRiwayatSaya() {
        ApiClient.getService().getSuratIzinSaya()
                .enqueue(new Callback<SuratIzinResponse>() {
                    @Override
                    public void onResponse(Call<SuratIzinResponse> c,
                                           Response<SuratIzinResponse> r) {
                        if (r.isSuccessful() && r.body() != null
                                && r.body().getData() != null) {
                            adapterSaya.updateData(r.body().getData());
                        }
                    }
                    @Override
                    public void onFailure(Call<SuratIzinResponse> c, Throwable t) {
                        Toast.makeText(SuratIzinActivity.this,
                                "Gagal memuat riwayat", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Semua izin — hanya HRD, untuk approve/tolak */
    private void loadSemuaIzin() {
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.getService().getAllSuratIzin()
                .enqueue(new Callback<SuratIzinResponse>() {
                    @Override
                    public void onResponse(Call<SuratIzinResponse> c,
                                           Response<SuratIzinResponse> r) {
                        progressBar.setVisibility(View.GONE);
                        if (r.isSuccessful() && r.body() != null
                                && r.body().getData() != null) {
                            adapterSemua.updateData(r.body().getData());
                        }
                    }
                    @Override
                    public void onFailure(Call<SuratIzinResponse> c, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(SuratIzinActivity.this,
                                "Gagal memuat semua izin", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── AKSI FORM ─────────────────────────────────────────────────────────

    private void bukaGaleri() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void validasiDanKirim() {
        String jenisIzin      = spinnerJenisIzin.getSelectedItem().toString();
        String tanggalMulai   = etTanggalMulai.getText().toString().trim();
        String tanggalSelesai = etTanggalSelesai.getText().toString().trim();
        String keterangan     = etKeterangan.getText().toString().trim();

        if (tanggalMulai.isEmpty()) {
            etTanggalMulai.setError("Pilih tanggal mulai");
            etTanggalMulai.requestFocus(); return;
        }
        if (tanggalSelesai.isEmpty()) {
            etTanggalSelesai.setError("Pilih tanggal selesai");
            etTanggalSelesai.requestFocus(); return;
        }
        if (keterangan.isEmpty()) {
            etKeterangan.setError("Keterangan wajib diisi");
            etKeterangan.requestFocus(); return;
        }
        kirimSuratIzin(jenisIzin, tanggalMulai, tanggalSelesai, keterangan);
    }

    private void kirimSuratIzin(String jenisIzin, String tanggalMulai,
                                String tanggalSelesai, String keterangan) {
        progressBar.setVisibility(View.VISIBLE);
        btnKirim.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("jenis_izin",      jenisIzin);
        body.put("tanggal_mulai",   tanggalMulai);
        body.put("tanggal_selesai", tanggalSelesai);
        body.put("keterangan",      keterangan);
        if (fotoBuktiBase64 != null) body.put("foto_bukti", fotoBuktiBase64);

        ApiClient.getService().kirimSuratIzin(body)
                .enqueue(new Callback<SuratIzinResponse>() {
                    @Override
                    public void onResponse(Call<SuratIzinResponse> c,
                                           Response<SuratIzinResponse> r) {
                        progressBar.setVisibility(View.GONE);
                        btnKirim.setEnabled(true);
                        if (r.isSuccessful() && r.body() != null && r.body().isSukses()) {
                            Toast.makeText(SuratIzinActivity.this,
                                    "Surat izin berhasil dikirim!", Toast.LENGTH_SHORT).show();
                            resetForm();
                            loadRiwayatSaya();         // refresh izin sendiri
                            if (isHrd) loadSemuaIzin(); // refresh semua izin juga
                        } else {
                            Toast.makeText(SuratIzinActivity.this,
                                    r.body() != null ? r.body().getPesan() : "Gagal",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<SuratIzinResponse> c, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnKirim.setEnabled(true);
                        Toast.makeText(SuratIzinActivity.this,
                                "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── AKSI HRD — Setujui / Tolak ────────────────────────────────────────

    @Override
    public void onSetujui(SuratIzinResponse.Data item) {
        tampilkanDialog(item, "disetujui");
    }

    @Override
    public void onTolak(SuratIzinResponse.Data item) {
        tampilkanDialog(item, "ditolak");
    }

    private void tampilkanDialog(SuratIzinResponse.Data item, String statusBaru) {
        final EditText etCatatan = new EditText(this);
        etCatatan.setHint("Catatan untuk karyawan (opsional)");
        etCatatan.setPadding(40, 20, 40, 20);

        String judul = "disetujui".equals(statusBaru) ? "✓ Setujui Izin" : "✗ Tolak Izin";

        new AlertDialog.Builder(this)
                .setTitle(judul)
                .setMessage(item.getJenisIzin() + " - " + item.getNamaKaryawan()
                        + "\n" + item.getTanggalMulai() + " s/d " + item.getTanggalSelesai())
                .setView(etCatatan)
                .setPositiveButton(judul, (d, w) ->
                        updateStatus(item.getId(), statusBaru,
                                etCatatan.getText().toString().trim()))
                .setNegativeButton("Batal", null)
                .show();
    }

    private void updateStatus(int id, String status, String catatan) {
        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> body = new HashMap<>();
        body.put("izin_id",     id);
        body.put("status",      status);
        body.put("catatan_hrd", catatan);

        ApiClient.getService().updateStatusIzin(body)
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> c,
                                           Response<BaseResponse> r) {
                        progressBar.setVisibility(View.GONE);
                        String pesan = "disetujui".equals(status)
                                ? "Izin disetujui" : "Izin ditolak";
                        Toast.makeText(SuratIzinActivity.this,
                                pesan, Toast.LENGTH_SHORT).show();
                        loadSemuaIzin();   // refresh list semua izin
                        loadRiwayatSaya(); // refresh izin sendiri juga
                    }
                    @Override
                    public void onFailure(Call<BaseResponse> c, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(SuratIzinActivity.this,
                                "Gagal update status", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── HELPER ───────────────────────────────────────────────────────────

    private void resetForm() {
        etTanggalMulai.setText("");
        etTanggalSelesai.setText("");
        etKeterangan.setText("");
        imgPreviewBukti.setVisibility(View.GONE);
        fotoBuktiBase64 = null;
        btnPilihFoto.setText("Pilih Foto Bukti");
        spinnerJenisIzin.setSelection(0);
    }

    private String formatTanggal(Calendar cal) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}