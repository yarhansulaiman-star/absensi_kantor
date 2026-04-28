package com.example.absensi_kantor.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.absensi_kantor.api.SessionManager;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SessionManager session = new SessionManager(context);
            if (session.isLoggedIn()) {
                AlarmScheduler.jadwalkanPengingatAbsen(context);
            }
        }
    }
}