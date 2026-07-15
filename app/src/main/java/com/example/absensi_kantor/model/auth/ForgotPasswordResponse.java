package com.example.absensi_kantor.model.auth;

import com.google.gson.annotations.SerializedName;

public class ForgotPasswordResponse {
    @SerializedName("sukses") public boolean sukses;
    @SerializedName("pesan")  public String  pesan;
}