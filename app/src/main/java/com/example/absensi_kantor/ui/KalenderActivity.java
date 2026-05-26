package com.example.absensi_kantor.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.absensi_kantor.R;
import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.api.SessionManager;
import com.example.absensi_kantor.model.absen.RiwayatResponse;
import com.example.absensi_kantor.model.izin.SuratIzinResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class KalenderActivity extends AppCompatActivity {

    private static final int COLOR_HADIR   = 0xFF4ADE80;
    private static final int COLOR_IZIN    = 0xFFFFD700;
    private static final int COLOR_SAKIT   = 0xFF60A5FA;
    private static final int COLOR_CUTI    = 0xFFFFB347;
    private static final int COLOR_ALPHA   = 0xFFEF4444;
    private static final int COLOR_DEFAULT = 0xFFF0F4FF;

    private static final String[] DATE_FORMATS = {
            "yyyy-MM-dd",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "dd-MM-yyyy",
            "dd/MM/yyyy",
            "MM/dd/yyyy"
    };

    private GridLayout gridKalender;
    private TextView tvBulanTahun;
    private ImageButton btnPrevBulan, btnNextBulan;
    private ProgressBar progressBar;
    private LinearLayout layoutDaftarIzin; // container daftar izin HRD

    private SessionManager session;
    private boolean isHrd;

    private int currentBulan;
    private int currentTahun;

    // Map: "YYYY-MM-DD" -> status
    private final Map<String, String> statusMap = new HashMap<>();

    // List semua izin bulan ini (untuk HRD)
    private final List<SuratIzinResponse.Data> daftarIzinBulanIni = new ArrayList<>();

    private final String[] NAMA_BULAN = {
            "", "Januari","Februari","Maret","April","Mei","Juni",
            "Juli","Agustus","September","Oktober","November","Desember"
    };
    private final String[] NAMA_HARI = {"Min","Sen","Sel","Rab","Kam","Jum","Sab"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kalender);

        ApiClient.init(this);
        session = new SessionManager(this);
        isHrd   = "hrd".equals(session.getRole()) || "admin".equals(session.getRole());

        gridKalender     = findViewById(R.id.gridKalender);
        tvBulanTahun     = findViewById(R.id.tvBulanTahun);
        btnPrevBulan     = findViewById(R.id.btnPrevBulan);
        btnNextBulan     = findViewById(R.id.btnNextBulan);
        progressBar      = findViewById(R.id.progressBarKalender);
        layoutDaftarIzin = findViewById(R.id.layoutDaftarIzin);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Sembunyikan daftar izin jika bukan HRD
        if (layoutDaftarIzin != null) {
            layoutDaftarIzin.setVisibility(isHrd ? View.VISIBLE : View.GONE);
        }

        Calendar cal = Calendar.getInstance();
        currentBulan = cal.get(Calendar.MONTH) + 1;
        currentTahun = cal.get(Calendar.YEAR);

        btnPrevBulan.setOnClickListener(v -> {
            currentBulan--;
            if (currentBulan < 1) { currentBulan = 12; currentTahun--; }
            muatData();
        });

        btnNextBulan.setOnClickListener(v -> {
            currentBulan++;
            if (currentBulan > 12) { currentBulan = 1; currentTahun++; }
            muatData();
        });

        muatData();
    }

    private void muatData() {
        progressBar.setVisibility(View.VISIBLE);
        statusMap.clear();
        daftarIzinBulanIni.clear();
        tvBulanTahun.setText(NAMA_BULAN[currentBulan] + " " + currentTahun);

        final int[] selesai    = {0};
        final int   totalTugas = isHrd ? 3 : 2;

        // 1) Riwayat absen milik sendiri
        ApiClient.getService().riwayat().enqueue(new Callback<RiwayatResponse>() {
            @Override
            public void onResponse(Call<RiwayatResponse> call, Response<RiwayatResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().data != null) {
                    String prefix = String.format("%04d-%02d", currentTahun, currentBulan);
                    for (RiwayatResponse.DataRiwayat item : response.body().data) {
                        if (item.tanggal != null) {
                            String tgl = normalisasiTanggal(item.tanggal);
                            if (tgl != null && tgl.startsWith(prefix)) {
                                statusMap.put(tgl, "hadir");
                            }
                        }
                    }
                }
                cekSelesai(++selesai[0], totalTugas);
            }

            @Override
            public void onFailure(Call<RiwayatResponse> call, Throwable t) {
                cekSelesai(++selesai[0], totalTugas);
            }
        });

        // 2) Surat izin milik sendiri
        ApiClient.getService().getSuratIzinSaya().enqueue(new Callback<SuratIzinResponse>() {
            @Override
            public void onResponse(Call<SuratIzinResponse> call,
                                   Response<SuratIzinResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    prosesIzin(response.body().getData(), false);
                }
                cekSelesai(++selesai[0], totalTugas);
            }

            @Override
            public void onFailure(Call<SuratIzinResponse> call, Throwable t) {
                cekSelesai(++selesai[0], totalTugas);
            }
        });

        // 3) Semua izin karyawan — hanya HRD
        if (isHrd) {
            ApiClient.getService().getAllSuratIzin().enqueue(new Callback<SuratIzinResponse>() {
                @Override
                public void onResponse(Call<SuratIzinResponse> call,
                                       Response<SuratIzinResponse> response) {
                    if (response.isSuccessful() && response.body() != null
                            && response.body().getData() != null) {
                        prosesIzin(response.body().getData(), true);
                    }
                    cekSelesai(++selesai[0], totalTugas);
                }

                @Override
                public void onFailure(Call<SuratIzinResponse> call, Throwable t) {
                    cekSelesai(++selesai[0], totalTugas);
                }
            });
        }
    }

    private void prosesIzin(List<SuratIzinResponse.Data> list, boolean untukDaftar) {
        String prefix = String.format("%04d-%02d", currentTahun, currentBulan);

        for (SuratIzinResponse.Data item : list) {
            String status = item.getStatus();

            if ("disetujui".equalsIgnoreCase(status)) {
                isiRentangTanggal(item.getTanggalMulai(), item.getTanggalSelesai(), item.getJenisIzin());
            } else if ("ditolak".equalsIgnoreCase(status)) {
                isiRentangTanggal(item.getTanggalMulai(), item.getTanggalSelesai(), "alpha");
            }

            // Kumpulkan untuk daftar HRD — semua status, asal di bulan ini
            if (untukDaftar) {
                String tglMulai = normalisasiTanggal(item.getTanggalMulai());
                String tglSelesai = normalisasiTanggal(item.getTanggalSelesai());
                boolean adaDibulanIni = false;

                if (tglMulai != null && tglMulai.startsWith(prefix)) adaDibulanIni = true;
                if (tglSelesai != null && tglSelesai.startsWith(prefix)) adaDibulanIni = true;

                // Cek rentang tanggal yang melintasi bulan ini
                if (!adaDibulanIni && tglMulai != null && tglSelesai != null) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        Date dMulai   = sdf.parse(tglMulai);
                        Date dSelesai = sdf.parse(tglSelesai);
                        Calendar cBulan = Calendar.getInstance();
                        cBulan.set(currentTahun, currentBulan - 1, 1);
                        Date awalBulan = cBulan.getTime();
                        cBulan.set(currentTahun, currentBulan - 1,
                                cBulan.getActualMaximum(Calendar.DAY_OF_MONTH));
                        Date akhirBulan = cBulan.getTime();
                        if (dMulai != null && dSelesai != null
                                && !dMulai.after(akhirBulan) && !dSelesai.before(awalBulan)) {
                            adaDibulanIni = true;
                        }
                    } catch (ParseException ignored) {}
                }

                if (adaDibulanIni) {
                    daftarIzinBulanIni.add(item);
                }
            }
        }
    }

    private void cekSelesai(int selesai, int total) {
        if (selesai == total) renderKalender();
    }

    private void isiRentangTanggal(String mulai, String selesai, String jenis) {
        if (mulai == null || selesai == null) return;

        String tglMulai   = normalisasiTanggal(mulai);
        String tglSelesai = normalisasiTanggal(selesai);
        if (tglMulai == null || tglSelesai == null) return;

        try {
            String[] bm = tglMulai.split("-");
            String[] bs = tglSelesai.split("-");

            Calendar c = Calendar.getInstance();
            c.set(Integer.parseInt(bm[0]), Integer.parseInt(bm[1]) - 1, Integer.parseInt(bm[2]));

            Calendar akhir = Calendar.getInstance();
            akhir.set(Integer.parseInt(bs[0]), Integer.parseInt(bs[1]) - 1, Integer.parseInt(bs[2]));

            String prefix = String.format("%04d-%02d", currentTahun, currentBulan);

            while (!c.after(akhir)) {
                String tgl = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                        c.get(Calendar.YEAR),
                        c.get(Calendar.MONTH) + 1,
                        c.get(Calendar.DAY_OF_MONTH));

                if (tgl.startsWith(prefix)) {
                    String statusBaru = jenis != null ? jenis.toLowerCase() : "izin";
                    if (!"alpha".equals(statusMap.get(tgl))) {
                        statusMap.put(tgl, statusBaru);
                    }
                }
                c.add(Calendar.DAY_OF_MONTH, 1);
            }
        } catch (Exception ignored) {}
    }

    private void renderKalender() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            gridKalender.removeAllViews();

            for (String hari : NAMA_HARI) {
                gridKalender.addView(buatHeaderHari(hari));
            }

            Calendar cal = Calendar.getInstance();
            cal.set(currentTahun, currentBulan - 1, 1);
            int hariPertama = cal.get(Calendar.DAY_OF_WEEK) - 1;
            int totalHari   = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

            for (int i = 0; i < hariPertama; i++) {
                gridKalender.addView(buatSelKosong());
            }

            String today = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    Calendar.getInstance().get(Calendar.YEAR),
                    Calendar.getInstance().get(Calendar.MONTH) + 1,
                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH));

            for (int hari = 1; hari <= totalHari; hari++) {
                String tgl    = String.format(Locale.getDefault(),
                        "%04d-%02d-%02d", currentTahun, currentBulan, hari);
                String status   = statusMap.getOrDefault(tgl, "");
                boolean isToday = tgl.equals(today);
                gridKalender.addView(buatSelTanggal(hari, status, isToday));
            }

            // Render daftar izin karyawan (HRD only)
            if (isHrd && layoutDaftarIzin != null) {
                renderDaftarIzin();
            }
        });
    }

    /**
     * Tampilkan daftar nama karyawan yang izin/sakit/cuti/alpha di bulan ini
     * Hanya terlihat oleh HRD
     */
    private void renderDaftarIzin() {
        layoutDaftarIzin.removeAllViews();

        if (daftarIzinBulanIni.isEmpty()) {
            TextView tvKosong = new TextView(this);
            tvKosong.setText("Tidak ada izin di bulan ini");
            tvKosong.setTextColor(0xFF888888);
            tvKosong.setTextSize(13f);
            tvKosong.setPadding(0, 8, 0, 8);
            layoutDaftarIzin.addView(tvKosong);
            return;
        }

        // Judul
        TextView tvJudul = new TextView(this);
        tvJudul.setText("Daftar Izin Karyawan — " + NAMA_BULAN[currentBulan] + " " + currentTahun);
        tvJudul.setTextSize(14f);
        tvJudul.setTypeface(null, Typeface.BOLD);
        tvJudul.setTextColor(0xFF0F3460);
        tvJudul.setPadding(0, 0, 0, 12);
        layoutDaftarIzin.addView(tvJudul);

        // Urutkan berdasarkan tanggal mulai
        List<SuratIzinResponse.Data> sorted = new ArrayList<>(daftarIzinBulanIni);
        Collections.sort(sorted, (a, b) -> {
            String ta = normalisasiTanggal(a.getTanggalMulai());
            String tb = normalisasiTanggal(b.getTanggalMulai());
            if (ta == null) return 1;
            if (tb == null) return -1;
            return ta.compareTo(tb);
        });

        for (SuratIzinResponse.Data item : sorted) {
            layoutDaftarIzin.addView(buatItemIzin(item));
        }
    }

    private View buatItemIzin(SuratIzinResponse.Data item) {
        // Card container
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dpToPx(8));
        card.setLayoutParams(cardLp);
        card.setRadius(dpToPx(10));
        card.setCardElevation(dpToPx(2));
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        row.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        // Indikator warna status di sisi kiri
        View indikator = new View(this);
        LinearLayout.LayoutParams indLp = new LinearLayout.LayoutParams(dpToPx(5), LinearLayout.LayoutParams.MATCH_PARENT);
        indLp.setMargins(0, 0, dpToPx(10), 0);
        indikator.setLayoutParams(indLp);
        indikator.setBackgroundColor(getWarnaStatus(
                item.getStatus().equalsIgnoreCase("ditolak") ? "alpha"
                        : item.getJenisIzin() != null ? item.getJenisIzin().toLowerCase() : "izin"));

        // Konten teks
        LinearLayout konten = new LinearLayout(this);
        konten.setOrientation(LinearLayout.VERTICAL);
        konten.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        // Nama karyawan
        TextView tvNama = new TextView(this);
        tvNama.setText(item.getNamaKaryawan());
        tvNama.setTextSize(13f);
        tvNama.setTypeface(null, Typeface.BOLD);
        tvNama.setTextColor(0xFF1A1A2E);

        // Jenis izin + tanggal
        String tglMulai   = normalisasiTanggal(item.getTanggalMulai());
        String tglSelesai = normalisasiTanggal(item.getTanggalSelesai());
        String rentang    = (tglMulai != null ? tglMulai : item.getTanggalMulai())
                + " s/d "
                + (tglSelesai != null ? tglSelesai : item.getTanggalSelesai());

        TextView tvDetail = new TextView(this);
        tvDetail.setText(item.getJenisIzin() + " • " + rentang);
        tvDetail.setTextSize(11f);
        tvDetail.setTextColor(0xFF666666);

        konten.addView(tvNama);
        konten.addView(tvDetail);

        // Badge status
        TextView tvBadge = new TextView(this);
        String statusText;
        int badgeColor;
        switch (item.getStatus().toLowerCase()) {
            case "disetujui":
                statusText = "DISETUJUI"; badgeColor = 0xFF4CAF50; break;
            case "ditolak":
                statusText = "DITOLAK";   badgeColor = 0xFFEF4444; break;
            default:
                statusText = "MENUNGGU";  badgeColor = 0xFFFF9800; break;
        }
        tvBadge.setText(statusText);
        tvBadge.setTextSize(10f);
        tvBadge.setTypeface(null, Typeface.BOLD);
        tvBadge.setTextColor(Color.WHITE);
        tvBadge.setBackgroundColor(badgeColor);
        tvBadge.setPadding(dpToPx(6), dpToPx(3), dpToPx(6), dpToPx(3));
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        badgeLp.gravity = Gravity.CENTER_VERTICAL;
        tvBadge.setLayoutParams(badgeLp);

        row.addView(indikator);
        row.addView(konten);
        row.addView(tvBadge);
        card.addView(row);
        return card;
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private String normalisasiTanggal(String raw) {
        if (raw == null || raw.isEmpty()) return null;

        if (raw.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            return raw.substring(0, 10);
        }

        for (String fmt : DATE_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.ENGLISH);
                sdf.setLenient(false);
                Date date = sdf.parse(raw);
                if (date != null) {
                    return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
                }
            } catch (ParseException ignored) {}
        }

        try {
            String cleaned = raw.replaceAll("^[A-Za-z]+,\\s*", "").trim();
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
            Date date = sdf.parse(cleaned);
            if (date != null) {
                return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
            }
        } catch (ParseException ignored) {}

        return null;
    }

    private TextView buatHeaderHari(String nama) {
        TextView tv = new TextView(this);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width      = 0;
        lp.height     = GridLayout.LayoutParams.WRAP_CONTENT;
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        tv.setLayoutParams(lp);
        tv.setText(nama);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(11f);
        tv.setTextColor(0xFF0F3460);
        tv.setPadding(0, 8, 0, 8);
        tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    private View buatSelKosong() {
        View v = new View(this);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width      = 0;
        lp.height     = dpToPx(42);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        v.setLayoutParams(lp);
        return v;
    }

    private CardView buatSelTanggal(int hari, String status, boolean isToday) {
        CardView card = new CardView(this);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width      = 0;
        lp.height     = dpToPx(42);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        lp.setMargins(2, 2, 2, 2);
        card.setLayoutParams(lp);
        card.setRadius(dpToPx(8));
        card.setCardElevation(isToday ? dpToPx(3) : 0);
        card.setCardBackgroundColor(isToday ? 0xFF0F3460 : getWarnaStatus(status));

        TextView tv = new TextView(this);
        tv.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        tv.setText(String.valueOf(hari));
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(12f);
        tv.setTextColor(isToday ? Color.WHITE
                : (status == null || status.isEmpty()) ? 0xFF333333 : Color.WHITE);
        if (isToday) tv.setTypeface(null, Typeface.BOLD);

        card.addView(tv);
        return card;
    }

    private int getWarnaStatus(String status) {
        if (status == null || status.isEmpty()) return COLOR_DEFAULT;
        switch (status.toLowerCase()) {
            case "hadir":                return COLOR_HADIR;
            case "izin":
            case "izin pribadi":
            case "keperluan keluarga":
            case "duka":
            case "lainnya":              return COLOR_IZIN;
            case "sakit":                return COLOR_SAKIT;
            case "cuti":                 return COLOR_CUTI;
            case "alpha":                return COLOR_ALPHA;
            default:                     return COLOR_DEFAULT;
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}