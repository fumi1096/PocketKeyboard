package com.zhangshang.keyboard;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * 整页背景：把键盘区域的背景色 / 背景图 + 压暗提亮遮罩铺满整个窗口，
 * 上方状态栏、快捷栏、键盘区都会显示同一张背景。
 */
public class PageBackgroundDrawable extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private AreaStyle style;

    public void setStyle(AreaStyle s) {
        style = s;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (style == null) {
            canvas.drawColor(0xFF0C0F14);
            return;
        }
        Bitmap image = style.image;
        if (image != null && !image.isRecycled()
                && image.getWidth() > 0 && image.getHeight() > 0) {
            float vw = b.width();
            float vh = b.height();
            float iw = image.getWidth();
            float ih = image.getHeight();
            float scale = Math.max(vw / iw, vh / ih);
            float dw = iw * scale;
            float dh = ih * scale;
            float left = b.left + (vw - dw) / 2f;
            float top = b.top + (vh - dh) / 2f;
            canvas.drawBitmap(image, null, new RectF(left, top, left + dw, top + dh), paint);
        } else {
            canvas.drawColor(style.background);
        }
        if (style.scrimColor != 0) canvas.drawColor(style.scrimColor);
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
