package com.zhangshang.keyboard;

import java.util.ArrayList;
import java.util.List;

/** 104 键横屏布局：22 列 × 6 行栅格。 */
public final class KeyLayout {
    public static final float COLS = 22f;
    public static final float ROWS = 6f;

    public final List<KeyInfo> keys = new ArrayList<>();

    private KeyLayout() {}

    private void add(String label, String sub, int usage, float col, float row, float w, float h) {
        keys.add(new KeyInfo(label, sub, usage, col, row, w, h));
    }

    private void addChar(char c, float col, float row) {
        keys.add(new KeyInfo(String.valueOf(c), null, HidKeys.usageFor(c), col, row, 1, 1));
    }

    public static KeyLayout create() {
        KeyLayout l = new KeyLayout();

        // ── 第 0 行：功能键区 ──────────────────────────────
        l.add("Esc", "退出", HidKeys.ESC, 0, 0, 1, 1);
        for (int i = 0; i < 12; i++) {
            l.add("F" + (i + 1), null, HidKeys.F1 + i, 1 + i, 0, 1, 1);
        }
        l.add("Prt", "截屏", HidKeys.PRINT_SCREEN, 13, 0, 1, 1);
        l.add("Scr", "滚动", HidKeys.SCROLL_LOCK, 14, 0, 1, 1);
        l.add("Brk", "暂停", HidKeys.PAUSE, 15, 0, 1, 1);

        // ── 第 1 行：数字行 ───────────────────────────────
        l.addChar('`', 0, 1);
        for (int i = 1; i <= 9; i++) l.addChar((char) ('0' + i), i, 1);
        l.addChar('0', 10, 1);
        l.addChar('-', 11, 1);
        l.addChar('=', 12, 1);
        l.add("Bksp", "退格", HidKeys.BACKSPACE, 13, 1, 2, 1);

        // ── 第 2 行：Q 行 ─────────────────────────────────
        l.add("Tab", "制表", HidKeys.TAB, 0, 2, 2, 1);
        String qrow = "QWERTYUIOP";
        for (int i = 0; i < qrow.length(); i++) l.addChar(qrow.charAt(i), 2 + i, 2);
        l.addChar('[', 12, 2);
        l.addChar(']', 13, 2);
        l.addChar('\\', 14, 2);

        // ── 第 3 行：A 行 ─────────────────────────────────
        l.add("Caps", "大写", HidKeys.CAPS_LOCK, 0, 3, 1.75f, 1);
        String arow = "ASDFGHJKL";
        for (int i = 0; i < arow.length(); i++) l.addChar(arow.charAt(i), 1.75f + i, 3);
        l.addChar(';', 10.75f, 3);
        l.addChar('\'', 11.75f, 3);
        l.add("Enter", "回车", HidKeys.ENTER, 12.75f, 3, 2.25f, 1);

        // ── 第 4 行：Z 行 ─────────────────────────────────
        l.add("Shift", "上档", HidKeys.LEFT_SHIFT, 0, 4, 2.25f, 1);
        String zrow = "ZXCVBNM";
        for (int i = 0; i < zrow.length(); i++) l.addChar(zrow.charAt(i), 2.25f + i, 4);
        l.addChar(',', 9.25f, 4);
        l.addChar('.', 10.25f, 4);
        l.addChar('/', 11.25f, 4);
        l.add("Shift", "上档", HidKeys.RIGHT_SHIFT, 12.25f, 4, 2.75f, 1);

        // ── 第 5 行：底行 ─────────────────────────────────
        l.add("Ctrl", "控制", HidKeys.LEFT_CTRL, 0, 5, 1.25f, 1);
        l.add("Win", "窗口", HidKeys.LEFT_GUI, 1.25f, 5, 1.25f, 1);
        l.add("Alt", "换挡", HidKeys.LEFT_ALT, 2.5f, 5, 1.25f, 1);
        l.add("Space", "空格", HidKeys.SPACE, 3.75f, 5, 6.25f, 1);
        l.add("Alt", "换挡", HidKeys.RIGHT_ALT, 10f, 5, 1.25f, 1);
        l.add("Win", "窗口", HidKeys.RIGHT_GUI, 11.25f, 5, 1.25f, 1);
        l.add("Menu", "菜单", HidKeys.MENU, 12.5f, 5, 1.25f, 1);
        l.add("Ctrl", "控制", HidKeys.RIGHT_CTRL, 13.75f, 5, 1.25f, 1);

        // ── 编辑键簇（第 15-17 列）────────────────────────
        l.add("Ins", "插入", HidKeys.INSERT, 15, 1, 1, 1);
        l.add("Home", "行首", HidKeys.HOME, 16, 1, 1, 1);
        l.add("PgUp", "上翻", HidKeys.PAGE_UP, 17, 1, 1, 1);
        l.add("Del", "删除", HidKeys.DELETE, 15, 2, 1, 1);
        l.add("End", "行尾", HidKeys.END, 16, 2, 1, 1);
        l.add("PgDn", "下翻", HidKeys.PAGE_DOWN, 17, 2, 1, 1);
        l.add("↑", null, HidKeys.UP, 16, 4, 1, 1);
        l.add("←", null, HidKeys.LEFT, 15, 5, 1, 1);
        l.add("↓", null, HidKeys.DOWN, 16, 5, 1, 1);
        l.add("→", null, HidKeys.RIGHT, 17, 5, 1, 1);

        // ── 独立小键盘（第 18-21 列）──────────────────────
        l.add("Num", "锁定", HidKeys.NUM_LOCK, 18, 1, 1, 1);
        l.add("/", null, HidKeys.KP_DIVIDE, 19, 1, 1, 1);
        l.add("*", null, HidKeys.KP_MULTIPLY, 20, 1, 1, 1);
        l.add("-", null, HidKeys.KP_MINUS, 21, 1, 1, 1);

        l.add("7", null, HidKeys.KP_7, 18, 2, 1, 1);
        l.add("8", null, HidKeys.KP_8, 19, 2, 1, 1);
        l.add("9", null, HidKeys.KP_9, 20, 2, 1, 1);
        l.add("+", null, HidKeys.KP_PLUS, 21, 2, 1, 2);

        l.add("4", null, HidKeys.KP_4, 18, 3, 1, 1);
        l.add("5", null, HidKeys.KP_5, 19, 3, 1, 1);
        l.add("6", null, HidKeys.KP_6, 20, 3, 1, 1);

        l.add("1", null, HidKeys.KP_1, 18, 4, 1, 1);
        l.add("2", null, HidKeys.KP_2, 19, 4, 1, 1);
        l.add("3", null, HidKeys.KP_3, 20, 4, 1, 1);
        l.add("Ent", "回车", HidKeys.KP_ENTER, 21, 4, 1, 2);

        l.add("0", null, HidKeys.KP_0, 18, 5, 2, 1);
        l.add(".", null, HidKeys.KP_DOT, 20, 5, 1, 1);

        return l;
    }
}
