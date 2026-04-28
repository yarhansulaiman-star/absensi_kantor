package com.example.absensi_kantor.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {


    public static String getTanggalHariIni() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }


    public static String getTanggalTampil() {
        return new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID")).format(new Date());
    }
}