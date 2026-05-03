package com.example.absensi_kantor.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.absensi_kantor.R;
import com.example.absensi_kantor.ui.absen.AbsenActivity;
import com.example.absensi_kantor.ui.izin.SuratIzinActivity;

public class NotificationHelper {

    //  Channel IDs
    public static final String CHANNEL_ABSEN      = "channel_absen";
    public static final String CHANNEL_PENGINGAT  = "channel_pengingat";
    public static final String CHANNEL_SURAT_IZIN = "channel_surat_izin";

    //  Notification IDs
    public static final int NOTIF_ABSEN_MASUK    = 1001;
    public static final int NOTIF_ABSEN_PULANG   = 1002;
    public static final int NOTIF_ABSEN_BERHASIL = 1003;
    public static final int NOTIF_SURAT_IZIN     = 1004;


    public static boolean canSendNotification(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    // Create Channels

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;

            NotificationChannel pengingat = new NotificationChannel(
                    CHANNEL_PENGINGAT, "Pengingat Absen",
                    NotificationManager.IMPORTANCE_HIGH);
            pengingat.setDescription("Pengingat absen masuk pagi dan pulang sore");

            NotificationChannel absen = new NotificationChannel(
                    CHANNEL_ABSEN, "Konfirmasi Absen",
                    NotificationManager.IMPORTANCE_DEFAULT);
            absen.setDescription("Notifikasi ketika absen berhasil tercatat");

            NotificationChannel suratIzin = new NotificationChannel(
                    CHANNEL_SURAT_IZIN, "Status Surat Izin",
                    NotificationManager.IMPORTANCE_HIGH);
            suratIzin.setDescription("Notifikasi status surat izin disetujui atau ditolak");

            manager.createNotificationChannel(pengingat);
            manager.createNotificationChannel(absen);
            manager.createNotificationChannel(suratIzin);
        }
    }

    //  Pengingat Absen Masuk
    public static void tampilkanPengingatMasuk(Context context) {
        if (!canSendNotification(context)) return;

        Intent intent = new Intent(context, AbsenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pi = PendingIntent.getActivity(context, NOTIF_ABSEN_MASUK, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_PENGINGAT)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("⏰ Waktunya Absen Masuk!")
                        .setContentText("Jangan lupa absen masuk hari ini.")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pi)
                        .setAutoCancel(true);

        //noinspection MissingPermission – sudah dicek via canSendNotification()
        NotificationManagerCompat.from(context).notify(NOTIF_ABSEN_MASUK, builder.build());
    }

    //  Pengingat Absen Pulang
    public static void tampilkanPengingatPulang(Context context) {
        if (!canSendNotification(context)) return;

        Intent intent = new Intent(context, AbsenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pi = PendingIntent.getActivity(context, NOTIF_ABSEN_PULANG, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_PENGINGAT)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("🏠 Waktunya Absen Pulang!")
                        .setContentText("Jangan lupa absen pulang sebelum meninggalkan kantor.")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pi)
                        .setAutoCancel(true);

        //noinspection MissingPermission – sudah dicek via canSendNotification()
        NotificationManagerCompat.from(context).notify(NOTIF_ABSEN_PULANG, builder.build());
    }

    //  Konfirmasi Absen Berhasil
    public static void tampilkanAbsenBerhasil(Context context, String jenisAbsen, String waktu) {
        if (!canSendNotification(context)) return;

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ABSEN)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("✅ Absen " + jenisAbsen + " Berhasil")
                        .setContentText("Absen " + jenisAbsen.toLowerCase()
                                + " tercatat pukul " + waktu)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);

        //noinspection MissingPermission – sudah dicek via canSendNotification()
        NotificationManagerCompat.from(context).notify(NOTIF_ABSEN_BERHASIL, builder.build());
    }

    // Status Surat Izin
    public static void tampilkanStatusSuratIzin(Context context, boolean disetujui, String tanggal) {
        if (!canSendNotification(context)) return;

        Intent intent = new Intent(context, SuratIzinActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pi = PendingIntent.getActivity(context, NOTIF_SURAT_IZIN, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String judul = disetujui ? "✅ Surat Izin Disetujui" : "❌ Surat Izin Ditolak";
        String pesan = disetujui
                ? "Surat izin Anda untuk tanggal " + tanggal + " telah disetujui."
                : "Surat izin Anda untuk tanggal " + tanggal + " ditolak. Silakan hubungi HRD.";

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_SURAT_IZIN)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(judul)
                        .setContentText(pesan)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(pesan))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pi)
                        .setAutoCancel(true);

        //noinspection MissingPermission – sudah dicek via canSendNotification()
        NotificationManagerCompat.from(context).notify(NOTIF_SURAT_IZIN, builder.build());
    }
}