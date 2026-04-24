package com.example.absensi_kantor.api;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "absensi_session";
    private static final String KEY_TOKEN    = "token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE     = "role";
    private static final String KEY_USER_ID  = "user_id";  // ← tambahan

    // Komponen gaji
    private static final String KEY_GAJI_POKOK           = "gaji_pokok";
    private static final String KEY_TUNJANGAN_TRANSPORT  = "tunjangan_transport";
    private static final String KEY_TUNJANGAN_MAKAN      = "tunjangan_makan";
    private static final String KEY_TUNJANGAN_JABATAN    = "tunjangan_jabatan";
    private static final String KEY_TARIF_TERLAMBAT      = "tarif_terlambat";
    private static final String KEY_TARIF_ALPHA          = "tarif_alpha";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }


    public void simpanSession(String token, String username, String role, int userId) {
        editor.putString(KEY_TOKEN,    token);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_ROLE,     role);
        editor.putInt(KEY_USER_ID,     userId);
        editor.apply();
    }


    public void simpanSession(String token, String username, String role) {
        simpanSession(token, username, role, 0);
    }

    public String  getToken()    { return prefs.getString(KEY_TOKEN, ""); }
    public String  getUsername() { return prefs.getString(KEY_USERNAME, ""); }
    public String  getRole()     { return prefs.getString(KEY_ROLE, "user"); }
    public int     getUserId()   { return prefs.getInt(KEY_USER_ID, 0); }

    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }



    public void simpanDataGaji(double gajiPokok,
                               double tunjanganTransport,
                               double tunjanganMakan,
                               double tunjanganJabatan,
                               double tarifTerlambat,
                               double tarifAlpha) {
        editor.putLong(KEY_GAJI_POKOK,         (long) gajiPokok);
        editor.putLong(KEY_TUNJANGAN_TRANSPORT, (long) tunjanganTransport);
        editor.putLong(KEY_TUNJANGAN_MAKAN,     (long) tunjanganMakan);
        editor.putLong(KEY_TUNJANGAN_JABATAN,   (long) tunjanganJabatan);
        editor.putLong(KEY_TARIF_TERLAMBAT,     (long) tarifTerlambat);
        editor.putLong(KEY_TARIF_ALPHA,         (long) tarifAlpha);
        editor.apply();
    }

    public long getGajiPokok()          { return prefs.getLong(KEY_GAJI_POKOK, 0); }
    public long getTunjanganTransport() { return prefs.getLong(KEY_TUNJANGAN_TRANSPORT, 0); }
    public long getTunjanganMakan()     { return prefs.getLong(KEY_TUNJANGAN_MAKAN, 0); }
    public long getTunjanganJabatan()   { return prefs.getLong(KEY_TUNJANGAN_JABATAN, 0); }
    public int  getTarifTerlambat()     { return prefs.getInt(KEY_TARIF_TERLAMBAT, 500); }
    public int  getTarifAlpha()         { return prefs.getInt(KEY_TARIF_ALPHA, 100000); }

    /** Hitung total tunjangan langsung dari session */
    public long getTotalTunjangan() {
        return getTunjanganTransport() + getTunjanganMakan() + getTunjanganJabatan();
    }

    /** Hitung total penghasilan (gaji pokok + semua tunjangan) */
    public long getTotalPenghasilan() {
        return getGajiPokok() + getTotalTunjangan();
    }



    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}