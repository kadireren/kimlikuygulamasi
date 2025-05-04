package com.example.kimlikuygulamasi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

public class IdCardOverlayView extends View {
    private Paint transparentPaint;
    private Paint borderPaint;
    private Paint cornerPaint; // Sınıf seviyesine taşındı
    private RectF cardRect;

    // Turkish ID card dimensions in mm (width x height)
    private static final float ID_CARD_WIDTH_MM = 85.6f;
    private static final float ID_CARD_HEIGHT_MM = 54.0f;

    // Aspect ratio of Turkish ID card
    private static final float ID_CARD_ASPECT_RATIO = ID_CARD_WIDTH_MM / ID_CARD_HEIGHT_MM;

    public IdCardOverlayView(Context context) {
        super(context);
        init();
    }

    public IdCardOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public IdCardOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Paint for the transparent card cutout
        transparentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        transparentPaint.setColor(Color.TRANSPARENT);
        transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        // Paint for the border
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStrokeWidth(5);

        // Paint for corners - init() içinde tanımlandı
        cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setColor(Color.WHITE);
        cornerPaint.setStrokeWidth(5);

        // Initialize the rectangle (will be updated in onSizeChanged)
        cardRect = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Calculate the card rectangle size based on screen dimensions
        // while maintaining the ID card aspect ratio
        float cardWidth;
        float cardHeight;

        if ((float) w / h > ID_CARD_ASPECT_RATIO) {
            // Screen is wider than ID card aspect ratio
            cardWidth = h * ID_CARD_ASPECT_RATIO * 0.7f; // Use 70% of height factoring in aspect ratio
            cardHeight = h * 0.7f; // Use 70% of height
        } else {
            // Screen is narrower than ID card aspect ratio
            cardWidth = w * 0.85f; // Use 85% of width
            cardHeight = w / ID_CARD_ASPECT_RATIO * 0.85f; // Use width factoring in aspect ratio
        }

        // Center the rectangle
        float left = (w - cardWidth) / 2;
        float top = (h - cardHeight) / 2;
        cardRect.set(left, top, left + cardWidth, top + cardHeight);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Need to create a layer for the transparency effect
        int sc = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);

        // Draw a semi-transparent overlay
        canvas.drawColor(Color.parseColor("#80000000")); // 50% transparent black

        // Draw the transparent ID card cutout
        canvas.drawRoundRect(cardRect, 20, 20, transparentPaint);

        // Draw the border
        canvas.drawRoundRect(cardRect, 20, 20, borderPaint);

        // Draw corner markers
        float cornerSize = 40;
        // Her çizim işleminde cornerPaint nesnesinin yeniden oluşturulması kaldırıldı
        cornerPaint.setStrokeWidth(5); // Gerekirse stroke width güncellenebilir

        // Top-left corner
        canvas.drawLine(cardRect.left, cardRect.top, cardRect.left + cornerSize, cardRect.top, cornerPaint);
        canvas.drawLine(cardRect.left, cardRect.top, cardRect.left, cardRect.top + cornerSize, cornerPaint);

        // Top-right corner
        canvas.drawLine(cardRect.right - cornerSize, cardRect.top, cardRect.right, cardRect.top, cornerPaint);
        canvas.drawLine(cardRect.right, cardRect.top, cardRect.right, cardRect.top + cornerSize, cornerPaint);

        // Bottom-left corner
        canvas.drawLine(cardRect.left, cardRect.bottom - cornerSize, cardRect.left, cardRect.bottom, cornerPaint);
        canvas.drawLine(cardRect.left, cardRect.bottom, cardRect.left + cornerSize, cardRect.bottom, cornerPaint);

        // Bottom-right corner
        canvas.drawLine(cardRect.right - cornerSize, cardRect.bottom, cardRect.right, cardRect.bottom, cornerPaint);
        canvas.drawLine(cardRect.right, cardRect.bottom - cornerSize, cardRect.right, cardRect.bottom, cornerPaint);

        canvas.restoreToCount(sc);
    }

    // Method to get the card rectangle for cropping
    public RectF getCardRect() {
        return cardRect;
    }
}