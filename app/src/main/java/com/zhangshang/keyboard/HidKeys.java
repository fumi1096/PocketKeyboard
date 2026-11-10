package com.zhangshang.keyboard;

/** HID 键盘 usage 常量（Usage Page 0x07）与字符换算。 */
public final class HidKeys {
    private HidKeys() {}

    // 修饰键（0xE0-0xE7 同时对应报文第 0 字节的 bit0-bit7）
    public static final int LEFT_CTRL = 0xE0;
    public static final int LEFT_SHIFT = 0xE1;
    public static final int LEFT_ALT = 0xE2;
    public static final int LEFT_GUI = 0xE3;
    public static final int RIGHT_CTRL = 0xE4;
    public static final int RIGHT_SHIFT = 0xE5;
    public static final int RIGHT_ALT = 0xE6;
    public static final int RIGHT_GUI = 0xE7;

    // 控制键
    public static final int ENTER = 0x28;
    public static final int ESC = 0x29;
    public static final int BACKSPACE = 0x2A;
    public static final int TAB = 0x2B;
    public static final int SPACE = 0x2C;
    public static final int CAPS_LOCK = 0x39;

    // 功能键
    public static final int F1 = 0x3A;
    public static final int F2 = 0x3B;
    public static final int F3 = 0x3C;
    public static final int F4 = 0x3D;
    public static final int F5 = 0x3E;
    public static final int F6 = 0x3F;
    public static final int F7 = 0x40;
    public static final int F8 = 0x41;
    public static final int F9 = 0x42;
    public static final int F10 = 0x43;
    public static final int F11 = 0x44;
    public static final int F12 = 0x45;
    public static final int PRINT_SCREEN = 0x46;
    public static final int SCROLL_LOCK = 0x47;
    public static final int PAUSE = 0x48;

    // 编辑键
    public static final int INSERT = 0x49;
    public static final int HOME = 0x4A;
    public static final int PAGE_UP = 0x4B;
    public static final int DELETE = 0x4C;
    public static final int END = 0x4D;
    public static final int PAGE_DOWN = 0x4E;

    // 方向键
    public static final int RIGHT = 0x4F;
    public static final int LEFT = 0x50;
    public static final int DOWN = 0x51;
    public static final int UP = 0x52;

    // 小键盘
    public static final int NUM_LOCK = 0x53;
    public static final int KP_DIVIDE = 0x54;
    public static final int KP_MULTIPLY = 0x55;
    public static final int KP_MINUS = 0x56;
    public static final int KP_PLUS = 0x57;
    public static final int KP_ENTER = 0x58;
    public static final int KP_1 = 0x59;
    public static final int KP_2 = 0x5A;
    public static final int KP_3 = 0x5B;
    public static final int KP_4 = 0x5C;
    public static final int KP_5 = 0x5D;
    public static final int KP_6 = 0x5E;
    public static final int KP_7 = 0x5F;
    public static final int KP_8 = 0x60;
    public static final int KP_9 = 0x61;
    public static final int KP_0 = 0x62;
    public static final int KP_DOT = 0x63;
    public static final int MENU = 0x65;

    public static boolean isModifier(int usage) {
        return usage >= LEFT_CTRL && usage <= RIGHT_GUI;
    }

    /** 修饰键对应的报文位（bit0 = 左 Ctrl … bit7 = 右 GUI）。 */
    public static int modifierBit(int usage) {
        return 1 << (usage - LEFT_CTRL);
    }

    public static int letter(char c) {
        return 0x04 + (Character.toUpperCase(c) - 'A');
    }

    public static int digit(char c) {
        return c == '0' ? 0x27 : 0x1E + (c - '1');
    }

    /** 符号键的 usage，未收录返回 -1。 */
    public static int symbol(char c) {
        switch (c) {
            case '-': return 0x2D;
            case '=': return 0x2E;
            case '[': return 0x2F;
            case ']': return 0x30;
            case '\\': return 0x31;
            case ';': return 0x33;
            case '\'': return 0x34;
            case '`': return 0x35;
            case ',': return 0x36;
            case '.': return 0x37;
            case '/': return 0x38;
            default: return -1;
        }
    }

    /** 字符 → usage（字母、数字、符号）。 */
    public static int usageFor(char c) {
        if (c >= 'A' && c <= 'Z') return letter(c);
        if (c >= 'a' && c <= 'z') return letter(c);
        if (c >= '0' && c <= '9') return digit(c);
        return symbol(c);
    }
}
