package com.example.absensi_kantor.model;

import java.util.List;

public class GajiResponse {
    public boolean  sukses;
    public String   pesan;
    public GajiData data;

    public static class GajiData {
        public String  nama;
        public String  jabatan;
        public String  periode;

        // Penghasilan
        public long    gaji_pokok;
        public long    tunjangan_transport;
        public long    tunjangan_makan;
        public long    tunjangan_jabatan;
        public long    uang_lembur;
        public long    total_penghasilan;

        // Potongan
        public long    potongan_terlambat;
        public long    potongan_alpha;
        public int     jumlah_hari_alpha;
        public long    bpjs_kesehatan;
        public long    bpjs_tk;
        public long    pph21;
        public long    total_potongan;

        // Hasil akhir
        public long    gaji_bersih;

        // Detail keterlambatan
        public List<DetailTerlambat> detail_terlambat;
    }

    public static class DetailTerlambat {
        public String tanggal;
        public String jam_masuk;
        public int    menit_terlambat;
        public long   potongan;
    }
}