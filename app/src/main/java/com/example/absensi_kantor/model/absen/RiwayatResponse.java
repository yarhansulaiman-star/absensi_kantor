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

        //  Helper: cek apakah sudah check-out
        public boolean sudahKeluar() {
            return jam_keluar != null && !jam_keluar.isEmpty();
        }

        //  Helper: label status ramah
        public String getLabelStatus() {
            if (status == null) return "Tidak Diketahui";
            switch (status.toLowerCase()) {
                case "hadir":     return "Hadir";
                case "terlambat": return "Terlambat";
                case "izin":      return "Izin";
                case "sakit":     return "Sakit";
                case "alpha":     return "Alpha";
                default:          return status;
            }
        }

        // Helper: warna status untuk UI
        public int getWarnaStatus() {
            if (status == null) return 0xFF9E9E9E;
            switch (status.toLowerCase()) {
                case "hadir":     return 0xFF4ADE80; // hijau
                case "terlambat": return 0xFFFFB74D; // oranye
                case "izin":      return 0xFF64B5F6; // biru
                case "sakit":     return 0xFFBA68C8; // ungu
                case "alpha":     return 0xFFEF5350; // merah
                default:          return 0xFF9E9E9E; // abu
            }
        }
    }
}