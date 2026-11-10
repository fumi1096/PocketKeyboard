package com.zhangshang.keyboard;

/** 实验性虚拟手柄的 HID 描述符（Report ID 3）。 */
public final class GamepadDescriptor {
    private GamepadDescriptor() {}

    /**
     * 标准游戏手柄：16 个按键 + 4 个 8 位轴 + 4 位方向帽（hat）。
     * 报文 7 字节 = [按键低][按键高][X][Y][Z][Rz][hat(低4位)+填充]
     */
    public static final byte[] GAMEPAD = {
            0x05, 0x01,                   // Usage Page (Generic Desktop)
            0x09, 0x05,                   // Usage (Game Pad)
            (byte) 0xA1, 0x01,            // Collection (Application)
            (byte) 0x85, 0x03,            //   Report ID (3)
            0x05, 0x09,                   //   Usage Page (Button)
            0x19, 0x01,                   //   Usage Minimum (1)
            0x29, 0x10,                   //   Usage Maximum (16)
            0x15, 0x00,                   //   Logical Minimum (0)
            0x25, 0x01,                   //   Logical Maximum (1)
            (byte) 0x95, 0x10,            //   Report Count (16)
            0x75, 0x01,                   //   Report Size (1)
            (byte) 0x81, 0x02,            //   Input (Data, Variable, Absolute) —— 16 个按键
            0x05, 0x01,                   //   Usage Page (Generic Desktop)
            0x09, 0x30,                   //   Usage (X)
            0x09, 0x31,                   //   Usage (Y)
            0x09, 0x32,                   //   Usage (Z)
            0x09, 0x35,                   //   Usage (Rz)
            0x15, (byte) 0x81,            //   Logical Minimum (-127)
            0x25, 0x7F,                   //   Logical Maximum (127)
            0x75, 0x08,                   //   Report Size (8)
            (byte) 0x95, 0x04,            //   Report Count (4)
            (byte) 0x81, 0x02,            //   Input (Data, Variable, Absolute) —— 四个轴
            0x09, 0x39,                   //   Usage (Hat switch)
            0x15, 0x00,                   //   Logical Minimum (0)
            0x25, 0x07,                   //   Logical Maximum (7)
            0x35, 0x00,                   //   Physical Minimum (0)
            0x46, 0x3B, 0x01,             //   Physical Maximum (315)
            0x65, 0x14,                   //   Unit (English Rotation)
            0x75, 0x04,                   //   Report Size (4)
            (byte) 0x95, 0x01,            //   Report Count (1)
            (byte) 0x81, 0x42,            //   Input (Data, Variable, Absolute, Null State)
            0x75, 0x04,                   //   Report Size (4)
            (byte) 0x95, 0x01,            //   Report Count (1)
            (byte) 0x81, 0x03,            //   Input (Constant) —— 填充
            (byte) 0xC0                     // End Collection
    };

    /** 开启实验性手柄时注册的复合描述符：键盘 + 鼠标 + 手柄。 */
    public static final byte[] COMPOSITE_GAMEPAD =
            concat(ReportDescriptor.KEYBOARD, ReportDescriptor.MOUSE, GAMEPAD);

    private static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) total += p.length;
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        return out;
    }
}
