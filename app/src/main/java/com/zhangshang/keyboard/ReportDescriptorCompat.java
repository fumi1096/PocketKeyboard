package com.zhangshang.keyboard;

/**
 * 兼容模式用的经典键盘描述符：不带 Report ID，报文固定 8 字节。
 * 部分电脑（尤其 Windows）对"复合设备 + Report ID"兼容不好时，用这份描述符只注册键盘。
 */
public final class ReportDescriptorCompat {
    private ReportDescriptorCompat() {}

    public static final byte[] KEYBOARD_NO_REPORT_ID = {
            0x05, 0x01,              // Usage Page (Generic Desktop)
            0x09, 0x06,              // Usage (Keyboard)
            (byte) 0xA1, 0x01,       // Collection (Application)
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
}
