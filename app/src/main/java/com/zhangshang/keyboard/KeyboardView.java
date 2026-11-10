package com.zhangshang.keyboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 自绘键盘：支持多指同时按住（组合键）、修饰键点按锁定（粘滞键）、
 * 长按交给主机自动连发（HID 键保持按下时主机系统会重复）。
 */
public class KeyboardView extends View {

    public interface ReportListener {
        void onReport(byte[] report);
    }

    private static final float COLS = KeyLayout.COLS;
    private static final float ROWS = KeyLayout.ROWS;

    private final Paint keyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private final KeyLayout layout = KeyLayout.create();
    private final Map<Integer, KeyInfo> pointerKeys = new LinkedHashMap<>();
    private final Set<KeyInfo> pressed = new LinkedHashSet<>();
    private final Map<KeyInfo, Long> modifierDownAt = new HashMap<>();
    private final Set<KeyInfo> modifierTapEligible = new HashSet<>();
    private int latchBits = 0;

    private float unitW;
    private float unitH;
    private float gap;
    private float pad;
    private final float density;
    private float maxLabelSize;
    private float maxSubSize;

    private ReportListener listener;
    private AreaStyle style = AreaStyle.from(AreaTheme.keyboardDefault(), null);
    private boolean drawBackground = true;

    public KeyboardView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        gap = 3 * density;
        pad = 4 * density;
        labelPaint.setColor(0xFFE8EDF4);
        subPaint.setColor(0xFF97A3B4);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        subPaint.setTextAlign(Paint.Align.CENTER);
        setFocusable(true);
    }

    public void setReportListener(ReportListener l) {
        listener = l;
    }

    public void setStyle(AreaStyle s) {
        if (s == null) return;
        style = s;
        invalidate();
    }

    /** 由整页背景负责绘制底色/背景图时设为 false，键盘区保持透明。 */
    public void setDrawOwnBackground(boolean draw) {
        if (drawBackground == draw) return;
        drawBackground = draw;
        setBackground(null);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        unitW = (w - pad * 2) / COLS;
        unitH = (h - pad * 2) / ROWS;
        maxLabelSize = Math.min(unitH * 0.40f, 15 * density);
        maxSubSize = Math.min(unitH * 0.22f, 9 * density);
    }

    private RectF rectOf(KeyInfo k) {
        rect.set(pad + k.col * unitW,
                pad + k.row * unitH,
                pad + k.col * unitW + k.w * unitW - gap,
                pad + k.row * unitH + k.h * unitH - gap);
        return rect;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (drawBackground) {
            if (style.image != null) {
                drawCenterCrop(canvas, style.image);
            } else {
                canvas.drawColor(style.background);
            }
            if (style.scrimColor != 0) canvas.drawColor(style.scrimColor);
        }
        labelPaint.setColor(style.label);
        subPaint.setColor(style.subLabel);
        for (KeyInfo k : layout.keys) {
            RectF r = rectOf(k);
            boolean latched = k.isModifier() && (latchBits & HidKeys.modifierBit(k.usage)) != 0;
            boolean down = pressed.contains(k);
            if (latched) keyPaint.setColor(style.keyLatched);
            else if (down) keyPaint.setColor(style.keyPressed);
            else if (k.isModifier()) keyPaint.setColor(style.keyModifier);
            else keyPaint.setColor(style.keyNormal);

            float radius = 7 * density;
            canvas.drawRoundRect(r, radius, radius, keyPaint);

            // 文字按键帽可用宽度自动缩小，并裁剪在键帽内 ——
            // 避免 Backspace / Ins / PgUp / Enter 这类较长的字压到相邻键上
            float availW = Math.max(1f, r.width() - 12 * density);
            float labelSize = fitTextSize(labelPaint, k.label, maxLabelSize, availW, 5.5f * density);
            float cx = r.centerX();
            float cy = r.centerY();

            canvas.save();
            canvas.clipRect(r.left + 1.5f * density, r.top + 1, r.right - 1.5f * density, r.bottom - 1);

            if (k.sub != null) {
                float subSize = fitTextSize(subPaint, k.sub, maxSubSize, availW, 4.5f * density);
                float gapPx = 2 * density;
                float labelH = labelSize * 0.86f;
                float subH = subSize * 0.86f;
                float top = cy - (labelH + gapPx + subH) / 2f;
                canvas.drawText(k.label, cx, top + labelH, labelPaint);
                canvas.drawText(k.sub, cx, top + labelH + gapPx + subH, subPaint);
            } else {
                canvas.drawText(k.label, cx, cy + labelSize * 0.35f, labelPaint);
            }
            canvas.restore();
        }
    }

    /** 在 maxWidth 内放得下文字：必要时等比缩小字号，最小到 minSize。 */
    private float fitTextSize(Paint paint, String text, float maxSize, float maxWidth, float minSize) {
        if (text == null || text.isEmpty()) return maxSize;
        paint.setTextSize(maxSize);
        float w = paint.measureText(text);
        if (w <= 0 || w <= maxWidth) return maxSize;
        float size = Math.max(minSize, maxSize * (maxWidth / w));
        // 关键：把缩小后的字号真正应用回画笔，否则仍按原字号绘制会被键帽裁掉
        paint.setTextSize(size);
        return size;
    }

    /** 背景图按"居中裁剪铺满"绘制。 */
    private void drawCenterCrop(Canvas canvas, Bitmap bmp) {
        if (bmp == null || bmp.isRecycled()) return;
        float vw = getWidth();
        float vh = getHeight();
        float iw = bmp.getWidth();
        float ih = bmp.getHeight();
        if (iw <= 0 || ih <= 0 || vw <= 0 || vh <= 0) return;
        float scale = Math.max(vw / iw, vh / ih);
        float dw = iw * scale;
        float dh = ih * scale;
        float left = (vw - dw) / 2f;
        float top = (vh - dh) / 2f;
        RectF dst = new RectF(left, top, left + dw, top + dh);
        canvas.drawBitmap(bmp, null, dst, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int idx = e.getActionIndex();
                KeyInfo k = keyAt(e.getX(idx), e.getY(idx));
                pointerKeys.put(e.getPointerId(idx), k);
                if (k != null) press(k);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < e.getPointerCount(); i++) {
                    int id = e.getPointerId(i);
                    KeyInfo now = keyAt(e.getX(i), e.getY(i));
                    KeyInfo old = pointerKeys.get(id);
                    if (now != old) {
                        pointerKeys.put(id, now);
                        if (old != null) release(old);
                        if (now != null) press(now);
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int idx = e.getActionIndex();
                KeyInfo k = pointerKeys.remove(e.getPointerId(idx));
                if (k != null) release(k);
                return true;
            }
            case MotionEvent.ACTION_CANCEL: {
                List<KeyInfo> all = new ArrayList<>(pointerKeys.values());
                pointerKeys.clear();
                for (KeyInfo k : all) if (k != null) release(k);
                return true;
            }
            default:
                return super.onTouchEvent(e);
        }
    }

    private KeyInfo keyAt(float x, float y) {
        for (KeyInfo k : layout.keys) {
            RectF r = rectOf(k);
            if (x >= r.left && x <= r.right && y >= r.top && y <= r.bottom) return k;
        }
        return null;
    }

    private void press(KeyInfo k) {
        if (!pressed.add(k)) return;
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        if (k.isModifier()) {
            modifierDownAt.put(k, SystemClock.uptimeMillis());
            modifierTapEligible.add(k);
        } else {
            // 按住修饰键期间又按了普通键 → 这次不算"点按锁定"
            modifierTapEligible.clear();
        }
        send();
        invalidate();
    }

    private void release(KeyInfo k) {
        pressed.remove(k);
        if (k.isModifier()) {
            Long t = modifierDownAt.remove(k);
            boolean eligible = modifierTapEligible.remove(k);
            // 短按修饰键单独点一下 → 锁定（粘滞键）
            if (t != null && eligible && SystemClock.uptimeMillis() - t <= 400) {
                latchBits ^= HidKeys.modifierBit(k.usage);
            }
        } else if (latchBits != 0) {
            boolean stillHoldingNonMod = false;
            for (KeyInfo p : pressed) {
                if (!p.isModifier()) {
                    stillHoldingNonMod = true;
                    break;
                }
            }
            if (!stillHoldingNonMod) latchBits = 0;
        }
        send();
        invalidate();
    }

    private void send() {
        int mod = latchBits;
        byte[] keys = new byte[6];
        int n = 0;
        boolean[] seen = new boolean[0x100];
        for (KeyInfo k : pressed) {
            if (k.isModifier()) {
                mod |= HidKeys.modifierBit(k.usage);
            } else if (n < 6 && !seen[k.usage]) {
                seen[k.usage] = true;
                keys[n++] = (byte) k.usage;
            }
        }
        byte[] report = new byte[8];
        report[0] = (byte) mod;
        report[1] = 0;
        System.arraycopy(keys, 0, report, 2, 6);
        if (listener != null) listener.onReport(report);
    }

    /** 页面退到后台时清空按键，避免主机端一直显示"按住"。 */
    public void releaseAll() {
        pointerKeys.clear();
        pressed.clear();
        modifierDownAt.clear();
        modifierTapEligible.clear();
        latchBits = 0;
        send();
        invalidate();
    }
}
