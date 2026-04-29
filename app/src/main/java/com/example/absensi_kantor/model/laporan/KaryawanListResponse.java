package com.example.absensi_kantor.model.laporan;

import java.util.List;

public class KaryawanListResponse {
    public boolean sukses;
    public String  pesan;
    public List<Item> data;

    public static class Item {
        public int    id;
        public String nama;
        public String jabatan;
        public String departemen;
    }
}