package com.example.absensi_kantor.model.auth;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("sukses")   public boolean sukses;
    @SerializedName("token")    public String  token;
    @SerializedName("username") public String  username;
    @SerializedName("role")     public String  role;
    @SerializedName("user_id")  public int     userId;

    @SerializedName("pesan")    public String  pesan;

    @SerializedName("karyawan_id") public int karyawanId;

    @SerializedName("gaji_pokok")          public double gajiPokok;
    @SerializedName("tunjangan_transport") public double tunjanganTransport;
    @SerializedName("tunjangan_makan")     public double tunjanganMakan;
    @SerializedName("tunjangan_jabatan")   public double tunjanganJabatan;
    @SerializedName("tarif_terlambat")     public double tarifTerlambat;
    @SerializedName("tarif_alpha")         public double tarifAlpha;
}