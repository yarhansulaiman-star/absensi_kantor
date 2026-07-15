import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
}

android {
    namespace = "com.example.absensi_kantor"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.absensi_kantor"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject BASE_URL ke BuildConfig
        buildConfigField("String", "BASE_URL", "\"${localProps["BASE_URL"]}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp + Logging Interceptor
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // Google Location (FusedLocationProvider)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Guava (ListenableFuture untuk CameraX)
    implementation("com.google.guava:guava:33.0.0-android")

    implementation("com.google.android.material:material:1.11.0")

    // Machine Learning Kit - Face Detection
    implementation("com.google.mlkit:face-detection:16.1.5")

    implementation("androidx.activity:activity:1.7.0")

    // OpenStreetMap
    implementation("org.osmdroid:osmdroid-android:6.1.17")

    // Firebase BoM - mengatur versi semua library Firebase secara konsisten
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-messaging")

    implementation("androidx.core:core:1.12.0")

    // Lottie - background/loading animation
    implementation("com.airbnb.android:lottie:6.4.0")

    // Coroutines - untuk async task (network call, camera analysis) yang lebih rapi
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Lifecycle - ViewModel & LiveData, penting untuk CameraX lifecycle-aware
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // ExifInterface - fix orientasi foto dari kamera (WAJIB untuk foto absensi)
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Security Crypto - simpan token/session secara aman (EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // WorkManager - kalau perlu retry upload absensi otomatis saat koneksi jelek
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // SwipeRefreshLayout - kalau ada list riwayat absensi yang bisa di-refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("com.android.volley:volley:1.2.1")
}