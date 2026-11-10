package com.zhangshang.keyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.SparseIntArray;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/**
 * 实验性虚拟手柄：左边方向键 + 左摇杆，右边 ABXY + 右摇杆，肩键，Start/Back。
 * 发送手柄报文（Report ID 3）。
 */
public class GamepadView extends View {

    public interface Listener {
        void onGamepad(int buttons, int lx, int ly, int rx, int ry, int hat);
    }

    private static final int BTN_A = 1;
    private static final int BTN_B = 1 << 1;
    private static final int BTN_X = 1 << 2;
    private static final int BTN_Y = 1 << 3;
    private static final int BTN_LB = 1 << 4;
    private static final int BTN_RB = 1 << 5;
    private static final int BTN_LT = 1 << 6;
    private static final int BTN_RT = 1 << 7;
    private static final int BTN_BACK = 1 << 8;
    private static final int BTN_START = 1 << 9;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint halosPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final float density;
    private final RectF lbRect = new RectF();
    private final RectF rbRect = new RectF();
    private final RectF ltRect = new RectF();
    private final RectF rtRect = new RectF();
    private final RectF backRect = new RectF();
    private final RectF startRect = new RectF();

    private AreaStyle style = AreaStyle.from(AreaTheme.keyboardDefault(), null);
    private Listener listener;

    private int buttons;
    private int hat = 8;
    private float lx, ly, rx, ry;

    private float lcx, lcy, rcx, rcy, stickR;
    private float dcx, dcy, dh;
    private float acx, acy, abR;

    private int leftStickId = -1;
    private int rightStickId = -1;
    private int dpadId = -1;
    private final SparseIntArray pointerButtons = new SparseIntArray();

    public GamepadView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        density = getResources().getDisplayMetrics().density;
        textPaint.setTextAlign(Paint.Align.CENTER);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2 * density);
        halosPaint.setStyle(Paint.Style.FILL);
        setFocusable(true);
    }

    public void setStyle(AreaStyle s) {
        if (s == null) return;
        style = s;
        invalidate();
    }

    public void reset() {
        buttons = 0;
        hat = 8;
        lx = ly = rx = ry = 0;
        leftStickId = rightStickId = dpadId = -1;
        pointerButtons.clear();
        send();
        invalidate();
    }

    private void send() {
        if (listener == null) return;
        listener.onGamepad(buttons,
                Math.round(lx * 127f), Math.round(ly * 127f),
                Math.round(rx * 127f), Math.round(ry * 127f), hat);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float base = Math.min(w, h);
        stickR = base * 0.13f;
        lcx = w * 0.15f;
        lcy = h * 0.62f;
        rcx = w * 0.85f;
        rcy = h * 0.62f;
        dcx = w * 0.37f;
        dcy = h * 0.60f;
        dh = base * 0.17f;
        acx = w * 0.63f;
        acy = h * 0.60f;
        abR = base * 0.17f;

        float bw = w * 0.10f;
        float bh = h * 0.11f;
        lbRect.set(w * 0.035f, h * 0.05f, w * 0.035f + bw, h * 0.05f + bh);
        rbRect.set(w * 0.965f - bw, h * 0.05f, w * 0.965f, h * 0.05f + bh);
        ltRect.set(lbRect.right + dp(4), h * 0.05f, lbRect.right + dp(4) + bw, h * 0.05f + bh);
        rtRect.set(rbRect.left - dp(4) - bw, h * 0.05f, rbRect.left - dp(4), h * 0.05f + bh);

        float smallW = w * 0.055f;
        float smallH = h * 0.075f;
        backRect.set(w * 0.45f - smallW / 2f, h * 0.88f - smallH / 2f,
                w * 0.45f + smallW / 2f, h * 0.88f + smallH / 2f);
        startRect.set(w * 0.55f - smallW / 2f, h * 0.88f - smallH / 2f,
                w * 0.55f + smallW / 2f, h * 0.88f + smallH / 2f);
    }

    private float dp(float v) {
        return v * density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        fillPaint.setColor(style.keyNormal);
        strokePaint.setColor(style.panelBorder);
        textPaint.setColor(style.label);
        textPaint.setTextSize(13 * density);

        drawStick(canvas, lcx, lcy, lx, ly, "L");
        drawStick(canvas, rcx, rcy, rx, ry, "R");
        drawDpad(canvas);
        drawAbxy(canvas);

        drawButton(canvas, lbRect, "LB", (buttons & BTN_LB) != 0);
        drawButton(canvas, rbRect, "RB", (buttons & BTN_RB) != 0);
        drawButton(canvas, ltRect, "LT", (buttons & BTN_LT) != 0);
        drawButton(canvas, rtRect, "RT", (buttons & BTN_RT) != 0);
        drawButton(canvas, backRect, "Back", (buttons & BTN_BACK) != 0);
        drawButton(canvas, startRect, "Start", (buttons & BTN_START) != 0);
    }

    private void drawStick(Canvas canvas, float cx, float cy, float x, float y, String label) {
        halosPaint.setColor(style.keyNormal);
        canvas.drawCircle(cx, cy, stickR, halosPaint);
        canvas.drawCircle(cx, cy, stickR, strokePaint);
        float kx = cx + x * stickR * 0.55f;
        float ky = cy + y * stickR * 0.55f;
        halosPaint.setColor(style.keyModifier);
        canvas.drawCircle(kx, ky, stickR * 0.45f, halosPaint);
        canvas.drawCircle(kx, ky, stickR * 0.45f, strokePaint);
        textPaint.setColor(style.subLabel);
        textPaint.setTextSize(10 * density);
        canvas.drawText(label, cx, cy + stickR + 14 * density, textPaint);
        textPaint.setColor(style.label);
        textPaint.setTextSize(13 * density);
    }

    private void drawDpad(Canvas canvas) {
        float t = dh * 0.42f;
        float cx = dcx;
        float cy = dcy;
        drawButton(canvas, new RectF(cx - t, cy - dh, cx + t, cy - dh + dh * 0.6f), "▲", hat == 0);
        drawButton(canvas, new RectF(cx - t, cy + dh - dh * 0.6f, cx + t, cy + dh), "▼", hat == 4);
        drawButton(canvas, new RectF(cx - dh, cy - t, cx - dh + dh * 0.6f, cy + t), "◀", hat == 6);
        drawButton(canvas, new RectF(cx + dh - dh * 0.6f, cy - t, cx + dh, cy + t), "▶", hat == 2);
    }

    private void drawAbxy(Canvas canvas) {
        float r = abR * 0.42f;
        drawCircleButton(canvas, acx, acy + abR, r, "A", (buttons & BTN_A) != 0);
        drawCircleButton(canvas, acx + abR, acy, r, "B", (buttons & BTN_B) != 0);
        drawCircleButton(canvas, acx - abR, acy, r, "X", (buttons & BTN_X) != 0);
        drawCircleButton(canvas, acx, acy - abR, r, "Y", (buttons & BTN_Y) != 0);
    }

    private void drawButton(Canvas canvas, RectF r, String label, boolean pressed) {
        fillPaint.setColor(pressed ? style.keyPressed : style.keyNormal);
        float rad = 10 * density;
        canvas.drawRoundRect(r, rad, rad, fillPaint);
        canvas.drawRoundRect(r, rad, rad, strokePaint);
        float size = Math.min(r.height() * 0.42f, 13 * density);
        textPaint.setTextSize(size);
        textPaint.setColor(style.label);
        canvas.drawText(label, r.centerX(), r.centerY() + size * 0.35f, textPaint);
    }

    private void drawCircleButton(Canvas canvas, float cx, float cy, float r, String label, boolean pressed) {
        halosPaint.setColor(pressed ? style.keyPressed : style.keyNormal);
        canvas.drawCircle(cx, cy, r, halosPaint);
        canvas.drawCircle(cx, cy, r, strokePaint);
        float size = r * 0.9f;
        textPaint.setTextSize(size);
        textPaint.setColor(style.label);
        canvas.drawText(label, cx, cy + size * 0.35f, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int idx = e.getActionIndex();
                handleDown(e.getPointerId(idx), e.getX(idx), e.getY(idx));
                send();
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < e.getPointerCount(); i++) {
                    int id = e.getPointerId(i);
                    float x = e.getX(i);
                    float y = e.getY(i);
                    if (id == leftStickId) updateStick(true, x, y);
                    else if (id == rightStickId) updateStick(false, x, y);
                    else if (id == dpadId) updateHat(x, y);
                }
                send();
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int idx = e.getActionIndex();
                handleUp(e.getPointerId(idx));
                send();
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                reset();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    private void handleDown(int id, float x, float y) {
        if (dist(x, y, lcx, lcy) <= stickR * 1.25f) {
            leftStickId = id;
            updateStick(true, x, y);
            return;
        }
        if (dist(x, y, rcx, rcy) <= stickR * 1.25f) {
            rightStickId = id;
            updateStick(false, x, y);
            return;
        }
        if (Math.abs(x - dcx) <= dh && Math.abs(y - dcy) <= dh) {
            dpadId = id;
            updateHat(x, y);
            return;
        }
        int bit = hitButton(x, y);
        if (bit != 0) {
            buttons |= bit;
            pointerButtons.put(id, bit);
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    private void handleUp(int id) {
        if (id == leftStickId) {
            leftStickId = -1;
            lx = ly = 0;
        } else if (id == rightStickId) {
            rightStickId = -1;
            rx = ry = 0;
        } else if (id == dpadId) {
            dpadId = -1;
            hat = 8;
        } else {
            int bit = pointerButtons.get(id, 0);
            if (bit != 0) {
                buttons &= ~bit;
                pointerButtons.delete(id);
            }
        }
    }

    private int hitButton(float x, float y) {
        float r = abR * 0.42f;
        if (dist(x, y, acx, acy + abR) <= r) return BTN_A;
        if (dist(x, y, acx + abR, acy) <= r) return BTN_B;
        if (dist(x, y, acx - abR, acy) <= r) return BTN_X;
        if (dist(x, y, acx, acy - abR) <= r) return BTN_Y;
        if (lbRect.contains(x, y)) return BTN_LB;
        if (rbRect.contains(x, y)) return BTN_RB;
        if (ltRect.contains(x, y)) return BTN_LT;
        if (rtRect.contains(x, y)) return BTN_RT;
        if (backRect.contains(x, y)) return BTN_BACK;
        if (startRect.contains(x, y)) return BTN_START;
        return 0;
    }

    private void updateStick(boolean left, float x, float y) {
        float cx = left ? lcx : rcx;
        float cy = left ? lcy : rcy;
        float nx = (x - cx) / stickR;
        float ny = (y - cy) / stickR;
        float len = (float) Math.sqrt(nx * nx + ny * ny);
        if (len > 1f) {
            nx /= len;
            ny /= len;
        }
        if (left) {
            lx = nx;
            ly = ny;
        } else {
            rx = nx;
            ry = ny;
        }
    }

    private void updateHat(float x, float y) {
        float dx = x - dcx;
        float dy = y - dcy;
        if (Math.abs(dx) < dh * 0.28f && Math.abs(dy) < dh * 0.28f) {
            hat = 8;
        } else if (Math.abs(dx) > Math.abs(dy)) {
            hat = dx > 0 ? 2 : 6;
        } else {
            hat = dy > 0 ? 4 : 0;
        }
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
