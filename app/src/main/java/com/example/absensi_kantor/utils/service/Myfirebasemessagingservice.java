package com.example.absensi_kantor.utils.service;

import com.example.absensi_kantor.utils.NotificationHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class Myfirebasemessagingservice extends FirebaseMessagingService {

    /**
     * Dipanggil setiap kali ada notifikasi FCM masuk (foreground maupun background).
     * Ditampilkan lewat NotificationHelper yang sudah ada, supaya channel & gaya
     * notifikasi konsisten dengan notifikasi lokal lainnya (absen berhasil, surat izin, dll).
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();
        String tipe = data.getOrDefault("tipe", "");

        switch (tipe) {
            case "pengingat_masuk":
                NotificationHelper.tampilkanPengingatMasuk(getApplicationContext());
                break;

            case "pengingat_pulang":
                NotificationHelper.tampilkanPengingatPulang(getApplicationContext());
                break;

            default:
                // Tipe tidak dikenali dari backend — tambahkan case lain di sini kalau perlu
                break;
        }
    }

    /**
     * Dipanggil setiap kali token FCM berubah/diperbarui oleh Firebase
     * (misal saat pertama install, atau token expired lalu digenerate ulang).
     */
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        FcmTokenManager.kirimTokenKeServer(getApplicationContext(), token);
    }
}