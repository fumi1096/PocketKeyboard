package com.zhangshang.keyboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/**
 * 触控板：单指移动光标、轻点左键、双指轻点右键、双指滑动滚轮、长按后拖动。
 * 光标速度、滚动速度、滚动方向都可由外部设置（MainActivity 的滑杆）。
 */
public class TrackpadView extends View {

    public interface MouseListener {
        void onMouse(int buttons, int dx, int dy, int wheel);
    }

    private static final int BUTTON_LEFT = 1;
    private static final int BUTTON_RIGHT = 2;

    private final Paint surfacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable holdRunnable = this::onHold;

    private final float density;
    private final float slop;
    private float sensitivity = 1.35f;
    private float scrollSpeed = 1.0f;
    private boolean reverseScroll = false;

    private MouseListener listener;
    private AreaStyle style = AreaStyle.from(AreaTheme.trackpadDefault(), null);
    private boolean drawBackground = true;

    private float lastX, lastY, downX, downY;
    private float accX, accY, scrollAcc, lastAvgY, twoFingerTravel;
    private long downTime, twoFingerDownTime;
    private boolean moved, dragging, twoFinger, twoFingerMoved;

    public TrackpadView(Context context, MouseListener listener) {
        super(context);
        this.listener = listener;
        density = getResources().getDisplayMetrics().density;
        slop = 8 * density;

        surfacePaint.setColor(0xFF151B24);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1 * density);
        borderPaint.setColor(0xFF2A3442);
        textPaint.setColor(0xFF7C8798);
        textPaint.setTextSize(12 * density);
        textPaint.setTextAlign(Paint.Align.CENTER);
        setFocusable(true);
    }

    public void setMouseListener(MouseListener l) {
        listener = l;
    }

    public void setStyle(AreaStyle s) {
        if (s == null) return;
        style = s;
        invalidate();
    }

    /** 由整页背景负责绘制底色/背景图时设为 false，触控板区域保持透明。 */
    public void setDrawOwnBackground(boolean draw) {
        if (drawBackground == draw) return;
        drawBackground = draw;
        invalidate();
    }

    /** 光标速度倍数，0.2 ~ 4.0。 */
    public void setSensitivity(float value) {
        sensitivity = Math.max(0.2f, Math.min(4.0f, value));
        invalidate();
    }

    /** 滚轮速度倍数，0.2 ~ 4.0（越大两指滑同样距离滚得越多）。 */
    public void setScrollSpeed(float value) {
        scrollSpeed = Math.max(0.2f, Math.min(4.0f, value));
    }

    /** 反转双指上下滚动的方向。 */
    public void setReverseScroll(boolean reverse) {
        reverseScroll = reverse;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float m = 12 * density;
        float radius = 16 * density;
        boolean hasImage = style.image != null;

        if (drawBackground) {
            if (hasImage) {
                // 图片模式：图片铺满整个触控区域
                drawCenterCrop(canvas, style.image);
            } else {
                canvas.drawColor(style.background);
            }
            if (style.scrimColor != 0) canvas.drawColor(style.scrimColor);
        }

        if (!hasImage) {
            surfacePaint.setColor(style.panel);
            borderPaint.setColor(style.panelBorder);
            canvas.drawRoundRect(m, m, w - m, h - m, radius, radius, surfacePaint);
            canvas.drawRoundRect(m, m, w - m, h - m, radius, radius, borderPaint);
        }

        // 提示文字画在最上层，图片模式下加反差阴影保证"透出来"又看得清
        textPaint.setColor(style.hintText);
        if (hasImage) {
            int shadow = AreaStyle.isLightColor(style.hintText) ? 0xCCFFFFFF : 0xCC000000;
            textPaint.setShadowLayer(5 * density, 0, 2 * density, shadow);
        } else {
            textPaint.clearShadowLayer();
        }
        float cx = w / 2f;
        float cy = h / 2f;
        canvas.drawText("单指移动光标　·　轻点=左键　·　双指轻点=右键", cx, cy - 8 * density, textPaint);
        canvas.drawText("双指滑动=滚轮　·　长按后拖动=拖拽", cx, cy + 14 * density, textPaint);
        textPaint.clearShadowLayer();
    }

    private void send(int buttons, int dx, int dy, int wheel) {
        if (listener != null) listener.onMouse(buttons, dx, dy, wheel);
    }

    private float avgY(MotionEvent e) {
        float sum = 0;
        for (int i = 0; i < e.getPointerCount(); i++) sum += e.getY(i);
        return sum / e.getPointerCount();
    }

    /** 长按未移动 → 按住左键进入拖拽。 */
    private void onHold() {
        if (!twoFinger && !moved && !dragging) {
            dragging = true;
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            send(BUTTON_LEFT, 0, 0, 0);
        }
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
        canvas.drawBitmap(bmp, null, new android.graphics.RectF(left, top, left + dw, top + dh), null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        long now = SystemClock.uptimeMillis();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                lastX = downX;
                lastY = downY;
                downTime = now;
                moved = false;
                dragging = false;
                twoFinger = false;
                twoFingerMoved = false;
                accX = accY = scrollAcc = 0;
                handler.postDelayed(holdRunnable, 450);
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                handler.removeCallbacks(holdRunnable);
                if (!moved) {
                    twoFinger = true;
                    twoFingerMoved = false;
                    twoFingerTravel = 0;
                    twoFingerDownTime = now;
                    lastAvgY = avgY(e);
                    scrollAcc = 0;
                }
                return true;

            case MotionEvent.ACTION_MOVE: {
                if (twoFinger && e.getPointerCount() >= 2) {
                    float avg = avgY(e);
                    float delta = lastAvgY - avg;
                    lastAvgY = avg;
                    twoFingerTravel += Math.abs(delta);
                    if (twoFingerTravel > slop) twoFingerMoved = true;
                    scrollAcc += delta;
                    float step = 26 * density / scrollSpeed;
                    int dir = reverseScroll ? -1 : 1;
                    while (scrollAcc >= step) {
                        send(0, 0, 0, dir);
                        scrollAcc -= step;
                        twoFingerMoved = true;
                    }
                    while (scrollAcc <= -step) {
                        send(0, 0, 0, -dir);
                        scrollAcc += step;
                        twoFingerMoved = true;
                    }
                } else if (!twoFinger) {
                    float x = e.getX();
                    float y = e.getY();
                    if (!moved && (Math.abs(x - downX) > slop || Math.abs(y - downY) > slop)) {
                        moved = true;
                        handler.removeCallbacks(holdRunnable);
                    }
                    if (moved) {
                        accX += (x - lastX) * sensitivity;
                        accY += (y - lastY) * sensitivity;
                        int ix = (int) accX;
                        int iy = (int) accY;
                        if (ix != 0 || iy != 0) {
                            send(dragging ? BUTTON_LEFT : 0, ix, iy, 0);
                            accX -= ix;
                            accY -= iy;
                        }
                    }
                    lastX = x;
                    lastY = y;
                }
                return true;
            }

            case MotionEvent.ACTION_POINTER_UP:
                if (e.getPointerCount() - 1 < 2) {
                    // 只剩一根手指时不要清掉 twoFinger 标记：
                    // 双指轻点的最后一根抬起时还要据此判定"右键"
                    int idx = e.getActionIndex() == 0 ? 1 : 0;
                    if (idx < e.getPointerCount()) {
                        lastX = e.getX(idx);
                        lastY = e.getY(idx);
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(holdRunnable);
                if (dragging) {
                    send(0, 0, 0, 0);
                    dragging = false;
                } else if (twoFinger) {
                    if (!twoFingerMoved && now - twoFingerDownTime < 350) {
                        rightClick();
                    }
                } else if (!moved && now - downTime < 300) {
                    leftClick();
                }
                twoFinger = false;
                return true;

            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(holdRunnable);
                if (dragging) {
                    dragging = false;
                    send(0, 0, 0, 0);
                }
                twoFinger = false;
                return true;

            default:
                return super.onTouchEvent(e);
        }
    }

    private void leftClick() {
        send(BUTTON_LEFT, 0, 0, 0);
        handler.postDelayed(() -> send(0, 0, 0, 0), 60);
    }

    private void rightClick() {
        send(BUTTON_RIGHT, 0, 0, 0);
        handler.postDelayed(() -> send(0, 0, 0, 0), 60);
    }

    /** 切走/退到后台时松掉所有按键，避免电脑端鼠标一直按着。 */
    public void reset() {
        handler.removeCallbacksAndMessages(null);
        dragging = false;
        twoFinger = false;
        moved = false;
        // 无条件补一帧全松开：防止点击的"松开"回调被取消后按键卡在按下状态
        send(0, 0, 0, 0);
    }
}
