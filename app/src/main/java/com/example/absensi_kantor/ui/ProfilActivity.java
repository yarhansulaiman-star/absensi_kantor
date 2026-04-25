package com.example.absensi_kantor.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.absensi_kantor.R;
import com.example.absensi_kantor.api.ApiClient;
import com.example.absensi_kantor.api.SessionManager;

public class ProfilActivity extends AppCompatActivity {

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profil);

        session = new SessionManager(this);

        TextView txtUser = findViewById(R.id.txtUser);
        TextView txtRole = findViewById(R.id.txtRole);
        Button btnLogout = findViewById(R.id.btnLogout);

        txtUser.setText("Username: " + session.getUsername());
        txtRole.setText("Role: " + session.getRole());

        btnLogout.setOnClickListener(v -> {
            session.clearSession();
            ApiClient.reset();

            Intent intent = new Intent(ProfilActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // ✅ biar tidak bisa balik ke profil
        });
    }
}