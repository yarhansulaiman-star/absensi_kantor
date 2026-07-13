package com.example.absensi_kantor.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageUtils {

    // 640px cukup untuk face_recognition — dlib hanya butuh wajah ~150px
    // Quality 70 → ~30–60 KB per foto vs 300–500 KB sebelumnya (1280px, q90)
    private static final int MAX_SIZE     = 640;
    private static final int JPEG_QUALITY = 70;

    // ✅ TAMBAHAN: Baca rotasi dari EXIF metadata foto.
    // MediaStore.ACTION_IMAGE_CAPTURE sering menyimpan foto dengan piksel
    // "mentah" dari sensor, sementara orientasi tegak yang benar disimpan
    // terpisah di EXIF. Kalau ini diabaikan, bitmap yang diproses ML Kit
    // bisa sudah miring/berputar dari awal sebelum wajah user ikut miring.
    public static int bacaRotasiExif(Context context, Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return 0;
            ExifInterface exif = new ExifInterface(is);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:  return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (Exception e) {
            return 0; // gagal baca EXIF -> anggap tidak perlu rotasi
        }
    }

    // ✅ TAMBAHAN: Putar bitmap sesuai derajat dari EXIF sebelum diproses lebih lanjut
    public static Bitmap rotateBitmap(Bitmap bitmap, int derajat) {
        if (derajat == 0) return bitmap;
        Matrix matrix = new Matrix();
        matrix.postRotate(derajat);
        return Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap resizeBitmap(Bitmap bitmap, int maxW, int maxH) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w <= maxW && h <= maxH) return bitmap;
        float scale = Math.min((float) maxW / w, (float) maxH / h);
        return Bitmap.createScaledBitmap(bitmap,
                Math.round(w * scale), Math.round(h * scale), true);
    }

    public static Bitmap flipHorizontal(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.preScale(-1f, 1f);
        return Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap fixBitmap(Bitmap bitmap, boolean isFrontCamera) {
        Bitmap result = bitmap;

        if (isFrontCamera) {
            Matrix matrix = new Matrix();
            matrix.preScale(-1f, 1f);
            result = Bitmap.createBitmap(result, 0, 0,
                    result.getWidth(), result.getHeight(), matrix, true);
        }

        // Turun dari 1280 ke 640 — ini kunci utama pengurangan timeout
        result = resizeBitmap(result, MAX_SIZE, MAX_SIZE);
        return result;
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Turun dari quality 90 ke 70
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }
}