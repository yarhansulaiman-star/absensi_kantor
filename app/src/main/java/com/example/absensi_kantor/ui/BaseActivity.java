package com.example.absensi_kantor.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.absensi_kantor.api.SessionManager;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply dark mode SEBELUM layout di-inflate
        applyDarkMode();
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();


        SessionManager session = new SessionManager(this);
        int targetMode = session.isDarkMode()
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO;

        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode);
            recreate();
        }
    }

    private void applyDarkMode() {
        SessionManager session = new SessionManager(this);
        AppCompatDelegate.setDefaultNightMode(
                session.isDarkMode()
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}