package com.zhangshang.keyboard;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

/**
 * 蓝牙 HID 复合设备管理：把手机注册成"键盘 + 鼠标"，并发送对应报文。
 *
 * 键盘报文（Report ID 1，8 字节）：[修饰键位][保留][6 个普通键 usage]
 * 修饰键位 bit0 = 左 Ctrl … bit7 = 右 GUI(Windows 键 / Command 键)
 * 鼠标报文（Report ID 2，4 字节）：[按键位][dx][dy][滚轮]
 */
public class HidKeyboardManager {

    public interface Listener {
        void onStatusChanged(String status);

        void onConnectionChanged(boolean connected, String deviceName);
    }

    private static final int REPORT_ID_KEYBOARD = 1;
    private static final int REPORT_ID_MOUSE = 2;
    /** 兼容旧代码：键盘报文 ID。 */
    private static final int REPORT_ID = REPORT_ID_KEYBOARD;
    /** 上次成功连接的电脑地址，用于主动重连。 */
    private static final String PREF_LAST_DEVICE = "last_device";

    private final Context context;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SharedPreferences prefs;

    private BluetoothAdapter adapter;
    private BluetoothHidDevice hidDevice;
    private BluetoothDevice connectedDevice;
    private boolean registered;
    private boolean started;
    private boolean compatMode;
    private boolean gamepadEnabled;
    /** 手动/退出断开后置 true，屏蔽"断线自动重连"，直到再次主动连接。 */
    private boolean suppressReconnect;
    private int sentOk;
    private int sentFail;
    private long lastRegisterAttempt;

    private final BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile != BluetoothProfile.HID_DEVICE) return;
            hidDevice = (BluetoothHidDevice) proxy;
            registerApp();
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile != BluetoothProfile.HID_DEVICE) return;
            hidDevice = null;
            registered = false;
            notifyStatus("蓝牙 HID 服务已断开");
        }
    };

    private final BluetoothHidDevice.Callback callback = new BluetoothHidDevice.Callback() {
        @Override
        public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean isRegistered) {
            registered = isRegistered;
            if (isRegistered) {
                notifyStatus(compatMode
                        ? "已注册（兼容模式：只发键盘），请在电脑蓝牙里连接手机"
                        : "已注册为蓝牙键盘；若电脑显示已配对但没连上，点「设置」→「重新连接电脑」");
                // 之前连过这台电脑的话，注册完成后主动发起连接（Windows 不会自己连）
                mainHandler.postDelayed(HidKeyboardManager.this::connectToBondedHost, 1200);
            } else {
                notifyStatus("蓝牙键盘注册已注销");
            }
        }

        @Override
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            if (state == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device;
                try {
                    prefs.edit().putString(PREF_LAST_DEVICE, device.getAddress()).apply();
                } catch (Exception ignored) {
                }
                String name = safeName(device);
                notifyStatus("已连接：" + name + "，可以开始打字了");
                notifyConnection(true, name);
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                if (connectedDevice != null && connectedDevice.equals(device)) {
                    connectedDevice = null;
                }
                notifyConnection(false, null);
                if (suppressReconnect) {
                    notifyStatus("已断开（不再自动重连）");
                } else {
                    notifyStatus("连接已断开，正在自动重连…");
                    mainHandler.postDelayed(HidKeyboardManager.this::reconnectNow, 1500);
                }
            }
        }

        @Override
        public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize) {
            if (hidDevice == null) return;
            try {
                int size = Math.max(8, Math.min(bufferSize, 64));
                hidDevice.replyReport(device, type, id, new byte[size]);
            } catch (Exception ignored) {
            }
        }

        @Override
        public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data) {
            // 主机设置报文（例如 CapsLock 指示灯），回成功
            if (hidDevice != null) {
                try {
                    hidDevice.reportError(device, (byte) 0);
                } catch (Exception ignored) {
                }
            }
        }

        @Override
        public void onSetProtocol(BluetoothDevice device, byte protocol) {
            // 0 = 启动协议(Boot)，1 = 报文协议(Report)。两种模式都是 8 字节键盘报文，无需处理
        }

        @Override
        public void onInterruptData(BluetoothDevice device, byte reportId, byte[] data) {
        }

        @Override
        public void onVirtualCableUnplug(BluetoothDevice device) {
            connectedDevice = null;
            notifyStatus("电脑已断开蓝牙连接");
            notifyConnection(false, null);
        }
    };

    public HidKeyboardManager(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.prefs = this.context.getSharedPreferences("pocket_keyboard", Context.MODE_PRIVATE);
        this.compatMode = prefs.getBoolean("compat_mode", false);
        this.gamepadEnabled = prefs.getBoolean("gamepad_enabled", false);
    }

    /** 注册蓝牙键盘（需要蓝牙权限已授予、蓝牙已打开）。 */
    public void start() {
        if (started) return;
        started = true;

        BluetoothManager bm = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = bm == null ? null : bm.getAdapter();
        if (adapter == null) {
            notifyStatus("这台手机没有蓝牙适配器");
            started = false;
            return;
        }
        if (!adapter.isEnabled()) {
            notifyStatus("蓝牙未开启");
            started = false;
            return;
        }

        notifyStatus("正在把手机注册成蓝牙键盘…");
        boolean ok;
        try {
            ok = adapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE);
        } catch (SecurityException e) {
            ok = false;
        }
        if (!ok) {
            notifyStatus("本机蓝牙不支持 HID 设备模式（需 Android 9+ 且系统未裁剪）");
            started = false;
        }
    }

    private void registerApp() {
        if (hidDevice == null) return;
        try {
            BluetoothHidDeviceAppSdpSettings sdp = new BluetoothHidDeviceAppSdpSettings(
                    "掌上全键盘",
                    "手机模拟的电脑键盘",
                    "掌上全键盘",
                    (byte) (compatMode ? 0x40 : 0xC0),
                    compatMode
                            ? ReportDescriptorCompat.KEYBOARD_NO_REPORT_ID
                            : (gamepadEnabled
                                    ? GamepadDescriptor.COMPOSITE_GAMEPAD
                                    : ReportDescriptor.COMPOSITE));
            boolean ok = hidDevice.registerApp(sdp, null, null, context.getMainExecutor(), callback);
            if (!ok) {
                notifyStatus("注册蓝牙键盘失败，请退出 App 重进再试");
            }
        } catch (SecurityException e) {
            notifyStatus("缺少蓝牙权限，请在系统设置里授予「附近的设备」权限");
        } catch (Exception e) {
            notifyStatus("注册蓝牙键盘出错：" + e.getMessage());
        }
    }

    /** 发送一帧 8 字节键盘报文。 */
    public boolean sendReport(byte[] report) {
        if (hidDevice == null || connectedDevice == null) return false;
        if (!registered) {
            // 注册被系统注销后仍保留连接状态，导致"显示已连接但没反应" —— 自动补注册
            scheduleReRegister();
            sentFail++;
            return false;
        }
        try {
            boolean ok = hidDevice.sendReport(connectedDevice,
                    compatMode ? 0 : REPORT_ID_KEYBOARD, report);
            if (ok) sentOk++;
            else {
                sentFail++;
                scheduleReRegister();
            }
            return ok;
        } catch (Exception e) {
            sentFail++;
            return false;
        }
    }

    /** 发送一次性组合键：按下 → 80ms 后全部松开。 */
    public void sendCombo(int modifiers, int usage) {
        byte[] down = new byte[8];
        down[0] = (byte) modifiers;
        if (usage > 0 && usage <= 0xFF) down[2] = (byte) usage;
        sendReport(down);
        mainHandler.postDelayed(() -> sendReport(new byte[8]), 80);
    }

    /**
     * 发送鼠标报文（Report ID 2）。
     *
     * @param buttons 按键位：1=左键 2=右键 4=中键
     * @param dx      水平位移（相对，-127..127）
     * @param dy      垂直位移（相对，-127..127）
     * @param wheel   滚轮（正=向上，负=向下）
     */
    public boolean sendMouseReport(int buttons, int dx, int dy, int wheel) {
        if (compatMode) return false;
        if (hidDevice == null || connectedDevice == null) return false;
        if (!registered) {
            scheduleReRegister();
            return false;
        }
        byte[] report = new byte[4];
        report[0] = (byte) (buttons & 0x07);
        report[1] = (byte) clamp127(dx);
        report[2] = (byte) clamp127(dy);
        report[3] = (byte) clamp127(wheel);
        try {
            return hidDevice.sendReport(connectedDevice, REPORT_ID_MOUSE, report);
        } catch (Exception e) {
            return false;
        }
    }

    private static int clamp127(int v) {
        return v > 127 ? 127 : (v < -127 ? -127 : v);
    }

    // ── 实验性：虚拟手柄 ──────────────────────────────────

    public boolean isGamepadEnabled() {
        return gamepadEnabled;
    }

    /** 开关实验性虚拟手柄：会重新注册 HID 描述符，电脑上需要重新连接一次。 */
    public void setGamepadEnabled(boolean enabled) {
        if (this.gamepadEnabled == enabled) return;
        this.gamepadEnabled = enabled;
        prefs.edit().putBoolean("gamepad_enabled", enabled).apply();
        try {
            if (hidDevice != null && registered) hidDevice.unregisterApp();
        } catch (Exception ignored) {
        }
        registered = false;
        mainHandler.postDelayed(this::registerApp, 400);
        notifyStatus(enabled
                ? "已开启虚拟手柄（实验性），请在电脑上重新连接手机"
                : "已关闭虚拟手柄，请在电脑上重新连接手机");
    }

    /** 发送手柄报文（Report ID 3，7 字节）。 */
    public boolean sendGamepadReport(int buttons, int lx, int ly, int rx, int ry, int hat) {
        if (!gamepadEnabled || compatMode) return false;
        if (hidDevice == null || connectedDevice == null) return false;
        if (!registered) {
            scheduleReRegister();
            return false;
        }
        byte[] report = new byte[7];
        report[0] = (byte) (buttons & 0xFF);
        report[1] = (byte) ((buttons >> 8) & 0xFF);
        report[2] = (byte) clamp127(lx);
        report[3] = (byte) clamp127(ly);
        report[4] = (byte) clamp127(rx);
        report[5] = (byte) clamp127(ry);
        report[6] = (byte) (hat & 0x0F);
        try {
            boolean ok = hidDevice.sendReport(connectedDevice, 3, report);
            if (ok) sentOk++;
            else sentFail++;
            return ok;
        } catch (Exception e) {
            sentFail++;
            return false;
        }
    }

    private String lastDisconnectInfo = "";

    public String getLastDisconnectInfo() {
        return lastDisconnectInfo;
    }

    /**
     * 强制断开与电脑的蓝牙连接：逐个 disconnect + 注销 HID 注册。
     * 注销注册会让 Android 关闭 HID 连接，电脑端才会真正显示未连接；
     * 回到 App 时 reconnectNow() 会自动重新注册并连回去。
     */
    public void disconnectNow() {
        suppressReconnect = true;
        mainHandler.removeCallbacksAndMessages(null);
        StringBuilder sb = new StringBuilder();
        if (hidDevice == null) {
            connectedDevice = null;
            lastDisconnectInfo = "HID 未就绪";
            return;
        }
        int count = 0;
        try {
            java.util.List<BluetoothDevice> devices = hidDevice.getConnectedDevices();
            if (devices != null) {
                count = devices.size();
                for (BluetoothDevice d : devices) {
                    if (d == null) continue;
                    boolean ok;
                    try {
                        ok = hidDevice.disconnect(d);
                    } catch (Exception e) {
                        ok = false;
                    }
                    sb.append(ok ? "断开成功" : "断开请求被拒").append("；");
                }
            }
        } catch (Exception e) {
            sb.append("读取连接失败；");
        }
        if (count == 0) {
            if (connectedDevice != null) {
                try {
                    hidDevice.disconnect(connectedDevice);
                    sb.append("断开缓存设备；");
                } catch (Exception e) {
                    sb.append("断开缓存设备失败；");
                }
            } else {
                sb.append("当前无连接；");
            }
        }
        try {
            hidDevice.unregisterApp();
            registered = false;
            sb.append("已注销 HID");
        } catch (Exception e) {
            sb.append("注销 HID 失败");
        }
        connectedDevice = null;
        lastDisconnectInfo = sb.toString();
        notifyStatus("蓝牙已断开（退出期间不会自动重连）");
    }

    public boolean isConnected() {
        return connectedDevice != null;
    }

    public boolean isRegistered() {
        return registered;
    }

    public int getSentOk() {
        return sentOk;
    }

    public int getSentFail() {
        return sentFail;
    }

    public boolean isCompatMode() {
        return compatMode;
    }

    /** 切换兼容模式：只用键盘、经典 8 字节描述符（电脑认不到复合设备时用）。切换后需重新连接电脑。 */
    public void setCompatMode(boolean enabled) {
        if (this.compatMode == enabled) return;
        this.compatMode = enabled;
        prefs.edit().putBoolean("compat_mode", enabled).apply();
        try {
            if (hidDevice != null && registered) hidDevice.unregisterApp();
        } catch (Exception ignored) {
        }
        registered = false;
        mainHandler.postDelayed(this::registerApp, 400);
        notifyStatus(enabled
                ? "已切换兼容模式（只发键盘），等电脑重新连接即可"
                : "已切回复合模式（键盘 + 鼠标），等电脑重新连接即可");
    }

    /** 连续发送失败时补一次注册（3 秒节流）。 */
    private void scheduleReRegister() {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastRegisterAttempt < 3000) return;
        lastRegisterAttempt = now;
        registerApp();
        notifyStatus("检测到发送异常，正在重新注册键盘…");
    }

    /**
     * 主动连接 / 重连电脑。
     * Windows 在"已配对但未连接"时不会自己连，手机侧主动 connect 才能触发。
     * 如果当前没注册（比如刚从后台回来），会先重新注册再连接。
     */
    public void reconnectNow() {
        suppressReconnect = false;
        if (hidDevice == null) {
            notifyStatus("蓝牙 HID 还没就绪，稍等几秒再试");
            return;
        }
        if (!registered) {
            registerApp();
            mainHandler.postDelayed(this::connectToBondedHost, 1000);
            return;
        }
        connectToBondedHost();
    }

    /** 向上次连接过的电脑发起 HID 连接。 */
    private void connectToBondedHost() {
        if (suppressReconnect) return;
        if (hidDevice == null || connectedDevice != null) return;
        String addr;
        try {
            addr = prefs.getString(PREF_LAST_DEVICE, null);
        } catch (Exception e) {
            return;
        }
        if (addr == null) return;
        try {
            BluetoothDevice device = adapter.getRemoteDevice(addr);
            boolean ok = hidDevice.connect(device);
            if (ok) {
                notifyStatus("正在连接电脑…（如果没反应，请在电脑端点击该设备）");
            }
        } catch (Exception ignored) {
        }
    }

    /** 手机当前蓝牙名称，用于配对说明。 */
    public String deviceName() {
        try {
            BluetoothManager bm = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter a = bm == null ? null : bm.getAdapter();
            String n = a == null ? null : a.getName();
            if (n != null && !n.isEmpty()) return n;
        } catch (Exception ignored) {
        }
        return "掌上全键盘";
    }

    public void stop() {
        try {
            if (hidDevice != null && registered) hidDevice.unregisterApp();
        } catch (Exception ignored) {
        }
        try {
            if (adapter != null && hidDevice != null) {
                adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice);
            }
        } catch (Exception ignored) {
        }
        hidDevice = null;
        connectedDevice = null;
        registered = false;
        started = false;
    }

    private String safeName(BluetoothDevice d) {
        try {
            String n = d.getName();
            return n == null ? "未知设备" : n;
        } catch (Exception e) {
            return "未知设备";
        }
    }

    private void notifyStatus(String s) {
        if (listener != null) listener.onStatusChanged(s);
    }

    private void notifyConnection(boolean connected, String name) {
        if (listener != null) listener.onConnectionChanged(connected, name);
    }
}
