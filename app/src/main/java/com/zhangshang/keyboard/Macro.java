package com.zhangshang.keyboard;

/** 一个自定义快捷键：名称 + 修饰键位 + 一个按键。 */
public class Macro {
    public final String name;
    public final int mods;
    public final int usage;

    public Macro(String name, int mods, int usage) {
        String n = name == null ? "" : name.trim();
        this.name = n.isEmpty() ? MacroKeys.labelFor(usage) : n;
        this.mods = mods;
        this.usage = usage;
    }
}
