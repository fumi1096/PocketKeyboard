# 手机当蓝牙 HID 键盘 / 触控板：开源方案调研

> 调研时间：2026-11-08。Star 数、许可证、更新时间来自 GitHub API / F-Droid / App Store 当时的公开数据，会随时间变化。

## 0. 结论速览

有，而且比想象中多。你描述的"手机变成蓝牙 HID 设备、电脑/平板端零安装"的路线，
在 Android 上叫 **Bluetooth HID Device profile**（Android 9 / API 28 起系统公开支持，免 root），
在 iOS 上要走 **HID over GATT（HOGP，BLE）**。

直接可用的开源成品（按成熟度）：

1. **Mousedroid**（412★，MIT）——手机当鼠标 + 键盘，含触控板，经典蓝牙 HID。
2. **Bluke**（166★，AGPL-3.0，F-Droid）——机械键盘 + 游戏手柄 + 触控板 + 多媒体遥控。
3. **Bluetooth Remote**（133★，GPL-3.0，F-Droid / Play）——键盘 + 鼠标 + 媒体遥控，作者同一人还做了 iOS 版。
4. **Kontroller**（258★，Apache-2.0）——明确写支持 PC / Mac / TV / iPad，走 Android 9+ HID Device profile。
5. **NightBoard68**（32★，MIT）——横屏 68 键键盘 + 触控板，中文说明"电脑零安装"。

iOS / iPad / Mac 端：**darwin-bt-remote**（82★，AGPL-3.0，App Store 免费），
用 HOGP 把 iPhone / iPad / Mac 变成蓝牙键盘 + 触控板 + 媒体遥控。

---

## 1. 先厘清概念

- 你说的 "UHD" 应该是 **HID**（Human Interface Device，人机接口设备）。
- 两条技术路线：

| 路线 | 安卓实现 | iOS 实现 | 主机兼容性 | 功耗 |
|---|---|---|---|---|
| 经典蓝牙 HID（BR/EDR） | `BluetoothHidDevice`（API 28+，公开 API，免 root） | 不开放经典 HID 设备端 | Windows / macOS / Linux / Android / iPadOS / tvOS / ChromeOS 都认 | 较高 |
| BLE HID over GATT（HOGP） | 自建 GATT Server（如 kshoji 库） | `CBPeripheralManager` 可行 | 看主机是否支持 BLE HID；Windows/macOS/iPadOS 可以，部分 Linux 需 BlueZ 配置 | 低 |

- 这类方案和 WiFi 方案（Weylus、KDE Connect、Unified Remote、Mobile Mouse）的本质区别：
  **主机端不用装任何软件**，在系统蓝牙设置里配对一次，手机就显示为普通"键盘/鼠标"。
- 手机上装 App、开权限，选"注册 HID 设备"，然后在电脑/平板的蓝牙设置里配对即可。

---

## 2. Android 开源项目

| 项目 | 许可 | Star | 能力 | 备注 |
|---|---|---|---|---|
| [Mousedroid](https://github.com/darusc/Mousedroid) | MIT | 412 | 鼠标 / 触控板 / 键盘 | Kotlin，2023 起持续更新，话题含 bluetooth-classic、touchpad-remote |
| [Bluke](https://github.com/arnav-kr/Bluke) | AGPL-3.0 | 166 | 机械键盘（带音效）/ 游戏手柄 / 触控板 / 多媒体遥控 | Jetpack Compose，F-Droid 有包（dev.arnv.bluke） |
| [Bluetooth Remote](https://github.com/jqssun/android-bt-remote) | GPL-3.0 | 133 | 键盘 / 鼠标 / 媒体遥控 | 作者自述"首个开源通用 serverless 蓝牙 HID"；F-Droid、Play 均有 |
| [Kontroller](https://github.com/raghavk92/Kontroller) | Apache-2.0 | 258 | 鼠标 / 键盘 | 描述明确：Android 9+ BT HID Device profile，控 PC/Mac/TV/iPad |
| [NightBoard68](https://github.com/elysia395/NightBoard68) | MIT | 32 | 68 键（65%）键盘 + 触控板 | 中文项目，横屏使用，"电脑零安装" |
| [HidRemote](https://github.com/supertaohaili/HidRemote) | 无 | 60 | 键盘 / 鼠标 / 影音遥控 | 中文，Java，2021 年后未更新 |
| [hid-barcode-scanner](https://github.com/Fabi019/hid-barcode-scanner) | GPL-3.0 | 291 | 扫码 → 当蓝牙键盘输入到电脑 | 场景单一，但代码质量与 HID 用法很值得参考 |
| [GKME](https://github.com/4zyz4/GKME) | GPL-3.0 | 51 | 虚拟手柄 / 键鼠 | 支持 WiFi、USB、蓝牙三种直连；部分功能用 Shizuku/uinput |
| [apx-android](https://github.com/guocheng1378/apx-android) | MIT | 6 | 触控板 / 键盘 / 多媒体 / 副屏 / 麦克风 / 音响 | USB + 蓝牙 HID，功能最"全家桶"，较新 |
| [AutoFlower](https://github.com/Kningc/AutoFlower) | GPL-3.0 | 31 | 按脚本自动发送按键 | 自动化用途，不是交互式键鼠 |
| [ghostboard](https://github.com/ToxicOrca/ghostboard-android-bluetooth-mouse-keyboard) | MIT | 13 | 键鼠 / 触控板 | 2026 年新项目 |
| [linkpad](https://github.com/Devdas-gupta/linkpad) | MIT | 9 | 触控板 / 键盘 / 陀螺仪空中鼠标 / 媒体遥控 | 2026 年新项目 |
| [ghost-hid](https://github.com/ShubhamJ010/ghost-hid) | 无 | 8 | 键鼠 / 宏键盘 / 原始 HID 注入 | 偏极客玩法 |
| [keyboardOFbluetooth](https://github.com/FengyunTHU/keyboardOFbluetooth) | MIT | 15 | 安卓模拟蓝牙键盘 | 中文，Java |
| [Android-Bluetooth-HID-mouse](https://github.com/AFunGUYY/Android-Bluetooth-HID-mouse) | — | 少 | 原生蓝牙 HID 鼠标 + 键盘 | 中文，Kotlin，适合读代码 |

其他可逛的入口：[GitHub `bluetooth-hid` 话题](https://github.com/topics/bluetooth-hid)。

### 选型建议

- **只想马上用**：Mousedroid（键鼠最纯粹）、Bluke（功能最花）、Bluetooth Remote（F-Droid 好装）。
- **要在 iPad 上用**：Kontroller 的描述里明确写了 iPad；其它项目理论也可以（iPadOS 认标准 HID 键鼠）。
- **要键盘手感/布局**：NightBoard68（横屏 68 键）或 Bluke（机械键盘）。
- **要媒体控制**：Bluke、Bluetooth Remote、linkpad。
- **要读代码自己改**：Mousedroid（结构清晰）、NightBoard68（中文注释友好）、hid-barcode-scanner（HID 报告最简）。

---

## 3. iPhone / iPad / Mac 开源项目

| 项目 | 许可 | Star | 说明 |
|---|---|---|---|
| [darwin-bt-remote](https://github.com/jqssun/darwin-bt-remote) | AGPL-3.0 | 82 | iPhone / iPad / Mac 变成蓝牙键盘 + 触控板 + 媒体遥控；**HOGP（BLE）** 实现；App Store 免费：[BT Remote: Keyboard & Trackpad](https://apps.apple.com/us/app/bt-remote-keyboard-trackpad/id6778921831)（iOS 15+） |

要点：

- iOS 不开放经典蓝牙 HID 设备端，所以 iPhone 只能走 **BLE HID over GATT**。
- 该 App 的描述明确写：兼容 iOS / iPadOS / macOS / tvOS / Linux / Windows / ChromeOS，
  主机端零安装，配对一次即可；支持点击、双指右键、双指滚动、专用鼠标键、媒体键。
- 这是目前少见的"iOS 端也能当 HID 外设"的开源证据，做跨端方案时值得读它的 GATT 实现。

---

## 4. 底层库 / 参考实现

| 项目 | 许可 | Star | 用途 |
|---|---|---|---|
| [BLE-HID-Peripheral-for-Android](https://github.com/kshoji/BLE-HID-Peripheral-for-Android) | Apache-2.0 | 259 | Android 做 BLE HOGP 外设的库（HID service + report characteristic） |
| [bluehid](https://github.com/ralismark/bluehid) | Apache-2.0 | 42 | Android Oreo 时代的 HID 设备 PoC，历史参考 |
| [Authorizer](https://github.com/tejado/Authorizer) | — | 572 | 通过 **USB** HID 输入密码的开源密码管理器；USB HID 报告描述符可参考 |

官方文档：[BluetoothHidDevice](https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice)（API 28+）。

---

## 5. 闭源但成熟的产品（对照）

- **AppGround 的 "Bluetooth Keyboard & Mouse" / "Serverless Bluetooth Keyboard & Mouse"**：
  Play 上这类需求最出名的闭源产品，功能与上面开源项目重合，可作体验对照。
- iOS 上的 **AirBoard Pro** 等：蓝牙 + WiFi 双模式，部分功能内购。
- 这些闭源产品的存在说明：需求真实，但开源实现已经够用。

---

## 6. 这类方案的共同限制

1. **Android 需 9.0+**（API 28）；个别机型/ROM 的蓝牙栈可能不暴露 HID Device profile。
2. **一次只能连一台主机**，换设备要在手机端断开或主机端切换。
3. **屏幕必须亮着**（触控板/键盘都要摸屏幕），耗电明显；后台还需防止被国产 ROM 杀进程。
4. **手势能力有限**：HID 鼠标只有相对位移、按键、滚轮；双指缩放、Mission Control 这类原生触控板手势
   只能靠"模拟键盘快捷键"实现，不如真触控板顺滑。
5. **键盘布局**：发的是 HID scancode，主机布局（美式/中文）不同会导致符号错位；中文输入仍靠主机输入法。
6. **进不了 BIOS/UEFI**，也控制不了启动前的界面（经典蓝牙也要 OS 起来后才加载 HID 驱动）。
7. **延迟/干扰**：2.4GHz 环境一般 10–30ms 量级，竞技游戏不推荐；日常办公够用。
8. **iPadOS** 支持蓝牙鼠标/键盘，但指针是辅助触控样式，体验和 Mac 有差距。
9. 安全上：一个能自动打字的 HID 设备等同于键盘，来源不明的此类 App 有风险，优先选开源可审计的。

WiFi 替代路线（需要主机端装软件，但延迟/手势更灵活）：Weylus、KDE Connect、Unified Remote、Mobile Mouse。

---

## 7. 如果你想自己做一个

Android 端最小实现（经典 HID）：

1. 权限：`BLUETOOTH_CONNECT`（Android 12+ 运行时权限）。
2. `BluetoothAdapter.getProfileProxy(context, listener, BluetoothProfile.HID_DEVICE)` 拿到 `BluetoothHidDevice`。
3. 准备 HID Report Descriptor（建议复合设备）：
   - Report ID 1：键盘（6KRO 或 NKRO）
   - Report ID 2：鼠标（按键 + X/Y + 滚轮）
   - Report ID 3：Consumer Control（播放/音量等）
4. `registerApp(SdpSettings, QosSettings, appData, callback)` 注册；`SdpSettings` 里填设备名、描述、厂商、subclass、descriptor。
5. 主机在蓝牙设置里配对后，`onConnectionStateChanged` 回调；用 `sendReport(device, reportId, data)` 发按键/移动。
6. 实现 `onGetReport` / `onSetReport` / `onSetProtocol`（至少返回成功），否则部分主机会拒绝。
7. UI：触控板（`onTouchEvent` 换算相对位移）、键盘（自绘或系统输入法拦截）、媒体键。
8. 保活：前台 Service + 引导用户关闭电池优化。

iOS 端：`CBPeripheralManager` 建 HID Service（0x1812）+ Report characteristic + Report Reference descriptor，
由主机发起配对；细节可对照 darwin-bt-remote 的源码。

可直接抄作业的仓库：Mousedroid（结构最清晰）、NightBoard68（横屏键盘布局）、
hid-barcode-scanner（报告描述符最简）、darwin-bt-remote（HOGP 跨端）。

---

## 8. 参考链接

- Mousedroid — https://github.com/darusc/Mousedroid
- Bluke — https://github.com/arnav-kr/Bluke ；F-Droid：https://f-droid.org/packages/dev.arnv.bluke/
- Bluetooth Remote — https://github.com/jqssun/android-bt-remote ；F-Droid：https://f-droid.org/packages/io.github.jqssun.btremote/
- Kontroller — https://github.com/raghavk92/Kontroller
- NightBoard68 — https://github.com/elysia395/NightBoard68
- HidRemote — https://github.com/supertaohaili/HidRemote
- hid-barcode-scanner — https://github.com/Fabi019/hid-barcode-scanner
- GKME — https://github.com/4zyz4/GKME
- apx-android — https://github.com/guocheng1378/apx-android
- AutoFlower — https://github.com/Kningc/AutoFlower
- darwin-bt-remote — https://github.com/jqssun/darwin-bt-remote
- BT Remote（App Store）— https://apps.apple.com/us/app/bt-remote-keyboard-trackpad/id6778921831
- BLE-HID-Peripheral-for-Android — https://github.com/kshoji/BLE-HID-Peripheral-for-Android
- bluehid — https://github.com/ralismark/bluehid
- Android 官方 API — https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice
