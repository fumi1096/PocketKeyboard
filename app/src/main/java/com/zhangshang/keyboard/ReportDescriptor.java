package com.zhangshang.keyboard;

/** HID 报文描述符（复合设备：键盘 + 鼠标）。 */
public final class ReportDescriptor {
    private ReportDescriptor() {}

    /**
     * 标准 6 键无冲键盘描述符（Report ID 1）。
     * 报文格式：8 字节 = [修饰键位][保留][6 个普通键 usage]。
     */
    public static final byte[] KEYBOARD = {
            0x05, 0x01,              // Usage Page (Generic Desktop)
            0x09, 0x06,              // Usage (Keyboard)
            (byte) 0xA1, 0x01,       // Collection (Application)
            (byte) 0x85, 0x01,       //   Report ID (1)
            0x05, 0x07,              //   Usage Page (Keyboard/Keypad)
            0x19, (byte) 0xE0,       //   Usage Minimum (Left Control)
            0x29, (byte) 0xE7,       //   Usage Maximum (Right GUI)
            0x15, 0x00,              //   Logical Minimum (0)
            0x25, 0x01,              //   Logical Maximum (1)
            0x75, 0x01,              //   Report Size (1)
            (byte) 0x95, 0x08,       //   Report Count (8)
            (byte) 0x81, 0x02,       //   Input (Data, Variable, Absolute) —— 修饰键位
            (byte) 0x95, 0x01,       //   Report Count (1)
            0x75, 0x08,              //   Report Size (8)
            (byte) 0x81, 0x01,       //   Input (Constant) —— 保留字节
            (byte) 0x95, 0x06,       //   Report Count (6)
            0x75, 0x08,              //   Report Size (8)
            0x15, 0x00,              //   Logical Minimum (0)
            0x25, 0x65,              //   Logical Maximum (101)
            0x05, 0x07,              //   Usage Page (Keyboard/Keypad)
            0x19, 0x00,              //   Usage Minimum (0)
            0x29, 0x65,              //   Usage Maximum (101)
            (byte) 0x81, 0x00,       //   Input (Data, Array) —— 6 个键码
            (byte) 0xC0               // End Collection
    };

    /**
     * 标准三键鼠标描述符（Report ID 2）。
     * 报文格式：4 字节 = [按键位][dx][dy][滚轮]，位移为相对值（-127..127）。
     */
    public static final byte[] MOUSE = {
            0x05, 0x01,              // Usage Page (Generic Desktop)
            0x09, 0x02,              // Usage (Mouse)
            (byte) 0xA1, 0x01,       // Collection (Application)
            (byte) 0x85, 0x02,       //   Report ID (2)
            0x09, 0x01,              //   Usage (Pointer)
            (byte) 0xA1, 0x00,       //   Collection (Physical)
            0x05, 0x09,              //     Usage Page (Button)
            0x19, 0x01,              //     Usage Minimum (1)
            0x29, 0x03,              //     Usage Maximum (3)
            0x15, 0x00,              //     Logical Minimum (0)
            0x25, 0x01,              //     Logical Maximum (1)
            (byte) 0x95, 0x03,       //     Report Count (3)
            0x75, 0x01,              //     Report Size (1)
            (byte) 0x81, 0x02,       //     Input (Data, Variable, Absolute) —— 左/右/中键
            (byte) 0x95, 0x01,       //     Report Count (1)
            0x75, 0x05,              //     Report Size (5)
            (byte) 0x81, 0x01,       //     Input (Constant) —— 填充
            0x05, 0x01,              //     Usage Page (Generic Desktop)
            0x09, 0x30,              //     Usage (X)
            0x09, 0x31,              //     Usage (Y)
            0x09, 0x38,              //     Usage (Wheel)
            0x15, (byte) 0x81,       //     Logical Minimum (-127)
            0x25, 0x7F,              //     Logical Maximum (127)
            0x75, 0x08,              //     Report Size (8)
            (byte) 0x95, 0x03,       //     Report Count (3)
            (byte) 0x81, 0x06,       //     Input (Data, Variable, Relative)
            (byte) 0xC0,             //   End Collection
            (byte) 0xC0              // End Collection
    };

    /** 注册给电脑的复合描述符：键盘 + 鼠标。 */
    public static final byte[] COMPOSITE = concat(KEYBOARD, MOUSE);

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }
}
