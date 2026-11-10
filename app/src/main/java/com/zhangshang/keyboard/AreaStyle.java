package com.zhangshang.keyboard;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * 由 AreaTheme 计算出的可直接绘制的配色。
 * 背景图、遮罩、按键颜色/透明度都在这里落成具体 ARGB。
 */
public final class AreaStyle {

    public final int background;
    public final int keyNormal;
    public final int keyModifier;
    public final int keyPressed;
    public final int keyLatched;
    public final int label;
    public final int subLabel;
    public final int panel;
    public final int panelBorder;
    public final int hintText;
    public final int scrimColor;
    public final Bitmap image;

    private AreaStyle(int background, int keyNormal, int keyModifier, int keyPressed, int keyLatched,
                      int label, int subLabel, int panel, int panelBorder, int hintText,
                      int scrimColor, Bitmap image) {
        this.background = background;
        this.keyNormal = keyNormal;
        this.keyModifier = keyModifier;
        this.keyPressed = keyPressed;
        this.keyLatched = keyLatched;
        this.label = label;
        this.subLabel = subLabel;
        this.panel = panel;
        this.panelBorder = panelBorder;
        this.hintText = hintText;
        this.scrimColor = scrimColor;
        this.image = image;
    }

    public static AreaStyle from(AreaTheme t, Bitmap image) {
        int bg = Color.HSVToColor(new float[]{t.bgHue, c01(t.bgSat), c01(t.bgVal)});

        int keyRgb = Color.HSVToColor(new float[]{t.keyHue, c01(t.keySat), c01(t.keyVal)});
        int alpha = Math.round(c01(t.keyAlpha) * 255f);
        int key = withAlpha(keyRgb, alpha);

        int modifier = withAlpha(shiftValue(keyRgb, isLight(keyRgb) ? -0.10f : 0.08f), alpha);
        int pressed = isLight(keyRgb) ? 0xFFB9CEFB : 0xFF3B7BFF;
        int latched = isLight(keyRgb) ? 0xFFF0CE8A : 0xFFC08A2E;

        // 有背景图时，文字颜色按图片平均明暗来定，避免图片亮/暗时看不清
        int baseForText = bg;
        int hint;
        if (image != null) {
            float lum = averageLuminance(image);
            baseForText = lum > 0.55f ? 0xFFF2F5F9 : 0xFF0C0F14;
            hint = lum > 0.55f ? 0xFF1D222A : 0xFFF4F6FA;
        } else {
            hint = isLight(bg) ? 0xFF4C5663 : 0xFF7C8798;
        }

        int blendedKey = blend(baseForText, keyRgb, alpha / 255f);
        int label = isLight(blendedKey) ? 0xFF1D222A : 0xFFE8EDF4;
        int sub = isLight(blendedKey) ? 0xFF5B6572 : 0xFF97A3B4;

        int panel = withAlpha(shiftValue(keyRgb, -0.02f), alpha);
        int panelBorder = withAlpha(shiftValue(keyRgb, isLight(keyRgb) ? -0.14f : 0.14f), alpha);

        int scrim = 0;
        float net = c01(t.lighten) - c01(t.darken);
        if (net > 0.001f) {
            scrim = Color.argb(Math.round(Math.min(1f, net) * 220f), 255, 255, 255);
        } else if (net < -0.001f) {
            scrim = Color.argb(Math.round(Math.min(1f, -net) * 220f), 0, 0, 0);
        }

        return new AreaStyle(bg, key, modifier, pressed, latched, label, sub,
                panel, panelBorder, hint, scrim, image);
    }

    /** 粗略估计图片平均亮度（每 16 个像素取一个点），用于自动选浅色/深色文字。 */
    private static float averageLuminance(Bitmap bmp) {
        try {
            int w = bmp.getWidth();
            int h = bmp.getHeight();
            if (w <= 0 || h <= 0) return 0.3f;
            long sum = 0;
            int count = 0;
            int stepX = Math.max(1, w / 48);
            int stepY = Math.max(1, h / 48);
            for (int y = 0; y < h; y += stepY) {
                for (int x = 0; x < w; x += stepX) {
                    int c = bmp.getPixel(x, y);
                    sum += (299L * Color.red(c) + 587L * Color.green(c) + 114L * Color.blue(c)) / 1000L;
                    count++;
                }
            }
            return count == 0 ? 0.3f : (sum / (float) count) / 255f;
        } catch (Exception e) {
            return 0.3f;
        }
    }

    public static boolean isLightColor(int color) {
        return isLight(color);
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(clamp255(alpha), Color.red(color), Color.green(color), Color.blue(color));
    }

    private static int blend(int base, int top, float a) {
        int r = Math.round(Color.red(base) + (Color.red(top) - Color.red(base)) * a);
        int g = Math.round(Color.green(base) + (Color.green(top) - Color.green(base)) * a);
        int b = Math.round(Color.blue(base) + (Color.blue(top) - Color.blue(base)) * a);
        return Color.rgb(clamp255(r), clamp255(g), clamp255(b));
    }

    private static int shiftValue(int color, float delta) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = c01(hsv[2] + delta);
        return Color.HSVToColor(hsv);
    }

    private static boolean isLight(int color) {
        float lum = (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)) / 255f;
        return lum > 0.55f;
    }

    private static float c01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static int clamp255(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
