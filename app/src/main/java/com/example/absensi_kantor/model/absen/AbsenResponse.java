package com.example.absensi_kantor.model.absen;

import com.google.gson.annotations.SerializedName;

public class AbsenResponse {

    @SerializedName("sukses")
    public boolean sukses;

    @SerializedName("pesan")
    public String pesan;

    @SerializedName("nama")
    public String nama;

    @SerializedName("jabatan")
    public String jabatan;

    @SerializedName("departemen")
    public String departemen;

    @SerializedName("keyakinan")
    public double keyakinan;

    @SerializedName("status")
    public String status;

    @SerializedName("tipe")
    public String tipe;

    @SerializedName("alamat")
    public String alamat;
}
