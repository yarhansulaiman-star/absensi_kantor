package com.example.absensi_kantor.api;

import android.content.Context;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.example.absensi_kantor.BuildConfig;

import java.util.concurrent.TimeUnit;

public class ApiClient {

    private static final String BASE_URL = BuildConfig.BASE_URL;

    private static ApiService apiService         = null; // timeout default
    private static ApiService apiServiceRegister = null; // timeout 120 detik untuk register

    public static void init(Context context) {
        if (apiService == null) {

            SessionManager session = new SessionManager(context.getApplicationContext());

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC); // BODY terlalu verbose untuk base64

            // ── Builder interceptor yang sama untuk keduanya ──────────
            OkHttpClient.Builder baseBuilder = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder();

                        // Header wajib untuk ngrok
                        builder.header("ngrok-skip-browser-warning", "true");
                        builder.header("Content-Type", "application/json");

                        String token = session.getToken();
                        if (token != null && !token.isEmpty()) {
                            builder.header("Authorization", "Bearer " + token);
                        }

                        return chain.proceed(builder.build());
                    });

            // ── Client standar (timeout default OkHttp = 10 detik) ───
            OkHttpClient clientDefault = baseBuilder.build();

            // ── Client register (timeout 120 detik) ──────────────────
            // Face encoding dlib untuk 3 foto bisa memakan 20–40 detik
            OkHttpClient clientRegister = baseBuilder
                    .connectTimeout(120, TimeUnit.SECONDS)
                    .readTimeout(120,    TimeUnit.SECONDS)
                    .writeTimeout(120,   TimeUnit.SECONDS)
                    .build();

            Retrofit retrofitDefault = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(clientDefault)
                    .build();

            Retrofit retrofitRegister = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(clientRegister)
                    .build();

            apiService         = retrofitDefault.create(ApiService.class);
            apiServiceRegister = retrofitRegister.create(ApiService.class);
        }
    }


    public static ApiService getService() {
        return apiService;
    }

    public static ApiService getServiceRegister() {
        return apiServiceRegister;
    }

    public static void reset() {
        apiService         = null;
        apiServiceRegister = null;
    }
}