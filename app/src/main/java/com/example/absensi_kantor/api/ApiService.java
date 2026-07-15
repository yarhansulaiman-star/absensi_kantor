package com.example.absensi_kantor.api;

import com.example.absensi_kantor.model.*;
import com.example.absensi_kantor.model.absen.AbsenResponse;
import com.example.absensi_kantor.model.absen.RiwayatResponse;
import com.example.absensi_kantor.model.auth.ForgotPasswordResponse;
import com.example.absensi_kantor.model.auth.LoginResponse;
import com.example.absensi_kantor.model.auth.RegisterResponse;
import com.example.absensi_kantor.model.gaji.GajiResponse;
import com.example.absensi_kantor.model.izin.SuratIzinResponse;
import com.example.absensi_kantor.model.laporan.KaryawanListResponse;
import com.example.absensi_kantor.model.laporan.LaporanResponse;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @POST("login")
    Call<LoginResponse> login(@Body Map<String, String> body);

    @POST("register/multi")
    Call<RegisterResponse> registerMulti(@Body Map<String, Object> body);

    @POST("/forgot-password")
    Call<ForgotPasswordResponse> lupaPassword(@Body Map<String, String> body);

    @POST("absen")
    Call<AbsenResponse> absen(@Body Map<String, Object> body);

    @GET("riwayat")
    Call<RiwayatResponse> riwayat();

    @GET("laporan")
    Call<LaporanResponse> laporan(@Query("tanggal") String tanggal);

    @GET("laporan/pdf")
    @Streaming
    Call<ResponseBody> laporanPdf(@Query("tanggal") String tanggal);

    @GET("gaji")
    Call<GajiResponse> getGaji(
            @Query("karyawan_id") Integer karyawanId,
            @Query("bulan") int bulan,
            @Query("tahun") int tahun
    );

    @GET("karyawan/list")
    Call<KaryawanListResponse> getKaryawanList();

    @POST("gaji/set")
    Call<BaseResponse> setGaji(@Body Map<String, Object> body);

    @POST("surat-izin")
    Call<SuratIzinResponse> kirimSuratIzin(@Body Map<String, Object> body);


    @GET("surat-izin")
    Call<SuratIzinResponse> getSuratIzinSaya();

    @GET("surat-izin/semua")
    Call<SuratIzinResponse> getAllSuratIzin();

    @POST("surat-izin/update-status")
    Call<BaseResponse> updateStatusIzin(@Body Map<String, Object> body);

    @POST("kenali")
    Call<AbsenResponse> kenali(@Body Map<String, Object> body);

    @POST("simpan-fcm-token")
    Call<Void> simpanFcmToken(
            @Header("Authorization") String token,
            @Body Map<String, String> body
    );

    // Dipakai di MainActivity untuk export slip gaji PDF
    @GET("gaji/slip/export")
    @Streaming
    Call<ResponseBody> exportSlipGaji(
            @Query("user_id") int userId,
            @Query("bulan")   int bulan,
            @Query("tahun")   int tahun


    );
}