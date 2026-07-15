package com.example.absensi_kantor.utils.service;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.absensi_kantor.api.SessionManager;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FcmTokenManager {

    private static final String TAG = "FcmTokenManager";

    // Sesuaikan dengan alamat backend kamu.
    // Kalau testing di emulator Android Studio, "127.0.0.1" harus diganti "10.0.2.2"
    private static final String BASE_URL = "http://10.0.2.2:5000";

    /**
     * Ambil token FCM terbaru dari Firebase, lalu kirim ke backend.
     * Panggil fungsi ini setelah user berhasil login / saat MainActivity dibuka.
     */
    public static void ambilDanKirimToken(Context context) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Gagal ambil token FCM", task.getException());
                        return;
                    }

                    String fcmToken = task.getResult();
                    Log.d(TAG, "Token FCM: " + fcmToken);
                    kirimTokenKeServer(context, fcmToken);
                });
    }

    /**
     * Kirim token ke endpoint /simpan-fcm-token di backend.
     * Menggunakan JWT dari SessionManager yang sudah ada di project.
     *
     * ⚠️ SESUAIKAN: ganti "session.getToken()" di bawah ini sesuai nama method
     * yang benar-benar ada di SessionManager.java kamu
     * (bisa jadi namanya getJwt(), getAccessToken(), getAuthToken(), dll).
     */
    public static void kirimTokenKeServer(Context context, String fcmToken) {
        SessionManager session = new SessionManager(context);
        String jwtToken = session.getToken(); // <-- SESUAIKAN NAMA METHOD INI

        if (jwtToken == null || jwtToken.isEmpty()) {
            Log.w(TAG, "JWT token belum ada, lewati kirim FCM token (user belum login)");
            return;
        }

        String url = BASE_URL + "/simpan-fcm-token";

        JSONObject body = new JSONObject();
        try {
            body.put("fcm_token", fcmToken);
        } catch (JSONException e) {
            Log.e(TAG, "Gagal buat JSON body", e);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                response -> Log.d(TAG, "Token FCM tersimpan di server: " + response),
                error -> Log.e(TAG, "Gagal kirim token FCM ke server", error)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + jwtToken);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(request);
    }
}