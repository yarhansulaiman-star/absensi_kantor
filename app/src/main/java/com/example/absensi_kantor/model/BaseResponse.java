package com.example.absensi_kantor.model;
import com.google.gson.annotations.SerializedName;

public class BaseResponse {
    @SerializedName("sukses")
    public boolean sukses;
    @SerializedName("pesan")
    public String pesan;
}