package com.example.absensi_kantor.ui;

import android.graphics.Color;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.absensi_kantor.R;
import com.example.absensi_kantor.model.SuratIzinResponse;

import java.util.List;

public class SuratIzinAdapter extends RecyclerView.Adapter<SuratIzinAdapter.ViewHolder> {

    public interface OnIzinActionListener {
        void onSetujui(SuratIzinResponse.Data item);
        void onTolak(SuratIzinResponse.Data item);
    }

    private List<SuratIzinResponse.Data> list;
    private final boolean isHrd;
    private OnIzinActionListener listener;

    public SuratIzinAdapter(List<SuratIzinResponse.Data> list, boolean isHrd) {
        this.list  = list;
        this.isHrd = isHrd;
    }

    public void setOnIzinActionListener(OnIzinActionListener l) { this.listener = l; }

    public void updateData(List<SuratIzinResponse.Data> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_surat_izin, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        SuratIzinResponse.Data item = list.get(position);

        // Nama karyawan — hanya tampil di mode HRD
        if (isHrd) {
            h.tvNama.setVisibility(View.VISIBLE);
            h.tvNama.setText(item.getNamaKaryawan());
        } else {
            h.tvNama.setVisibility(View.GONE);
        }

        h.tvJenisIzin.setText(item.getJenisIzin());
        h.tvTanggal.setText(item.getTanggalMulai() + " s/d " + item.getTanggalSelesai());
        h.tvKeterangan.setText(item.getKeterangan());

        // Badge status
        String status = item.getStatus();
        switch (status) {
            case "disetujui":
                h.tvStatus.setText("DISETUJUI");
                h.tvStatus.setBackgroundColor(Color.parseColor("#4CAF50"));
                break;
            case "ditolak":
                h.tvStatus.setText("DITOLAK");
                h.tvStatus.setBackgroundColor(Color.parseColor("#F44336"));
                break;
            default:
                h.tvStatus.setText("MENUNGGU");
                h.tvStatus.setBackgroundColor(Color.parseColor("#FF9800"));
                break;
        }

        // Catatan HRD
        String catatan = item.getCatatanHrd();
        if (catatan != null && !catatan.isEmpty()) {
            h.tvCatatanHrd.setVisibility(View.VISIBLE);
            h.tvCatatanHrd.setText("Catatan HRD: " + catatan);
        } else {
            h.tvCatatanHrd.setVisibility(View.GONE);
        }

        // Tombol aksi hanya untuk HRD dan status masih menunggu
        if (isHrd && "menunggu".equals(status)) {
            h.layoutAksi.setVisibility(View.VISIBLE);
            h.btnSetujui.setOnClickListener(v -> {
                if (listener != null) listener.onSetujui(item);
            });
            h.btnTolak.setOnClickListener(v -> {
                if (listener != null) listener.onTolak(item);
            });
        } else {
            h.layoutAksi.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return list != null ? list.size() : 0; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView    tvNama, tvJenisIzin, tvTanggal, tvKeterangan, tvStatus, tvCatatanHrd;
        LinearLayout layoutAksi;
        Button      btnSetujui, btnTolak;

        ViewHolder(@NonNull View v) {
            super(v);
            tvNama       = v.findViewById(R.id.tvNama);
            tvJenisIzin  = v.findViewById(R.id.tvJenisIzin);
            tvTanggal    = v.findViewById(R.id.tvTanggal);
            tvKeterangan = v.findViewById(R.id.tvKeterangan);
            tvStatus     = v.findViewById(R.id.tvStatus);
            tvCatatanHrd = v.findViewById(R.id.tvCatatanHrd);
            layoutAksi   = v.findViewById(R.id.layoutAksi);
            btnSetujui   = v.findViewById(R.id.btnSetujui);
            btnTolak     = v.findViewById(R.id.btnTolak);
        }
    }
}