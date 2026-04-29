package com.example.absensi_kantor.model.gaji;

import java.util.List;

public class GajiResponse {
    public boolean  sukses;
    public String   pesan;
    public GajiData data;

    public static class GajiData {
        public String nama;
        public String jabatan;
        public String periode;

        // Penghasilan
        public double gaji_pokok;
        public double tunjangan_transport;
        public double tunjangan_makan;
        public double tunjangan_jabatan;
        public double uang_lembur;
        public double total_penghasilan;

        // Potongan
        public double potongan_terlambat;
        public double potongan_alpha;
        public int    jumlah_hari_alpha;
        public double bpjs_kesehatan;
        public double bpjs_tk;
        public double pph21;
        public double total_potongan;

        // Hasil akhir
        public double gaji_bersih;

        // Detail keterlambatan
        public List<DetailTerlambat> detail_terlambat;
    }

    public static class DetailTerlambat {
        public String tanggal;
        public String jam_masuk;
        public int    menit_terlambat;
        public double potongan;
    }
}