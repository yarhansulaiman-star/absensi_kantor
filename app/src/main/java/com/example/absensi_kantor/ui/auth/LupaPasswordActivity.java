package com.example.absensi_kantor.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.absensi_kantor.R;
import com.example.absensi_kantor.model.auth.ForgotPasswordResponse;
import com.example.absensi_kantor.api.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LupaPasswordActivity extends AppCompatActivity {

    private TextInputLayout layoutEmail;
    private TextInputEditText inputEmail;
    private MaterialButton tombolKirim;
    private MaterialButton tombolBatal;
    private TextView labelError;
    private TextView labelSukses;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lupapassword);

        initView();
        initListener();
    }

    private void initView() {
        layoutEmail = findViewById(R.id.layoutEmail);
        inputEmail = findViewById(R.id.inputEmail);
        tombolKirim = findViewById(R.id.tombolKirim);
        tombolBatal = findViewById(R.id.tombolBatal);
        labelError = findViewById(R.id.labelError);
        labelSukses = findViewById(R.id.labelSukses);
        progressBar = findViewById(R.id.progressBar);
    }

    private void initListener() {
        tombolKirim.setOnClickListener(v -> prosesKirimLinkReset());
        tombolBatal.setOnClickListener(v -> finish());
    }

    private void prosesKirimLinkReset() {
        sembunyikanPesan();

        String email = inputEmail.getText() != null
                ? inputEmail.getText().toString().trim()
                : "";

        if (!validasiEmail(email)) {
            return;
        }

        tampilkanLoading(true);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        Call<ForgotPasswordResponse> call = ApiClient.getService().lupaPassword(body);

        call.enqueue(new Callback<ForgotPasswordResponse>() {
            @Override
            public void onResponse(Call<ForgotPasswordResponse> call,
                                   Response<ForgotPasswordResponse> response) {
                tampilkanLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ForgotPasswordResponse hasil = response.body();

                    if (hasil.sukses) {
                        tampilkanSukses(hasil.pesan != null
                                ? hasil.pesan
                                : "Link reset password telah dikirim ke email Anda.");
                        inputEmail.setText("");
                    } else {
                        tampilkanError(hasil.pesan != null
                                ? hasil.pesan
                                : "Email tidak ditemukan.");
                    }
                } else {
                    tampilkanError("Terjadi kesalahan pada server. Silakan coba lagi.");
                }
            }

            @Override
            public void onFailure(Call<ForgotPasswordResponse> call, Throwable t) {
                tampilkanLoading(false);
                tampilkanError("Gagal terhubung ke server: " + t.getMessage());
            }
        });
    }

    private boolean validasiEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            layoutEmail.setError("Email tidak boleh kosong");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("Format email tidak valid");
            return false;
        }

        layoutEmail.setError(null);
        return true;
    }

    private void tampilkanLoading(boolean sedangLoading) {
        progressBar.setVisibility(sedangLoading ? View.VISIBLE : View.GONE);
        tombolKirim.setEnabled(!sedangLoading);
        tombolBatal.setEnabled(!sedangLoading);
    }

    private void tampilkanError(String pesan) {
        labelSukses.setVisibility(View.GONE);
        labelError.setText(pesan);
        labelError.setVisibility(View.VISIBLE);
    }

    private void tampilkanSukses(String pesan) {
        labelError.setVisibility(View.GONE);
        labelSukses.setText(pesan);
        labelSukses.setVisibility(View.VISIBLE);
    }

    private void sembunyikanPesan() {
        labelError.setVisibility(View.GONE);
        labelSukses.setVisibility(View.GONE);
        layoutEmail.setError(null);
    }
}