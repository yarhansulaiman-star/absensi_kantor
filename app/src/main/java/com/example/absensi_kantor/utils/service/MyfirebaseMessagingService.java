package com.example.absensi_kantor.utils.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.absensi_kantor.R;
import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.api.SessionManager;
import com.example.absensi_kantor.ui.absen.AbsenActivity;
import com.example.absensi_kantor.utils.NotificationHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyfirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG          = "FCMService";
    private static final String CHANNEL_ID   = "absen_channel";
    private static final String CHANNEL_NAME = "Notifikasi Absen";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "Pesan diterima dari: " + remoteMessage.getFrom());

        String title = "Absensi Kantor";
        String body  = "";

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body  = remoteMessage.getNotification().getBody();
        }

        if (!remoteMessage.getData().isEmpty()) {
            if (remoteMessage.getData().containsKey("title"))
                title = remoteMessage.getData().get("title");
            if (remoteMessage.getData().containsKey("body"))
                body  = remoteMessage.getData().get("body");
        }

        tampilkanNotifikasi(title, body);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "FCM Token baru: " + token);

        getSharedPreferences("fcm_pref", MODE_PRIVATE)
                .edit()
                .putString("fcm_token", token)
                .apply();

        kirimTokenKeServer(token);
    }

    private void kirimTokenKeServer(String fcmToken) {
        try {
            ApiClient.init(this);
            SessionManager session = new SessionManager(this);
            String savedToken = session.getToken();

            if (savedToken == null || savedToken.isEmpty()) {
                Log.d(TAG, "Belum login, token FCM disimpan lokal saja");
                return;
            }

            Map<String, String> body = new HashMap<>();
            body.put("fcm_token", fcmToken);

            ApiClient.getService()
                    .simpanFcmToken("Bearer " + savedToken, body)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call,
                                               @NonNull Response<Void> response) {
                            Log.d(TAG, "✅ Token FCM berhasil dikirim ke server");
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                            Log.e(TAG, "❌ Gagal kirim token FCM: " + t.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "❌ Error kirim token: " + e.getMessage());
        }
    }

    private void tampilkanNotifikasi(String title, String body) {
        // Cek permission POST_NOTIFICATIONS (Android 13+)
        if (!NotificationHelper.canSendNotification(this)) {
            Log.w(TAG, "Notifikasi diblokir — izin POST_NOTIFICATIONS belum diberikan");
            return;
        }

        Intent intent = new Intent(this, AbsenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Pengingat absen masuk dan pulang");
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        // Permission sudah dicek di atas via canSendNotification()
        //noinspection MissingPermission
        NotificationManagerCompat.from(this)
                .notify((int) System.currentTimeMillis(), builder.build());
    }
}