package com.example.absensi_kantor.ui.auth;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import com.example.absensi_kantor.R;
import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.api.ApiService;
import com.example.absensi_kantor.api.SessionManager;
import com.example.absensi_kantor.model.absen.RiwayatResponse;
import com.example.absensi_kantor.model.gaji.GajiResponse;
import com.example.absensi_kantor.ui.MainActivity;
import com.example.absensi_kantor.ui.laporan.LaporanActivity;
import com.example.absensi_kantor.ui.laporan.RiwayatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.Calendar;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfilActivity extends AppCompatActivity {

    private static final String TAG = "ProfilActivity";

    private SessionManager session;
    private ApiService     api;

    private TextView txtAvatar, txtUsername, txtRole, txtUserId;
    private TextView txtStatHadir, txtStatTerlambat, txtStatIzin;
    private TextView txtGajiPokok, txtTunjangan, txtTotalPenghasilan;
    private SwitchCompat switchDarkMode;
    private TextView txtVersi;
    private Button btnTentang, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);

        if (session.isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        setContentView(R.layout.activity_profil);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Profil");
        }

        ApiClient.init(this);
        api = ApiClient.getService();

        initViews();
        loadAkun();
        loadGaji();
        loadVersi();
        loadStatistik();
        setupListeners();

        // ── Bottom Navigation ───────────────────────────────────────
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_profil);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_riwayat) {
                startActivity(new Intent(this, RiwayatActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_laporan) {
                startActivity(new Intent(this, LaporanActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profil) {
                return true; // sudah di halaman ini
            }

            return false;
        });
    }

    private void initViews() {
        txtAvatar   = findViewById(R.id.txtAvatar);
        txtUsername = findViewById(R.id.txtUsername);
        txtRole     = findViewById(R.id.txtRole);
        txtUserId   = findViewById(R.id.txtUserId);

        txtStatHadir     = findViewById(R.id.txtStatHadir);
        txtStatTerlambat = findViewById(R.id.txtStatTerlambat);
        txtStatIzin      = findViewById(R.id.txtStatIzin);

        txtGajiPokok        = findViewById(R.id.txtGajiPokok);
        txtTunjangan        = findViewById(R.id.txtTunjangan);
        txtTotalPenghasilan = findViewById(R.id.txtTotalPenghasilan);

        switchDarkMode = findViewById(R.id.switchDarkMode);
        txtVersi       = findViewById(R.id.txtVersi);

        btnTentang = findViewById(R.id.btnTentang);
        btnLogout  = findViewById(R.id.btnLogout);
    }

    private void loadAkun() {
        String username = session.getUsername();
        String inisial  = (username != null && !username.isEmpty())
                ? String.valueOf(username.charAt(0)).toUpperCase()
                : "?";

        txtAvatar.setText(inisial);
        txtUsername.setText(username);
        txtRole.setText(session.getRole().toUpperCase());
        txtUserId.setText("ID: " + session.getUserId());

        switchDarkMode.setChecked(session.isDarkMode());
    }

    private void loadGaji() {
        txtGajiPokok.setText("Gaji Pokok          : Rp " + formatRupiah(session.getGajiPokok()));
        txtTunjangan.setText("Total Tunjangan  : Rp " + formatRupiah(session.getTotalTunjangan()));
        txtTotalPenghasilan.setText("Total Penghasilan : Rp " + formatRupiah(session.getTotalPenghasilan()));

        Calendar cal   = Calendar.getInstance();
        int bulan      = cal.get(Calendar.MONTH) + 1;
        int tahun      = cal.get(Calendar.YEAR);
        int karyawanId = session.getKaryawanId();

        Log.d(TAG, "loadGaji → karyawanId=" + karyawanId
                + ", bulan=" + bulan + ", tahun=" + tahun);

        if (karyawanId == 0) {
            Log.e(TAG, "❌ karyawanId=0, belum login ulang setelah update!");
            return;
        }

        ApiClient.getService().getGaji(karyawanId, bulan, tahun)
                .enqueue(new Callback<GajiResponse>() {
                    @Override
                    public void onResponse(Call<GajiResponse> call,
                                           Response<GajiResponse> response) {
                        Log.d(TAG, "getGaji response code: " + response.code());

                        if (!response.isSuccessful() || response.body() == null) {
                            Log.w(TAG, "getGaji tidak sukses: " + response.code());
                            try {
                                String err = response.errorBody() != null
                                        ? response.errorBody().string() : "null";
                                Log.e(TAG, "errorBody: " + err);
                            } catch (Exception ignored) {}
                            return;
                        }

                        GajiResponse gaji = response.body();
                        Log.d(TAG, "getGaji sukses=" + gaji.sukses
                                + ", data=" + (gaji.data != null ? "ada" : "null"));

                        if (!gaji.sukses || gaji.data == null) {
                            Log.w(TAG, "getGaji sukses=false atau data null: " + gaji.pesan);
                            return;
                        }

                        GajiResponse.GajiData d = gaji.data;
                        Log.d(TAG, "gaji_pokok=" + d.gaji_pokok
                                + ", transport=" + d.tunjangan_transport
                                + ", makan=" + d.tunjangan_makan
                                + ", jabatan=" + d.tunjangan_jabatan);

                        session.simpanDataGaji(
                                d.gaji_pokok,
                                d.tunjangan_transport,
                                d.tunjangan_makan,
                                d.tunjangan_jabatan,
                                1000,
                                100000
                        );

                        runOnUiThread(() -> {
                            txtGajiPokok.setText("Gaji Pokok          : Rp "
                                    + formatRupiah(session.getGajiPokok()));
                            txtTunjangan.setText("Total Tunjangan  : Rp "
                                    + formatRupiah(session.getTotalTunjangan()));
                            txtTotalPenghasilan.setText("Total Penghasilan : Rp "
                                    + formatRupiah(session.getTotalPenghasilan()));
                        });
                    }

                    @Override
                    public void onFailure(Call<GajiResponse> call, Throwable t) {
                        Log.e(TAG, "❌ getGaji onFailure: " + t.getMessage());
                    }
                });
    }

    private void loadVersi() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            txtVersi.setText("Versi " + info.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            txtVersi.setText("Versi -");
        }
    }

    private void loadStatistik() {
        txtStatHadir.setText("-");
        txtStatTerlambat.setText("-");
        txtStatIzin.setText("-");

        api.riwayat().enqueue(new Callback<RiwayatResponse>() {
            @Override
            public void onResponse(Call<RiwayatResponse> call,
                                   Response<RiwayatResponse> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                RiwayatResponse body = response.body();
                if (!body.sukses || body.data == null) return;

                List<RiwayatResponse.DataRiwayat> data = body.data;

                int hadir     = 0;
                int terlambat = 0;

                for (RiwayatResponse.DataRiwayat item : data) {
                    if ("tepat_waktu".equals(item.status)) {
                        hadir++;
                    } else if ("terlambat".equals(item.status)) {
                        hadir++;
                        terlambat++;
                    }
                }

                int finalHadir     = hadir;
                int finalTerlambat = terlambat;

                runOnUiThread(() -> {
                    txtStatHadir.setText(String.valueOf(finalHadir));
                    txtStatTerlambat.setText(String.valueOf(finalTerlambat));
                    txtStatIzin.setText("-");
                });
            }

            @Override
            public void onFailure(Call<RiwayatResponse> call, Throwable t) {
                runOnUiThread(() -> {
                    txtStatHadir.setText("?");
                    txtStatTerlambat.setText("?");
                    txtStatIzin.setText("?");
                });
            }
        });
    }

    private void setupListeners() {
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            session.setDarkMode(isChecked);
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        btnTentang.setOnClickListener(v -> showTentangDialog());

        btnLogout.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Konfirmasi Logout")
                        .setMessage("Yakin ingin keluar?")
                        .setPositiveButton("Logout", (dialog, which) -> {
                            session.clearSession();
                            ApiClient.reset();
                            Intent intent = new Intent(ProfilActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton("Batal", null)
                        .show()
        );
    }

    private void showTentangDialog() {
        String versi;
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            versi = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versi = "-";
        }

        new AlertDialog.Builder(this)
                .setTitle("Tentang Aplikasi")
                .setMessage(
                        "Absensi Kantor\n\n" +
                                "Versi: " + versi + "\n" +
                                "Platform: Android\n" +
                                "Backend: Flask + MySQL\n\n" +
                                "Aplikasi absensi karyawan berbasis pengenalan wajah.\n\n" +
                                "2026 - All rights reserved"
                )
                .setPositiveButton("Tutup", null)
                .show();
    }

    private String formatRupiah(long nominal) {
        String str = String.valueOf(nominal);
        StringBuilder sb = new StringBuilder();
        int counter = 0;
        for (int i = str.length() - 1; i >= 0; i--) {
            if (counter > 0 && counter % 3 == 0) sb.insert(0, ".");
            sb.insert(0, str.charAt(i));
            counter++;
        }
        return sb.toString();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}