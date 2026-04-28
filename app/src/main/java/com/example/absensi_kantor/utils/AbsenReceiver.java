package com.example.absensi_kantor.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AbsenReceiver extends BroadcastReceiver {

    public static final String ACTION_ABSEN_MASUK  = "com.example.absensi_kantor.ACTION_ABSEN_MASUK";
    public static final String ACTION_ABSEN_PULANG = "com.example.absensi_kantor.ACTION_ABSEN_PULANG";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        switch (intent.getAction()) {
            case ACTION_ABSEN_MASUK:
                NotificationHelper.tampilkanPengingatMasuk(context);
                break;
            case ACTION_ABSEN_PULANG:
                NotificationHelper.tampilkanPengingatPulang(context);
                break;
        }
    }
}