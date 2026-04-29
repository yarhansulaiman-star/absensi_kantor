package com.example.absensi_kantor.model.laporan;

import com.example.absensi_kantor.model.BaseResponse;

import java.util.List;


public class LaporanResponse extends BaseResponse {
    public int             total;
    public List<DataAbsen> data;

    public static class DataAbsen {
        public String nama;
        public String jabatan;
        public String departemen;
        public String jam_masuk;
        public String jam_keluar;
        public String status;
        public String alamat;
    }
}