package com.example.absensi_kantor.model.absen;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RiwayatResponse {

    @SerializedName("sukses")
    public boolean sukses;

    @SerializedName("nama")
    public String nama;

    @SerializedName("data")
    public List<DataRiwayat> data;

    public static class DataRiwayat {

        @SerializedName("tanggal")
        public String tanggal;

        @SerializedName("jam_masuk")
        public String jam_masuk;

        @SerializedName("jam_keluar")
        public String jam_keluar;

        @SerializedName("status")
        public String status;

        @SerializedName("alamat")
        public String alamat;
    }
}