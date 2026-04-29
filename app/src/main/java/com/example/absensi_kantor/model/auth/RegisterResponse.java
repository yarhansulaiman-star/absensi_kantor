package com.example.absensi_kantor.model.auth;

import com.google.gson.annotations.SerializedName;

public class RegisterResponse {

    @SerializedName("sukses")
    public boolean sukses;

    @SerializedName("pesan")
    public String pesan;

    @SerializedName("karyawan_id")
    public int karyawan_id;

    @SerializedName("jumlah_encoding")
    public int jumlah_encoding;

    @SerializedName("foto_berhasil")
    public int foto_berhasil;

    @SerializedName("foto_gagal")
    public int foto_gagal;
}