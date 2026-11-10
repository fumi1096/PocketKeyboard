package com.zhangshang.keyboard;

/** 一个键位：显示文字、HID usage、栅格位置与尺寸（单位：格）。 */
public class KeyInfo {
    public final String label;
    public final String sub;      // 中文提示，可为 null
    public final int usage;
    public final float col;
    public final float row;
    public final float w;
    public final float h;

    public KeyInfo(String label, String sub, int usage, float col, float row, float w, float h) {
        this.label = label;
        this.sub = sub;
        this.usage = usage;
        this.col = col;
        this.row = row;
        this.w = w;
        this.h = h;
    }

    public boolean isModifier() {
        return HidKeys.isModifier(usage);
    }
}
