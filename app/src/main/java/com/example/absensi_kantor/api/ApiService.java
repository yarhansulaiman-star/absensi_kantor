package com.example.absensi_kantor.api;

import com.example.absensi_kantor.model.*;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ── SUDAH ADA (tidak diubah) ──────────────────────────────────────────

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
            @Query("karyawan_id") int karyawanId,
            @Query("bulan")       int bulan,
            @Query("tahun")       int tahun
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
}