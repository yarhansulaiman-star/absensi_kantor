# 📱 Absensi Kantor

Aplikasi **absensi karyawan berbasis Android** dengan fitur GPS, kamera, laporan kehadiran, dan manajemen gaji berbasis role (**Karyawan**, **HRD**, **Admin**).

> Backend REST API-nya ada di repository [**backend-face-Recognizer**](https://github.com/yarhansulaiman-star/backend-face-Recognizer).

---

## ✨ Fitur

### 👤 Karyawan
- 🔐 Login, register, dan lupa password
- 📍 Absen masuk & pulang dengan validasi **GPS**
- 📸 Absen dengan **verifikasi wajah** (kamera)
- 🗓️ Kalender & riwayat kehadiran
- 📄 Pengajuan surat izin
- 💰 Lihat slip gaji
- 🔔 Notifikasi & pengingat absen otomatis

### 🧑💼 HRD / Admin
- ✅ Persetujuan (approval) surat izin
- 💵 Pengaturan gaji karyawan
- 📊 Laporan kehadiran & statistik dashboard
- 👥 Manajemen data karyawan

---

## 🛠️ Tech Stack

![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=flat-square&logo=firebase&logoColor=black)

- **Bahasa:** Java
- **Platform:** Android (min. SDK sesuai `build.gradle`)
- **Networking:** Retrofit
- **Backend:** Flask REST API + MySQL
- **Push Notification:** Firebase Cloud Messaging
- **Location & Camera:** Google Play Services Location, CameraX/Intent

---

## 📂 Struktur Proyek

```
app/src/main/java/com/example/absensi_kantor/
├── api/                    # Retrofit client & service
│   ├── ApiClient.java
│   ├── ApiService.java
│   └── SessionManager.java
├── model/                  # Model response API
│   ├── absen/  auth/  gaji/  izin/  laporan/
├── ui/
│   ├── MainActivity.java
│   ├── DashboardStatistikActivity.java
│   ├── KalenderActivity.java
│   ├── absen/              # AbsenActivity, FaceOverlayView
│   ├── auth/               # Login, Register, LupaPassword, Profil
│   ├── gaji/               # GajiActivity, SetGajiActivity
│   ├── izin/               # SuratIzin, ApproveSuratIzin
│   └── laporan/            # LaporanActivity, RiwayatActivity
└── utils/                  # Helper, alarm, notifikasi, receiver
```

---

## 🚀 Cara Menjalankan

1. Clone repository:
   ```bash
   git clone https://github.com/yarhansulaiman-star/absensi_kantor.git
   ```
2. Buka project di **Android Studio**.
3. Tambahkan file `app/google-services.json` dari Firebase Console (file ini **tidak** ikut ter-commit).
4. Sesuaikan `BASE_URL` API pada `ApiClient.java` ke alamat backend.
5. Jalankan aplikasi pada emulator / perangkat.

---

## 🔒 Catatan Keamanan

- `app/google-services.json`, `local.properties`, keystore (`.jks`/`.keystore`), dan `key.properties` tidak boleh di-commit.
- Semua konfigurasi sensitif sudah didaftarkan pada `.gitignore`.

---

## 👤 Author

**Ahmed Yarhan Sulaiman** · [@yarhansulaiman-star](https://github.com/yarhansulaiman-star)
