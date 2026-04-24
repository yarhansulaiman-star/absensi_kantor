package com.example.absensi_kantor.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    /** Format untuk dikirim ke API: yyyy-MM-dd  (contoh: 2026-04-12) */
    public static String getTanggalHariIni() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    /** Format untuk ditampilkan ke user: dd MMMM yyyy  (contoh: 12 April 2026) */
    public static String getTanggalTampil() {
        return new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID")).format(new Date());
    }
}