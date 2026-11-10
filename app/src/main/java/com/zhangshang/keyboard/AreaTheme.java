package com.zhangshang.keyboard;

import org.json.JSONObject;

/**
 * 一块区域的外观参数：键盘或触控板各自一份。
 * 背景可以是纯色（HSV 可调），也可以是相册里选的图片；
 * 压暗/提亮是可调的遮罩强度；按键颜色与透明度也在这里。
 */
public class AreaTheme {
    public static final int MODE_COLOR = 0;
    public static final int MODE_IMAGE = 1;

    public int mode = MODE_COLOR;

    // 背景纯色（HSV）
    public float bgHue = 210f;
    public float bgSat = 0.25f;
    public float bgVal = 0.08f;

    /** 图片文件名（存放在 App 私有目录），null 表示没有图片。 */
    public String imageName = null;

    /** 压暗 / 提亮遮罩，0~1，两者同时大于 0 时取净值。 */
    public float darken = 0f;
    public float lighten = 0f;

    // 按键 / 面板颜色（HSV）
    public float keyHue = 210f;
    public float keySat = 0.10f;
    public float keyVal = 0.16f;

    /** 按键透明度 0~1（有背景图时调低可透出图片）。 */
    public float keyAlpha = 1f;

    public static AreaTheme keyboardDefault() {
        AreaTheme t = new AreaTheme();
        t.bgHue = 210f;
        t.bgSat = 0.25f;
        t.bgVal = 0.07f;
        t.keyHue = 210f;
        t.keySat = 0.10f;
        t.keyVal = 0.16f;
        t.keyAlpha = 1f;
        return t;
    }

    public static AreaTheme trackpadDefault() {
        AreaTheme t = new AreaTheme();
        t.bgHue = 210f;
        t.bgSat = 0.22f;
        t.bgVal = 0.08f;
        t.keyHue = 210f;
        t.keySat = 0.12f;
        t.keyVal = 0.10f;
        t.keyAlpha = 1f;
        return t;
    }

    public String toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("mode", mode);
            o.put("bgHue", bgHue);
            o.put("bgSat", bgSat);
            o.put("bgVal", bgVal);
            o.put("image", imageName == null ? "" : imageName);
            o.put("darken", darken);
            o.put("lighten", lighten);
            o.put("keyHue", keyHue);
            o.put("keySat", keySat);
            o.put("keyVal", keyVal);
            o.put("keyAlpha", keyAlpha);
        } catch (Exception ignored) {
        }
        return o.toString();
    }

    public static AreaTheme fromJson(String raw, AreaTheme fallback) {
        if (raw == null) return fallback;
        try {
            JSONObject o = new JSONObject(raw);
            AreaTheme t = new AreaTheme();
            t.mode = o.optInt("mode", fallback.mode);
            t.bgHue = (float) o.optDouble("bgHue", fallback.bgHue);
            t.bgSat = (float) o.optDouble("bgSat", fallback.bgSat);
            t.bgVal = (float) o.optDouble("bgVal", fallback.bgVal);
            String img = o.optString("image", "");
            t.imageName = (img == null || img.isEmpty()) ? null : img;
            t.darken = (float) o.optDouble("darken", fallback.darken);
            t.lighten = (float) o.optDouble("lighten", fallback.lighten);
            t.keyHue = (float) o.optDouble("keyHue", fallback.keyHue);
            t.keySat = (float) o.optDouble("keySat", fallback.keySat);
            t.keyVal = (float) o.optDouble("keyVal", fallback.keyVal);
            t.keyAlpha = (float) o.optDouble("keyAlpha", fallback.keyAlpha);
            return t;
        } catch (Exception e) {
            return fallback;
        }
    }
}
