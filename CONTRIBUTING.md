# 贡献指南

感谢你愿意改进 PocketKeyboard！下面是最小约定：

## 环境

- JDK 17
- Android SDK Platform 35（`compileSdk = 35`，`minSdk = 28`）
- Gradle 用仓库自带的 wrapper（8.10.2），不要改成本机安装的 Gradle

## 本地验证

```bash
./gradlew assembleDebug
```

Windows 用 `gradlew.bat assembleDebug`。提交前请确保构建通过。

## 提交 Issue

请尽量附上：

- 手机型号、Android 版本、ROM（如 MIUI / HyperOS / OneUI）
- 电脑系统（Windows 10/11、macOS、Linux）与蓝牙适配器型号
- 顶部状态文字、设置里「已发送 / 失败」计数
- 复现步骤；能抓 `adb logcat -s BluetoothHidDevice` 更好

## 提交 PR

- 一个 PR 只做一件事，描述里写清动机与验证方式
- 不新增第三方依赖（项目刻意保持零依赖、纯 Java + 自绘 View）
- 涉及 HID 描述符/报文的改动，请说明测试机型与主机系统
- 新增界面文案使用中文

## 代码风格

- 4 空格缩进，Java 17，无 AndroidX/Compose（项目使用平台 API 与自绘 View）
- 颜色统一走 `AreaStyle` / `AreaTheme`，不要在 View 里硬编码主题色
- 与蓝牙相关逻辑放 `HidKeyboardManager`，UI 只调用其公开方法
