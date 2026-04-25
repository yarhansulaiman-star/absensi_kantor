package com.example.absensi_kantor.model;

import com.google.gson.annotations.SerializedName;

public class BaseResponse {

    @SerializedName("sukses")
    public boolean sukses;   // ← public, bisa akses langsung

    @SerializedName("pesan")
    public String pesan;     // ← public, bisa akses langsung

    // Getter tetap ada agar SuratIzinActivity tidak error
    public boolean isSukses() { return sukses; }
    public String  getPesan() { return pesan;  }
}