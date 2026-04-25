package com.example.absensi_kantor.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.List;

public class FaceOverlayView extends View {

    private Paint paintKotak;
    private Paint paintTitik;
    private Paint paintGaris;
    private List<Face> faces;

    private int imageWidth = 0;
    private int imageHeight = 0;
    private boolean isFrontCamera = true;

    // Gunakan RectF global untuk menghindari pembuatan objek baru setiap kali onDraw dipanggil
    private final RectF rectWajah = new RectF();

    public FaceOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintKotak = new Paint();
        paintKotak.setColor(Color.parseColor("#00E676"));
        paintKotak.setStyle(Paint.Style.STROKE);
        paintKotak.setStrokeWidth(4f);
        paintKotak.setAntiAlias(true); // Biar garis lebih halus

        paintTitik = new Paint();
        paintTitik.setColor(Color.parseColor("#FF4081"));
        paintTitik.setStyle(Paint.Style.FILL);
        paintTitik.setAntiAlias(true);

        paintGaris = new Paint();
        paintGaris.setColor(Color.parseColor("#40C4FF"));
        paintGaris.setStyle(Paint.Style.STROKE);
        paintGaris.setStrokeWidth(2f);
        paintGaris.setAntiAlias(true);
    }

    public void setFrontCamera(boolean frontCamera) {
        this.isFrontCamera = frontCamera;
    }

    public void setFaces(List<Face> faces, int imageWidth, int imageHeight) {
        this.faces = faces;
        // KameraX Portrait: rotasi 90°, tukar width & height
        this.imageWidth = imageHeight;
        this.imageHeight = imageWidth;
        postInvalidate(); // Lebih aman untuk update UI dari background thread
    }

    public void clear() {
        this.faces = null;
        postInvalidate();
    }

    private Face ambilWajahTerbesar(List<Face> faces) {
        if (faces == null || faces.isEmpty()) return null;
        Face terbesar = null;
        long luasTerbesar = 0;
        for (Face face : faces) {
            Rect b = face.getBoundingBox();
            long luas = (long) b.width() * b.height();
            if (luas > luasTerbesar) {
                luasTerbesar = luas;
                terbesar = face;
            }
        }
        return terbesar;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (faces == null || faces.isEmpty() || imageWidth == 0 || imageHeight == 0) return;

        float viewW = getWidth();
        float viewH = getHeight();
        float scaleX = viewW / imageWidth;
        float scaleY = viewH / imageHeight;

        Face face = ambilWajahTerbesar(faces);
        if (face == null) return;

        // ── Kotak wajah ──
        Rect bounds = face.getBoundingBox();
        if (isFrontCamera) {
            rectWajah.left = viewW - (bounds.right * scaleX);
            rectWajah.right = viewW - (bounds.left * scaleX);
        } else {
            rectWajah.left = bounds.left * scaleX;
            rectWajah.right = bounds.right * scaleX;
        }
        rectWajah.top = bounds.top * scaleY;
        rectWajah.bottom = bounds.bottom * scaleY;

        canvas.drawRect(rectWajah, paintKotak);

        // ── Titik landmark ──
        int[] landmarks = {
                FaceLandmark.LEFT_EYE, FaceLandmark.RIGHT_EYE, FaceLandmark.NOSE_BASE,
                FaceLandmark.MOUTH_LEFT, FaceLandmark.MOUTH_RIGHT, FaceLandmark.MOUTH_BOTTOM,
                FaceLandmark.LEFT_EAR, FaceLandmark.RIGHT_EAR, FaceLandmark.LEFT_CHEEK, FaceLandmark.RIGHT_CHEEK
        };

        for (int type : landmarks) {
            gambarTitik(canvas, face, type, scaleX, scaleY, viewW);
        }

        // ── Garis ──
        gambarGarisAntara(canvas, face, FaceLandmark.LEFT_EYE, FaceLandmark.NOSE_BASE, scaleX, scaleY, viewW);
        gambarGarisAntara(canvas, face, FaceLandmark.RIGHT_EYE, FaceLandmark.NOSE_BASE, scaleX, scaleY, viewW);
        gambarGarisAntara(canvas, face, FaceLandmark.MOUTH_LEFT, FaceLandmark.MOUTH_RIGHT, scaleX, scaleY, viewW);
    }

    private float mirrorX(float x, float scaleX, float viewW) {
        return isFrontCamera ? viewW - (x * scaleX) : x * scaleX;
    }

    private void gambarTitik(Canvas canvas, Face face, int landmarkType, float scaleX, float scaleY, float viewW) {
        FaceLandmark landmark = face.getLandmark(landmarkType);
        if (landmark == null) return;
        float x = mirrorX(landmark.getPosition().x, scaleX, viewW);
        float y = landmark.getPosition().y * scaleY;
        canvas.drawCircle(x, y, 6f, paintTitik);
    }

    private void gambarGarisAntara(Canvas canvas, Face face, int dari, int ke, float scaleX, float scaleY, float viewW) {
        FaceLandmark lmDari = face.getLandmark(dari);
        FaceLandmark lmKe = face.getLandmark(ke);
        if (lmDari == null || lmKe == null) return;
        canvas.drawLine(
                mirrorX(lmDari.getPosition().x, scaleX, viewW),
                lmDari.getPosition().y * scaleY,
                mirrorX(lmKe.getPosition().x, scaleX, viewW),
                lmKe.getPosition().y * scaleY,
                paintGaris
        );
    }
}