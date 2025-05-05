package com.example.kimlikuygulamasi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

public class IdCardOverlayView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public IdCardOverlayView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(0xCCFFFFFF);
    }

    @Override
    protected void onDraw(@NonNull Canvas c) {
        super.onDraw(c);
        Rect r = getCropRect();
        c.drawRect(r, paint);
    }

    /**
     * Rehberdeki (ekranda görünen) kart bölgesinin koordinatlarını verir
     * (view koordinatlarında).
     */
    public Rect getCropRect() {
        int w = getWidth(), h = getHeight();
        int targetW, targetH;

        // gerçek kart oranı
        float cardAspectRatio = 85.6f / 54.0f;
        if ((float) w / h > cardAspectRatio) {
            // görüntü yatay daha geniş, yükseklik bazlı kırp
            targetH = (int) (h * 0.7f);
            targetW = (int) (targetH * cardAspectRatio);
        } else {
            // görüntü dikey daha uzun, genişlik bazlı kırp
            targetW = (int) (w * 0.85f);
            targetH = (int) (targetW / cardAspectRatio);
        }

        int left = (w - targetW) / 2;
        int top  = (h - targetH) / 2;
        return new Rect(left, top, left + targetW, top + targetH);
    }
}
