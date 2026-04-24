package com.example.absensi_kantor.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.absensi_kantor.R;
import com.example.absensi_kantor.model.LaporanResponse;
import java.util.List;

public class LaporanAdapter extends RecyclerView.Adapter<LaporanAdapter.ViewHolder> {

    private final List<LaporanResponse.DataAbsen> data;

    public LaporanAdapter(List<LaporanResponse.DataAbsen> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_laporan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LaporanResponse.DataAbsen item = data.get(position);

        holder.textNama.setText(item.nama != null ? item.nama : "-");
        holder.textJabatan.setText(
                (item.jabatan != null ? item.jabatan : "-") +
                        " - " +
                        (item.departemen != null ? item.departemen : "-"));
        holder.textMasuk.setText("Masuk  : " + (item.jam_masuk  != null ? item.jam_masuk  : "-"));
        holder.textKeluar.setText("Keluar : " + (item.jam_keluar != null ? item.jam_keluar : "-"));

        // Warna status terlambat vs hadir
        String status = item.status != null ? item.status : "-";
        holder.textStatus.setText(status);
        if ("terlambat".equalsIgnoreCase(status)) {
            holder.textStatus.setBackgroundResource(R.drawable.bg_status_terlambat);
        } else {
            holder.textStatus.setBackgroundResource(R.drawable.bg_status);
        }

        // Tampilkan alamat lokasi
        if (holder.textAlamat != null) {
            String alamat = item.alamat != null && !item.alamat.isEmpty()
                    ? item.alamat : "Lokasi tidak tersedia";
            holder.textAlamat.setText("📍 " + alamat);
        }
    }

    @Override
    public int getItemCount() { return data != null ? data.size() : 0; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textNama, textJabatan, textMasuk, textKeluar, textStatus, textAlamat;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textNama    = itemView.findViewById(R.id.textNama);
            textJabatan = itemView.findViewById(R.id.textJabatan);
            textMasuk   = itemView.findViewById(R.id.textMasuk);
            textKeluar  = itemView.findViewById(R.id.textKeluar);
            textStatus  = itemView.findViewById(R.id.textStatus);
            textAlamat  = itemView.findViewById(R.id.textAlamat); // null-safe di onBindViewHolder
        }
    }
}