package com.example.absensi_kantor.ui.auth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.absensi_kantor.ui.MainActivity;
import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.api.SessionManager;
import com.example.absensi_kantor.databinding.ActivityLoginBinding;
import com.example.absensi_kantor.model.auth.LoginResponse;
import com.example.absensi_kantor.model.auth.ForgotPasswordResponse;
import com.example.absensi_kantor.utils.AlarmScheduler;
import com.example.absensi_kantor.utils.NotificationHelper;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ApiClient.init(this);
        session = new SessionManager(this);
        NotificationHelper.createNotificationChannels(this);

        if (session.isLoggedIn()) {
            bukaMainActivity();
            return;
        }

        binding.tombolLogin.setOnClickListener(v -> prosesLogin());

        binding.inputPassword.setOnEditorActionListener((v, actionId, event) -> {
            prosesLogin();
            return true;
        });

        binding.tombolRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        binding.txtLupaPassword.setOnClickListener(v ->
                startActivity(new Intent(this, LupaPasswordActivity.class)));
    }

    private void prosesLogin() {
        String username = binding.inputUsername.getText().toString().trim();
        String password = binding.inputPassword.getText().toString().trim();

        if (username.isEmpty()) {
            binding.inputUsername.setError("Username harus diisi!");
            return;
        }
        if (password.isEmpty()) {
            binding.inputPassword.setError("Password harus diisi!");
            return;
        }

        binding.labelError.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tombolLogin.setEnabled(false);

        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);

        ApiClient.getService().login(body).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call,
                                   Response<LoginResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                binding.tombolLogin.setEnabled(true);

                Log.d(TAG, "Login response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse res = response.body();
                    Log.d(TAG, "sukses=" + res.sukses + ", role=" + res.role);

                    if (res.sukses) {
                        // ✅ Simpan session (tidak duplikat)
                        session.simpanSession(res.token, res.username, res.role, res.userId);
                        session.simpanKaryawanId(res.karyawanId);

                        // ✅ Simpan data gaji dari response login
                        long tarifTerlambat = res.tarifTerlambat > 0
                                ? (long) res.tarifTerlambat : 1000;
                        long tarifAlpha     = res.tarifAlpha > 0
                                ? (long) res.tarifAlpha : 100000;

                        session.simpanDataGaji(
                                (long) res.gajiPokok,
                                (long) res.tunjanganTransport,
                                (long) res.tunjanganMakan,
                                (long) res.tunjanganJabatan,
                                tarifTerlambat,
                                tarifAlpha
                        );

                        // ✅ Debug log — verifikasi data tersimpan benar
                        Log.d(TAG, "=== DEBUG LOGIN ===");
                        Log.d(TAG, "res.karyawanId      = " + res.karyawanId);
                        Log.d(TAG, "res.gajiPokok       = " + res.gajiPokok);
                        Log.d(TAG, "session.karyawanId  = " + session.getKaryawanId());
                        Log.d(TAG, "session.gajiPokok   = " + session.getGajiPokok());
                        Log.d(TAG, "==================");

                        Log.d(TAG, "Gaji tersimpan → pokok=" + (long) res.gajiPokok
                                + ", transport=" + (long) res.tunjanganTransport
                                + ", makan="     + (long) res.tunjanganMakan
                                + ", jabatan="   + (long) res.tunjanganJabatan);

                        // Jadwalkan alarm absen
                        AlarmScheduler.jadwalkanPengingatAbsen(LoginActivity.this);

                        // Ambil FCM Token lalu buka MainActivity
                        ambilFcmToken();

                    } else {
                        tampilkanError(res.pesan != null ? res.pesan
                                : "Username atau password salah!");
                    }
                } else {
                    try {
                        String errBody = response.errorBody() != null
                                ? response.errorBody().string() : "";
                        Log.e(TAG, "Login error " + response.code() + ": " + errBody);
                    } catch (Exception ignored) {}
                    tampilkanError("Login gagal (kode " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.tombolLogin.setEnabled(true);
                Log.e(TAG, "onFailure: " + t.getMessage());
                Toast.makeText(LoginActivity.this,
                        "Gagal konek ke server!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void ambilFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String fcmToken = task.getResult();

                        // ✅ PERBAIKAN: FCM token sekarang disimpan lewat SessionManager
                        // (EncryptedSharedPreferences), bukan SharedPreferences plain text terpisah lagi.
                        session.simpanFcmToken(fcmToken);

                        Map<String, String> body = new HashMap<>();
                        body.put("fcm_token", fcmToken);

                        ApiClient.getService()
                                .simpanFcmToken("Bearer " + session.getToken(), body)
                                .enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> response) {
                                        Log.d(TAG, "FCM token terkirim: " + response.code());
                                        bukaMainActivity();
                                    }
                                    @Override
                                    public void onFailure(Call<Void> call, Throwable t) {
                                        Log.e(TAG, "Gagal kirim FCM token: " + t.getMessage());
                                        bukaMainActivity();
                                    }
                                });
                    } else {
                        Log.e(TAG, "Gagal ambil FCM token");
                        bukaMainActivity();
                    }
                });
    }

    private void tampilkanError(String pesan) {
        binding.labelError.setVisibility(View.VISIBLE);
        binding.labelError.setText(pesan);
    }

    private void bukaMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // ===================== ✅ FITUR BARU: LUPA PASSWORD =====================

    private void tampilkanDialogLupaPassword() {
        EditText etEmail = new EditText(this);
        etEmail.setHint("Email terdaftar");
        etEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        new AlertDialog.Builder(this)
                .setTitle("Lupa Password")
                .setMessage("Masukkan email yang terdaftar, link reset password akan dikirim ke email tersebut.")
                .setView(etEmail)
                .setPositiveButton("Kirim", (dialog, which) -> {
                    String email = etEmail.getText().toString().trim();
                    if (email.isEmpty()) {
                        Toast.makeText(this, "Email wajib diisi", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    kirimLupaPassword(email);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void kirimLupaPassword(String email) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        ApiClient.getService().lupaPassword(body).enqueue(new Callback<ForgotPasswordResponse>() {
            @Override
            public void onResponse(Call<ForgotPasswordResponse> call,
                                   Response<ForgotPasswordResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(LoginActivity.this,
                            response.body().pesan, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Gagal mengirim permintaan reset", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ForgotPasswordResponse> call, Throwable t) {
                Log.e(TAG, "lupaPassword onFailure: " + t.getMessage());
                Toast.makeText(LoginActivity.this,
                        "Gagal konek ke server!", Toast.LENGTH_LONG).show();
            }
        });
    }
}