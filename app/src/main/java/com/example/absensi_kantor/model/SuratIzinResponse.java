package com.example.absensi_kantor.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SuratIzinResponse {

    @SerializedName("sukses")
    private boolean sukses;

    @SerializedName("pesan")
    private String pesan;

    @SerializedName("data")
    private List<Data> data;

    public boolean    isSukses() { return sukses; }
    public String     getPesan() { return pesan; }
    public List<Data> getData()  { return data; }

    public static class Data {

        @SerializedName("id")
        private int id;

        @SerializedName("user_id")
        private int userId;

        @SerializedName("nama_karyawan")
        private String namaKaryawan;

        @SerializedName("jenis_izin")
        private String jenisIzin;

        @SerializedName("tanggal_mulai")
        private String tanggalMulai;

        @SerializedName("tanggal_selesai")
        private String tanggalSelesai;

        @SerializedName("keterangan")
        private String keterangan;

        @SerializedName("foto_bukti")
        private String fotoBukti;

        @SerializedName("status")
        private String status;   // "menunggu" | "disetujui" | "ditolak"

        @SerializedName("catatan_hrd")
        private String catatanHrd;

        @SerializedName("created_at")
        private String createdAt;

        public int    getId()             { return id; }
        public int    getUserId()         { return userId; }
        public String getNamaKaryawan()   { return namaKaryawan != null ? namaKaryawan : ""; }
        public String getJenisIzin()      { return jenisIzin; }
        public String getTanggalMulai()   { return tanggalMulai; }
        public String getTanggalSelesai() { return tanggalSelesai; }
        public String getKeterangan()     { return keterangan; }
        public String getFotoBukti()      { return fotoBukti; }
        public String getStatus()         { return status != null ? status : "menunggu"; }
        public String getCatatanHrd()     { return catatanHrd; }
        public String getCreatedAt()      { return createdAt; }
    }
}