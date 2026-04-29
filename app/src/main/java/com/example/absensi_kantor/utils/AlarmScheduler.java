package com.example.absensi_kantor.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.example.absensi_kantor.utils.receiver.AbsenReceiver;

import java.util.Calendar;

public class AlarmScheduler {

    private static final int RC_MASUK  = 2001;
    private static final int RC_PULANG = 2002;

    public static void jadwalkanPengingatAbsen(Context context) {
        jadwalkanAlarm(context, AbsenReceiver.ACTION_ABSEN_MASUK,  RC_MASUK,  7,  30);
        jadwalkanAlarm(context, AbsenReceiver.ACTION_ABSEN_PULANG, RC_PULANG, 17, 0);
    }

    public static void batalkanPengingatAbsen(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(buatPendingIntent(context, AbsenReceiver.ACTION_ABSEN_MASUK,  RC_MASUK));
        am.cancel(buatPendingIntent(context, AbsenReceiver.ACTION_ABSEN_PULANG, RC_PULANG));
    }

    private static void jadwalkanAlarm(Context context, String action,
                                       int requestCode, int jam, int menit) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Calendar waktu = Calendar.getInstance();
        waktu.set(Calendar.HOUR_OF_DAY, jam);
        waktu.set(Calendar.MINUTE, menit);
        waktu.set(Calendar.SECOND, 0);
        waktu.set(Calendar.MILLISECOND, 0);

        if (waktu.getTimeInMillis() <= System.currentTimeMillis()) {
            waktu.add(Calendar.DAY_OF_YEAR, 1);
        }

        am.setRepeating(AlarmManager.RTC_WAKEUP,
                waktu.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                buatPendingIntent(context, action, requestCode));
    }

    private static PendingIntent buatPendingIntent(Context context, String action, int rc) {
        Intent intent = new Intent(context, AbsenReceiver.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, rc, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}