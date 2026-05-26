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
        buildConfig = true  //
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

    //Machine learning kit
    implementation ("com.google.mlkit:face-detection:16.1.5")

    implementation("androidx.activity:activity:1.7.0")

    implementation("org.osmdroid:osmdroid-android:6.1.17")

    // FCM
    implementation("com.google.firebase:firebase-messaging:23.4.1")

    implementation("androidx.core:core:1.12.0")

    //background
    implementation("com.airbnb.android:lottie:6.4.0")

}