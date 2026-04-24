package com.example.absensi_kantor.api;

import android.content.Context;
import com.example.absensi_kantor.api.SessionManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.example.absensi_kantor.BuildConfig;
public class ApiClient {

    private static final String BASE_URL = BuildConfig.BASE_URL;
    private static Retrofit retrofit = null;
    private static ApiService apiService = null;

    public static void init(Context context) {
        if (retrofit == null) {
            SessionManager session = new SessionManager(context);

            // Interceptor untuk Log (Membantu Debugging di Logcat)
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Interceptor untuk menyisipkan Token secara otomatis
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder();

                        String token = session.getToken();
                        if (token != null && !token.isEmpty()) {
                            builder.header("Authorization", "Bearer " + token);
                        }

                        return chain.proceed(builder.build());
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();

            apiService = retrofit.create(ApiService.class);
        }
    }

    public static ApiService getService() {
        return apiService;
    }

    public static void reset() {
        retrofit = null;
        apiService = null;
    }
}