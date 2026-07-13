package com.example.absensi_kantor.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SessionManager {

    private static final String TAG        = "SessionManager";
    private static final String PREF_NAME   = "absensi_session_secure";
    // ✅ PERBAIKAN: nama file lama (plain text) — dipakai sekali untuk migrasi data existing user
    private static final String PREF_NAME_LAMA = "absensi_session";

    private static final String KEY_TOKEN       = "token";
    private static final String KEY_USERNAME    = "username";
    private static final String KEY_ROLE        = "role";
    private static final String KEY_USER_ID     = "user_id";
    private static final String KEY_KARYAWAN_ID = "karyawan_id";

    // ✅ PERBAIKAN: key baru untuk menyimpan FCM token (sebelumnya method ini belum ada,
    // padahal sudah dipanggil dari LoginActivity -> session.simpanFcmToken(fcmToken))
    private static final String KEY_FCM_TOKEN = "fcm_token";

    // Dark mode
    private static final String KEY_DARK_MODE = "dark_mode";

    // Komponen gaji
    private static final String KEY_GAJI_POKOK          = "gaji_pokok";
    private static final String KEY_TUNJANGAN_TRANSPORT = "tunjangan_transport";
    private static final String KEY_TUNJANGAN_MAKAN     = "tunjangan_makan";
    private static final String KEY_TUNJANGAN_JABATAN   = "tunjangan_jabatan";
    private static final String KEY_TARIF_TERLAMBAT     = "tarif_terlambat";
    private static final String KEY_TARIF_ALPHA         = "tarif_alpha";

    // ✅ PERBAIKAN: satu sumber kebenaran untuk default tarif terlambat.
    // Sebelumnya SessionManager pakai 500 sedangkan LoginActivity pakai 1000 — sekarang disamakan.
    private static final long DEFAULT_TARIF_TERLAMBAT = 1000;
    private static final long DEFAULT_TARIF_ALPHA     = 100000;

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = buatEncryptedPrefs(context);
        migrasiDariPrefsLama(context);
    }

    // ✅ PERBAIKAN: SharedPreferences sekarang dienkripsi (token & data gaji tidak lagi plain text)
    private SharedPreferences buatEncryptedPrefs(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            return EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // Fallback ke SharedPreferences biasa kalau enkripsi gagal dibuat
            // (sangat jarang terjadi, tapi app tidak boleh crash karenanya)
            Log.e(TAG, "Gagal membuat EncryptedSharedPreferences, fallback ke plain prefs: " + e.getMessage());
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    // ✅ PERBAIKAN: pindahkan data lama (dari SharedPreferences plain text) sekali saja,
    // supaya user yang sudah login sebelumnya tidak otomatis ter-logout setelah update app.
    private void migrasiDariPrefsLama(Context context) {
        SharedPreferences prefsLama = context.getSharedPreferences(PREF_NAME_LAMA, Context.MODE_PRIVATE);
        String tokenLama = prefsLama.getString(KEY_TOKEN, "");

        if (!tokenLama.isEmpty() && prefs.getString(KEY_TOKEN, "").isEmpty()) {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(KEY_TOKEN, tokenLama);
            ed.putString(KEY_USERNAME, prefsLama.getString(KEY_USERNAME, ""));
            ed.putString(KEY_ROLE, prefsLama.getString(KEY_ROLE, "user"));
            ed.putInt(KEY_USER_ID, prefsLama.getInt(KEY_USER_ID, 0));
            ed.putInt(KEY_KARYAWAN_ID, prefsLama.getInt(KEY_KARYAWAN_ID, 0));
            ed.putBoolean(KEY_DARK_MODE, prefsLama.getBoolean(KEY_DARK_MODE, false));
            ed.putLong(KEY_GAJI_POKOK, prefsLama.getLong(KEY_GAJI_POKOK, 0));
            ed.putLong(KEY_TUNJANGAN_TRANSPORT, prefsLama.getLong(KEY_TUNJANGAN_TRANSPORT, 0));
            ed.putLong(KEY_TUNJANGAN_MAKAN, prefsLama.getLong(KEY_TUNJANGAN_MAKAN, 0));
            ed.putLong(KEY_TUNJANGAN_JABATAN, prefsLama.getLong(KEY_TUNJANGAN_JABATAN, 0));
            ed.putLong(KEY_TARIF_TERLAMBAT, prefsLama.getLong(KEY_TARIF_TERLAMBAT, DEFAULT_TARIF_TERLAMBAT));
            ed.putLong(KEY_TARIF_ALPHA, prefsLama.getLong(KEY_TARIF_ALPHA, DEFAULT_TARIF_ALPHA));
            ed.apply();

            // Hapus data lama yang plain text supaya tidak menggantung di disk
            prefsLama.edit().clear().apply();
            Log.d(TAG, "Migrasi sesi lama ke penyimpanan terenkripsi berhasil.");
        }
    }

    // ===================== SESSION =====================
    public void simpanSession(String token, String username, String role, int userId) {
        // ✅ PERBAIKAN: editor selalu diambil fresh dari prefs, bukan disimpan sebagai field,
        // supaya tidak ada risiko menulis berdasarkan state basi kalau ada beberapa instance aktif.
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USERNAME, username)
                .putString(KEY_ROLE, role)
                .putInt(KEY_USER_ID, userId)
                .apply();
    }

    public void simpanSession(String token, String username, String role) {
        simpanSession(token, username, role, 0);
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, "");
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, "user");
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, 0);
    }

    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }

    // ===================== KARYAWAN ID =====================
    public void simpanKaryawanId(int karyawanId) {
        prefs.edit().putInt(KEY_KARYAWAN_ID, karyawanId).apply();
    }

    public int getKaryawanId() {
        return prefs.getInt(KEY_KARYAWAN_ID, 0);
    }

    // ===================== FCM TOKEN =====================
    // ✅ PERBAIKAN: method ini sebelumnya belum ada, padahal LoginActivity sudah memanggilnya
    // (session.simpanFcmToken(fcmToken)) — inilah penyebab error "cannot resolve method".
    public void simpanFcmToken(String fcmToken) {
        prefs.edit().putString(KEY_FCM_TOKEN, fcmToken).apply();
    }

    public String getFcmToken() {
        return prefs.getString(KEY_FCM_TOKEN, "");
    }

    // ===================== DARK MODE =====================
    public void setDarkMode(boolean isDark) {
        prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    // ===================== GAJI =====================
    public void simpanDataGaji(double gajiPokok,
                               double tunjanganTransport,
                               double tunjanganMakan,
                               double tunjanganJabatan,
                               double tarifTerlambat,
                               double tarifAlpha) {

        prefs.edit()
                .putLong(KEY_GAJI_POKOK, (long) gajiPokok)
                .putLong(KEY_TUNJANGAN_TRANSPORT, (long) tunjanganTransport)
                .putLong(KEY_TUNJANGAN_MAKAN, (long) tunjanganMakan)
                .putLong(KEY_TUNJANGAN_JABATAN, (long) tunjanganJabatan)
                .putLong(KEY_TARIF_TERLAMBAT, (long) tarifTerlambat)
                .putLong(KEY_TARIF_ALPHA, (long) tarifAlpha)
                .apply();
    }

    public long getGajiPokok() {
        return prefs.getLong(KEY_GAJI_POKOK, 0);
    }

    public long getTunjanganTransport() {
        return prefs.getLong(KEY_TUNJANGAN_TRANSPORT, 0);
    }

    public long getTunjanganMakan() {
        return prefs.getLong(KEY_TUNJANGAN_MAKAN, 0);
    }

    public long getTunjanganJabatan() {
        return prefs.getLong(KEY_TUNJANGAN_JABATAN, 0);
    }

    // ✅ PERBAIKAN: default sekarang konsisten dengan fallback di LoginActivity (1000, bukan 500 lagi)
    public long getTarifTerlambat() {
        return prefs.getLong(KEY_TARIF_TERLAMBAT, DEFAULT_TARIF_TERLAMBAT);
    }

    public long getTarifAlpha() {
        return prefs.getLong(KEY_TARIF_ALPHA, DEFAULT_TARIF_ALPHA);
    }

    public long getTotalTunjangan() {
        return getTunjanganTransport()
                + getTunjanganMakan()
                + getTunjanganJabatan();
    }

    public long getTotalPenghasilan() {
        return getGajiPokok() + getTotalTunjangan();
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }
}