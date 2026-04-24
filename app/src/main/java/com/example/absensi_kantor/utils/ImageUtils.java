package com.example.absensi_kantor.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Base64;
import java.io.ByteArrayOutputStream;

public class ImageUtils {

    private static final int JPEG_QUALITY = 90;

    public static Bitmap resizeBitmap(Bitmap bitmap, int maxW, int maxH) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        if (w <= maxW && h <= maxH) return bitmap;

        float scale = Math.min((float) maxW / w, (float) maxH / h);
        int newW = Math.round(w * scale);
        int newH = Math.round(h * scale);

        return Bitmap.createScaledBitmap(bitmap, newW, newH, true);
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

        result = resizeBitmap(result, 1280, 1280); // ← naikan dari 1000 ke 1280

        return result;
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }
}