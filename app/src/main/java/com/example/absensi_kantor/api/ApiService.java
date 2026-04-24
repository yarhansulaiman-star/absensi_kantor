package com.example.absensi_kantor.api;

import com.example.absensi_kantor.model.*;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @POST("login")
    Call<LoginResponse> login(@Body Map<String, String> body);

    @POST("register/multi")
    Call<RegisterResponse> registerMulti(@Body Map<String, Object> body);

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
            @Query("user_id") int userId,
            @Query("bulan")   int bulan,
            @Query("tahun")   int tahun,
            @Header("Authorization") String token
    );

    @GET("karyawan/list")
    Call<KaryawanListResponse> getKaryawanList();

    @POST("gaji/set")
    Call<BaseResponse> setGaji(@Body Map<String, Object> body);
}