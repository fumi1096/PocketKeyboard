package com.zhangshang.keyboard;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements HidKeyboardManager.Listener {

    private static final int REQ_PERMISSIONS = 100;
    private static final int REQ_ENABLE_BT = 101;
    private static final int REQ_DISCOVERABLE = 102;
    private static final int REQ_PICK_IMAGE = 103;

    private static final String PREF_CURSOR_SPEED = "cursor_speed";
    private static final String PREF_SCROLL_SPEED = "scroll_speed";
    private static final String PREF_SCROLL_REVERSE = "scroll_reverse";

    private HidKeyboardManager hid;
    private TextView statusView;
    private KeyboardView keyboardView;
    private TrackpadView trackpadView;
    private GamepadView gamepadView;
    private FrameLayout keyArea;
    private LinearLayout macroRow;
    private LinearLayout rootView;
    private PageBackgroundDrawable pageBg;
    private Button modeButton;
    private SharedPreferences prefs;
    private AreaTheme keyboardTheme;
    private AreaTheme trackpadTheme;
    private Bitmap keyboardBitmap;
    private Bitmap trackpadBitmap;
    private int editingArea = 0;
    private SeekBar themeAlphaSeek;
    private TextView themeAlphaValue;
    private final List<Macro> macros = new ArrayList<>();
    private int mode = 0; // 0=键盘 1=触控板 2=虚拟手柄（实验性）
    private boolean started = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("pocket_keyboard", MODE_PRIVATE);
        keyboardTheme = ThemeStore.loadKeyboard(this);
        trackpadTheme = ThemeStore.loadTrackpad(this);
        keyboardBitmap = loadThemeBitmap(keyboardTheme.imageName);
        trackpadBitmap = loadThemeBitmap(trackpadTheme.imageName);
        hid = new HidKeyboardManager(this, this);
        macros.addAll(MacroStore.load(this));
        LinearLayout root = buildUi();
        setContentView(root);
        setupImmersive(root);
        // 打字期间不熄屏
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestBluetooth();
    }

    private LinearLayout buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        pageBg = new PageBackgroundDrawable();
        root.setBackground(pageBg);
        rootView = root;

        // 顶部状态栏
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(6), dp(10), dp(4));

        statusView = new TextView(this);
        statusView.setTextColor(0xFFB9C6D6);
        statusView.setTextSize(12);
        statusView.setText("正在初始化…");
        statusView.setShadowLayer(dp(4), 0, dp(1), 0xCC000000);
        bar.addView(statusView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        modeButton = makeButton("触控板");
        modeButton.setOnClickListener(v -> toggleMode());
        bar.addView(modeButton);

        Button btnAppearance = makeButton("外观");
        btnAppearance.setOnClickListener(v -> showAppearance());
        bar.addView(btnAppearance);

        Button btnSettings = makeButton("设置");
        btnSettings.setOnClickListener(v -> showSettings());
        bar.addView(btnSettings);

        root.addView(bar);

        // 自定义快捷键条（长按可编辑/删除，末尾「＋ 自定义」新增）
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        macroRow = new LinearLayout(this);
        macroRow.setOrientation(LinearLayout.HORIZONTAL);
        macroRow.setPadding(dp(8), 0, dp(8), dp(4));
        scroller.addView(macroRow);
        root.addView(scroller);

        // 键盘 / 触控板主体
        keyboardView = new KeyboardView(this);
        keyboardView.setReportListener(hid::sendReport);
        trackpadView = new TrackpadView(this, hid::sendMouseReport);
        gamepadView = new GamepadView(this, hid::sendGamepadReport);
        applyTrackpadPrefs();

        keyArea = new FrameLayout(this);
        keyArea.addView(keyboardView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(keyArea,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        rebuildMacroBar();
        applyTheme();
        return root;
    }

    // ── 自定义快捷键 ──────────────────────────────────────

    private void rebuildMacroBar() {
        if (macroRow == null) return;
        macroRow.removeAllViews();

        for (Macro m : macros) {
            Button b = makeButton(m.name);
            b.setOnClickListener(v -> {
                if (hid.isConnected()) {
                    hid.sendCombo(m.mods, m.usage);
                } else {
                    toast("还没有连接电脑，请先在电脑蓝牙里配对");
                }
            });
            b.setOnLongClickListener(v -> {
                showMacroMenu(m);
                return true;
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(30));
            lp.rightMargin = dp(6);
            macroRow.addView(b, lp);
        }

        Button add = makeButton("＋ 自定义");
        add.setOnClickListener(v -> showMacroDialog(null));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(30));
        lp.rightMargin = dp(6);
        macroRow.addView(add, lp);
    }

    private void showMacroMenu(final Macro m) {
        new AlertDialog.Builder(this)
                .setTitle(m.name)
                .setItems(new String[]{"编辑", "删除"}, (d, which) -> {
                    if (which == 0) {
                        showMacroDialog(m);
                    } else {
                        macros.remove(m);
                        MacroStore.save(this, macros);
                        rebuildMacroBar();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showMacroDialog(final Macro edit) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(12), dp(20), dp(4));

        EditText nameInput = new EditText(this);
        nameInput.setHint("名称，例如：任务管理器");
        nameInput.setSingleLine(true);
        if (edit != null) nameInput.setText(edit.name);
        content.addView(nameInput);

        TextView modTitle = new TextView(this);
        modTitle.setText("修饰键（可多选）");
        modTitle.setTextColor(0xFF9AA6B6);
        modTitle.setTextSize(12);
        modTitle.setPadding(0, dp(10), 0, dp(2));
        content.addView(modTitle);

        LinearLayout modsRow = new LinearLayout(this);
        modsRow.setOrientation(LinearLayout.HORIZONTAL);
        final CheckBox cbCtrl = makeCheck("Ctrl");
        final CheckBox cbShift = makeCheck("Shift");
        final CheckBox cbAlt = makeCheck("Alt");
        final CheckBox cbWin = makeCheck("Win");
        modsRow.addView(cbCtrl);
        modsRow.addView(cbShift);
        modsRow.addView(cbAlt);
        modsRow.addView(cbWin);
        content.addView(modsRow);

        TextView keyTitle = new TextView(this);
        keyTitle.setText("按键");
        keyTitle.setTextColor(0xFF9AA6B6);
        keyTitle.setTextSize(12);
        keyTitle.setPadding(0, dp(10), 0, dp(2));
        content.addView(keyTitle);

        final Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, MacroKeys.LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        if (edit != null) {
            spinner.setSelection(MacroKeys.indexOfUsage(edit.usage));
            cbCtrl.setChecked((edit.mods & 1) != 0);
            cbShift.setChecked((edit.mods & 2) != 0);
            cbAlt.setChecked((edit.mods & 4) != 0);
            cbWin.setChecked((edit.mods & 8) != 0);
        }
        content.addView(spinner);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(edit == null ? "添加快捷键" : "编辑快捷键")
                .setView(content)
                .setPositiveButton("保存", null)
                .setNegativeButton("取消", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int usage = MacroKeys.USAGES[spinner.getSelectedItemPosition()];
            int mods = (cbCtrl.isChecked() ? 1 : 0)
                    | (cbShift.isChecked() ? 2 : 0)
                    | (cbAlt.isChecked() ? 4 : 0)
                    | (cbWin.isChecked() ? 8 : 0);
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) name = comboLabel(mods, usage);

            Macro macro = new Macro(name, mods, usage);
            if (edit == null) {
                macros.add(macro);
            } else {
                int idx = macros.indexOf(edit);
                if (idx >= 0) macros.set(idx, macro);
                else macros.add(macro);
            }
            MacroStore.save(this, macros);
            rebuildMacroBar();
            dialog.dismiss();
        });
    }

    private CheckBox makeCheck(String text) {
        CheckBox cb = new CheckBox(this);
        cb.setText(text);
        cb.setTextSize(13);
        cb.setPadding(0, 0, dp(10), 0);
        return cb;
    }

    private String comboLabel(int mods, int usage) {
        StringBuilder sb = new StringBuilder();
        if ((mods & 1) != 0) sb.append("Ctrl+");
        if ((mods & 2) != 0) sb.append("Shift+");
        if ((mods & 4) != 0) sb.append("Alt+");
        if ((mods & 8) != 0) sb.append("Win+");
        return sb.append(MacroKeys.labelFor(usage)).toString();
    }

    // ── 键盘 / 触控板 / 手柄切换 ───────────────────────────

    private void toggleMode() {
        int next = mode + 1;
        if (next > 2) next = 0;
        if (next == 2 && !hid.isGamepadEnabled()) next = 0;
        setMode(next);
    }

    private void setMode(int newMode) {
        mode = newMode;
        if (keyboardView != null) keyboardView.releaseAll();
        if (trackpadView != null) trackpadView.reset();
        if (gamepadView != null) gamepadView.reset();
        keyArea.removeAllViews();
        if (mode == 1) {
            keyArea.addView(trackpadView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else if (mode == 2) {
            keyArea.addView(gamepadView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            keyArea.addView(keyboardView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        modeButton.setText(nextModeLabel());
        applyTheme();
    }

    private String nextModeLabel() {
        int next = mode + 1;
        if (next > 2) next = 0;
        if (next == 2 && !hid.isGamepadEnabled()) next = 0;
        if (next == 0) return "键盘";
        return next == 1 ? "触控板" : "手柄";
    }

    // ── 触控板设置（二级弹窗）──────────────────────────────

    private void applyTrackpadPrefs() {
        trackpadView.setSensitivity(prefs.getFloat(PREF_CURSOR_SPEED, 1.35f));
        trackpadView.setScrollSpeed(prefs.getFloat(PREF_SCROLL_SPEED, 1.0f));
        trackpadView.setReverseScroll(prefs.getBoolean(PREF_SCROLL_REVERSE, false));
    }

    // ── 设置（配对 / 连接 / 触控板）────────────────────────

    private void showSettings() {
        float cursor = prefs.getFloat(PREF_CURSOR_SPEED, 1.35f);
        float scroll = prefs.getFloat(PREF_SCROLL_SPEED, 1.0f);
        boolean reverse = prefs.getBoolean(PREF_SCROLL_REVERSE, false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(6), dp(20), dp(4));

        // 配对相关
        Button discover = new Button(this);
        discover.setText("让电脑搜到我");
        discover.setAllCaps(false);
        discover.setTextSize(13);
        discover.setPadding(dp(14), 0, dp(14), 0);
        discover.setOnClickListener(v -> requestDiscoverable());
        content.addView(discover, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)));

        Button guide = new Button(this);
        guide.setText("配对说明");
        guide.setAllCaps(false);
        guide.setTextSize(13);
        guide.setPadding(dp(14), 0, dp(14), 0);
        guide.setOnClickListener(v -> showGuide());
        content.addView(guide, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)));

        // 连接区：电脑显示已配对但没连上时，点这里由手机主动发起连接
        Button reconnect = new Button(this);
        reconnect.setText("重新连接电脑");
        reconnect.setAllCaps(false);
        reconnect.setTextSize(13);
        reconnect.setPadding(dp(14), 0, dp(14), 0);
        reconnect.setOnClickListener(v -> {
            hid.reconnectNow();
            toast("已向电脑发起连接，等 2~3 秒看顶部状态");
        });
        content.addView(reconnect, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)));

        // 兼容模式：电脑认不到"键盘+鼠标"复合设备时，退回只发键盘的经典描述符
        CheckBox compatBox = new CheckBox(this);
        compatBox.setText("兼容模式（只发键盘，电脑识别异常时勾选）");
        compatBox.setTextSize(13);
        compatBox.setTextColor(0xFFC6D0DC);
        compatBox.setChecked(hid.isCompatMode());
        compatBox.setPadding(0, dp(8), 0, 0);
        compatBox.setOnCheckedChangeListener((buttonView, checked) -> {
            hid.setCompatMode(checked);
            toast(checked ? "已切换兼容模式，请在电脑上重新连接手机" : "已切回复合模式，请在电脑上重新连接手机");
        });
        content.addView(compatBox);

        // 实验性功能
        CheckBox gamepadBox = new CheckBox(this);
        gamepadBox.setText("实验性功能：虚拟手柄（开启后电脑多一个手柄设备，需重新连接）");
        gamepadBox.setTextSize(12);
        gamepadBox.setTextColor(0xFFC6D0DC);
        gamepadBox.setChecked(hid.isGamepadEnabled());
        gamepadBox.setPadding(0, dp(8), 0, 0);
        gamepadBox.setOnCheckedChangeListener((buttonView, checked) -> {
            hid.setGamepadEnabled(checked);
            if (!checked && mode == 2) setMode(0);
            toast(checked ? "已开启虚拟手柄，请到电脑上重新连接一次" : "已关闭虚拟手柄");
        });
        content.addView(gamepadBox);

        // 诊断：发送是否真的成功
        TextView diag = new TextView(this);
        diag.setTextSize(12);
        diag.setTextColor(0xFF8E9AAA);
        diag.setPadding(0, dp(10), 0, 0);
        diag.setText("已发送 " + hid.getSentOk() + " 帧，失败 " + hid.getSentFail()
                + " 帧。失败数一直涨说明报文没发出去，先试上面「兼容模式」");
        content.addView(diag);

        Button btSettings = new Button(this);
        btSettings.setText("打开系统蓝牙设置（删除旧配对）");
        btSettings.setAllCaps(false);
        btSettings.setTextSize(13);
        btSettings.setPadding(dp(14), 0, dp(14), 0);
        btSettings.setOnClickListener(v -> {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
            } catch (Exception e) {
                toast("打不开蓝牙设置，请手动进系统设置");
            }
        });
        content.addView(btSettings, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)));

        // 光标速度 0.30 ~ 3.00（步进 0.01）
        final TextView cursorValue = optionValue(fmt(cursor));
        SeekBar cursorSeek = makeSeekBar(270,
                Math.round((clamp(cursor, 0.30f, 3.00f) - 0.30f) / 0.01f));
        cursorSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                float v = 0.30f + progress * 0.01f;
                trackpadView.setSensitivity(v);
                cursorValue.setText(fmt(v));
                prefs.edit().putFloat(PREF_CURSOR_SPEED, v).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        content.addView(settingRow("光标速度", cursorSeek, cursorValue));

        // 滚动速度 0.40 ~ 2.00（步进 0.01）
        final TextView scrollValue = optionValue(fmt(scroll));
        SeekBar scrollSeek = makeSeekBar(160,
                Math.round((clamp(scroll, 0.40f, 2.00f) - 0.40f) / 0.01f));
        scrollSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                float v = 0.40f + progress * 0.01f;
                trackpadView.setScrollSpeed(v);
                scrollValue.setText(fmt(v));
                prefs.edit().putFloat(PREF_SCROLL_SPEED, v).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        content.addView(settingRow("滚动速度", scrollSeek, scrollValue));

        CheckBox reverseBox = new CheckBox(this);
        reverseBox.setText("上下反向（双指上滑变下滚）");
        reverseBox.setTextSize(13);
        reverseBox.setTextColor(0xFFC6D0DC);
        reverseBox.setChecked(reverse);
        reverseBox.setPadding(0, dp(8), 0, 0);
        reverseBox.setOnCheckedChangeListener((buttonView, checked) -> {
            trackpadView.setReverseScroll(checked);
            prefs.edit().putBoolean(PREF_SCROLL_REVERSE, checked).apply();
        });
        content.addView(reverseBox);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(content);
        new AlertDialog.Builder(this)
                .setTitle("设置")
                .setView(scrollView)
                .setPositiveButton("完成", null)
                .show();
    }

    // ── 外观 / 皮肤 ────────────────────────────────────────

    private void showAppearance() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(6), dp(20), dp(6));

        content.addView(dialogButton("键盘外观（背景 / 按键）", v -> editArea(0)));
        content.addView(dialogButton("触控板外观（背景 / 面板）", v -> editArea(1)));
        content.addView(dialogButton("恢复默认外观", v -> {
            keyboardTheme = AreaTheme.keyboardDefault();
            trackpadTheme = AreaTheme.trackpadDefault();
            keyboardBitmap = null;
            trackpadBitmap = null;
            saveThemes();
            applyTheme();
            toast("已恢复默认外观");
        }));

        TextView hint = new TextView(this);
        hint.setText("键盘和触控板可以分别设置。\n"
                + "背景支持纯色（色相 / 饱和度 / 明度可调）或相册图片；\n"
                + "「压暗 / 提亮」调整整体明暗，按键颜色与透明度同样可调。");
        hint.setTextSize(12);
        hint.setTextColor(0xFF8E9AAA);
        hint.setPadding(0, dp(10), 0, 0);
        content.addView(hint);

        new AlertDialog.Builder(this)
                .setTitle("外观 / 皮肤")
                .setView(content)
                .setPositiveButton("完成", null)
                .show();
    }

    private void editArea(final int area) {
        final AreaTheme t = area == 0 ? keyboardTheme : trackpadTheme;
        themeAlphaSeek = null;
        themeAlphaValue = null;

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(6), dp(20), dp(6));

        final CheckBox imageMode = new CheckBox(this);
        imageMode.setText("使用图片作背景");
        imageMode.setTextSize(13);
        imageMode.setTextColor(0xFFC6D0DC);
        imageMode.setChecked(t.mode == AreaTheme.MODE_IMAGE && t.imageName != null);
        content.addView(imageMode);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        Button pick = dialogButtonSmall("从相册选图");
        Button clear = dialogButtonSmall("清除图片");
        pick.setOnClickListener(v -> {
            editingArea = area;
            try {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                i.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(i, REQ_PICK_IMAGE);
            } catch (Exception e) {
                toast("打不开相册选择器");
            }
        });
        clear.setOnClickListener(v -> {
            t.imageName = null;
            t.mode = AreaTheme.MODE_COLOR;
            imageMode.setChecked(false);
            setAreaBitmap(area, null);
            saveThemes();
            applyTheme();
            toast("已清除背景图片");
        });
        btnRow.addView(pick);
        btnRow.addView(clear);
        content.addView(btnRow);

        imageMode.setOnCheckedChangeListener((buttonView, checked) -> {
            t.mode = checked ? AreaTheme.MODE_IMAGE : AreaTheme.MODE_COLOR;
            if (checked && t.imageName == null) {
                toast("请先点「从相册选图」");
            } else if (checked && t.keyAlpha > 0.6f) {
                // 用图片做按键背景时，默认让按键半透明，图片才透得出来
                t.keyAlpha = 0.35f;
                if (themeAlphaSeek != null) {
                    themeAlphaSeek.setProgress(35);
                }
            }
            saveThemes();
            applyTheme();
        });

        addSliderRow(content, "背景色相", 360, Math.round(t.bgHue), 1f, "°", v -> {
            t.bgHue = v;
            saveThemes();
            applyTheme();
        });
        addSliderRow(content, "背景饱和度", 100, Math.round(t.bgSat * 100), 0.01f, "%", v -> {
            t.bgSat = v;
            saveThemes();
            applyTheme();
        });
        addSliderRow(content, "背景明度", 100, Math.round(t.bgVal * 100), 0.01f, "%", v -> {
            t.bgVal = v;
            saveThemes();
            applyTheme();
        });
        addSliderRow(content, "压暗", 100, Math.round(t.darken * 100), 0.01f, "%", v -> {
            t.darken = v;
            saveThemes();
            applyTheme();
        });
        addSliderRow(content, "提亮", 100, Math.round(t.lighten * 100), 0.01f, "%", v -> {
            t.lighten = v;
            saveThemes();
            applyTheme();
        });
        addSliderRow(content, "按键色相", 360, Math.round(t.keyHue), 1f, "°", v -> {
            t.keyHue = v;
            saveThemes();
            applyTheme();
        });
        addSliderRow(content, "按键饱和度", 100, Math.round(t.keySat * 100), 0.01f, "%", v -> {
            t.keySat = v;
            saveThemes();
            applyTheme();
        });
        addSliderRow(content, "按键明度", 100, Math.round(t.keyVal * 100), 0.01f, "%", v -> {
            t.keyVal = v;
            saveThemes();
            applyTheme();
        });
        final SeekBar alphaSeek = makeSeekBar(100, Math.round(t.keyAlpha * 100));
        final TextView alphaValue = optionValue(sliderText(alphaSeek.getProgress(), 0.01f, "%"));
        alphaSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int p, boolean fromUser) {
                alphaValue.setText(sliderText(p, 0.01f, "%"));
                t.keyAlpha = p * 0.01f;
                saveThemes();
                applyTheme();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        content.addView(settingRow("按键透明度", alphaSeek, alphaValue));
        themeAlphaSeek = alphaSeek;
        themeAlphaValue = alphaValue;

        if (area == 1) {
            TextView tip = new TextView(this);
            tip.setText("触控板用图片作背景时，整块触控区域就是这张图，提示文字浮在图片上；纯色模式下按键颜色 / 透明度控制中间面板。");
            tip.setTextSize(12);
            tip.setTextColor(0xFF8E9AAA);
            tip.setPadding(0, dp(10), 0, 0);
            content.addView(tip);
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(content);
        new AlertDialog.Builder(this)
                .setTitle(area == 0 ? "键盘外观" : "触控板外观")
                .setView(scrollView)
                .setPositiveButton("完成", null)
                .show();
    }

    private interface ValueSetter {
        void set(float value);
    }

    private void addSliderRow(LinearLayout parent, String label, int max, int progress,
                              final float scale, final String suffix, final ValueSetter setter) {
        final TextView value = optionValue(sliderText(progress, scale, suffix));
        SeekBar bar = makeSeekBar(max, progress);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int p, boolean fromUser) {
                value.setText(sliderText(p, scale, suffix));
                setter.set(p * scale);
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        parent.addView(settingRow(label, bar, value));
    }

    private static String sliderText(int progress, float scale, String suffix) {
        float v = progress * scale;
        if ("°".equals(suffix)) return Math.round(v) + "°";
        return Math.round(v * 100) + "%";
    }

    private Button dialogButton(String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setPadding(dp(14), 0, dp(14), 0);
        b.setOnClickListener(listener);
        b.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));
        return b;
    }

    private Button dialogButtonSmall(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setPadding(dp(10), 0, dp(10), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        lp.rightMargin = dp(6);
        b.setLayoutParams(lp);
        return b;
    }

    private void saveThemes() {
        ThemeStore.saveKeyboard(this, keyboardTheme);
        ThemeStore.saveTrackpad(this, trackpadTheme);
    }

    private void applyTheme() {
        AreaStyle kb = AreaStyle.from(keyboardTheme, keyboardBitmap);
        AreaStyle tp = AreaStyle.from(trackpadTheme, trackpadBitmap);
        // 整页背景跟随当前模式：键盘模式用键盘主题，触控板模式用触控板主题
        AreaStyle pageStyle = (mode == 1) ? tp : kb;
        if (pageBg != null) pageBg.setStyle(pageStyle);
        if (rootView != null) rootView.setBackground(pageBg);
        if (statusView != null) statusView.setTextColor(pageStyle.hintText);
        if (keyboardView != null) {
            keyboardView.setDrawOwnBackground(false);
            keyboardView.setStyle(kb);
        }
        if (trackpadView != null) {
            trackpadView.setDrawOwnBackground(false);
            trackpadView.setStyle(tp);
        }
        if (gamepadView != null) gamepadView.setStyle(kb);
    }

    private void setAreaBitmap(int area, Bitmap bmp) {
        if (area == 0) keyboardBitmap = bmp;
        else trackpadBitmap = bmp;
    }

    private Bitmap loadThemeBitmap(String name) {
        if (name == null) return null;
        try {
            File f = new File(getFilesDir(), name);
            if (!f.exists()) return null;
            return BitmapFactory.decodeFile(f.getAbsolutePath());
        } catch (Exception e) {
            return null;
        }
    }

    private void handlePickedImage(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        String name = editingArea == 0 ? "theme_kb.jpg" : "theme_tp.jpg";
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream in = getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(in, null, bounds);
            if (in != null) in.close();

            int sample = 1;
            int maxSide = 2048;
            while (bounds.outWidth / (sample * 2) >= maxSide
                    || bounds.outHeight / (sample * 2) >= maxSide) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            in = getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(in, null, opts);
            if (in != null) in.close();
            if (bmp == null) {
                toast("图片读取失败，换一张试试");
                return;
            }

            File f = new File(getFilesDir(), name);
            OutputStream out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.JPEG, 88, out);
            out.close();
            bmp.recycle();

            AreaTheme t = editingArea == 0 ? keyboardTheme : trackpadTheme;
            t.imageName = name;
            t.mode = AreaTheme.MODE_IMAGE;
            // 图片作为按键背景：默认按键半透明，图片从键帽里透出来
            if (t.keyAlpha > 0.6f) t.keyAlpha = 0.35f;
            setAreaBitmap(editingArea, loadThemeBitmap(name));
            saveThemes();
            applyTheme();
            if (themeAlphaSeek != null) {
                themeAlphaSeek.setProgress(Math.round(t.keyAlpha * 100));
            }
            toast("背景图片已设置（按键默认半透明，可在「按键透明度」里调）");
        } catch (Exception e) {
            toast("设置图片失败：" + e.getMessage());
        }
    }

    /** 一行设置：左标签 + 中间滑杆（占满）+ 右侧数值。 */
    private LinearLayout settingRow(String label, SeekBar bar, TextView value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, 0);

        TextView name = new TextView(this);
        name.setText(label);
        name.setTextSize(13);
        name.setTextColor(0xFFC6D0DC);
        row.addView(name, new LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.WRAP_CONTENT));

        row.addView(bar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(value);
        return row;
    }

    private TextView optionValue(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTextColor(0xFFD0D9E5);
        tv.setMinWidth(dp(56));
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    private SeekBar makeSeekBar(int max, int progress) {
        SeekBar bar = new SeekBar(this);
        bar.setMax(max);
        bar.setProgress(Math.max(0, Math.min(max, progress)));
        return bar;
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static String fmt(float v) {
        return String.format(Locale.US, "%.2f×", v);
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setPadding(dp(12), 0, dp(12), 0);
        b.setMinHeight(dp(30));
        b.setMinimumHeight(dp(30));
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
        lp.leftMargin = dp(6);
        b.setLayoutParams(lp);
        return b;
    }

    // ── 蓝牙初始化 ────────────────────────────────────────

    private void requestBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean needConnect = checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED;
            boolean needAdvertise = checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
                    != PackageManager.PERMISSION_GRANTED;
            if (needConnect || needAdvertise) {
                requestPermissions(new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_ADVERTISE
                }, REQ_PERMISSIONS);
                return;
            }
        }
        ensureBluetoothOn();
    }

    private void ensureBluetoothOn() {
        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bm == null ? null : bm.getAdapter();
        if (adapter == null) {
            statusView.setText("这台手机没有蓝牙");
            return;
        }
        if (!adapter.isEnabled()) {
            statusView.setText("请先打开蓝牙");
            try {
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_ENABLE_BT);
            } catch (Exception e) {
                statusView.setText("无法自动打开蓝牙，请在系统设置里开启");
            }
            return;
        }
        if (!started) {
            started = true;
            hid.start();
        }
    }

    private void requestDiscoverable() {
        try {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
            startActivityForResult(i, REQ_DISCOVERABLE);
        } catch (SecurityException e) {
            toast("缺少「附近设备」权限，无法设置可见");
        }
    }

    private void showGuide() {
        String msg = "1. 打开电脑的蓝牙（Windows：设置 → 蓝牙和设备 → 添加设备）\n"
                + "2. 手机上点「让电脑搜到我」，让手机进入可发现状态\n"
                + "3. 电脑列表里选择手机「" + hid.deviceName() + "」进行配对\n"
                + "4. 配对成功后本页顶部会显示「已连接」，键盘和触控板都可以用了\n\n"
                + "提示：\n"
                + "• 如果不显示，先在电脑上删除旧的配对记录再重试\n"
                + "• 一次只能连一台电脑；换电脑需先在原电脑断开\n"
                + "• 手机必须 Android 9 以上，个别系统裁剪了蓝牙 HID 功能会提示不支持";
        new AlertDialog.Builder(this)
                .setTitle("怎么连电脑")
                .setMessage(msg)
                .setPositiveButton("知道了", null)
                .show();
    }

    // ── HidKeyboardManager.Listener ──────────────────────

    @Override
    public void onStatusChanged(String status) {
        runOnUiThread(() -> statusView.setText(status));
    }

    @Override
    public void onConnectionChanged(boolean connected, String deviceName) {
        // 状态文字已由 onStatusChanged 更新，这里留作以后按钮联动
    }

    // ── 生命周期与回调 ────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_PERMISSIONS) return;
        boolean ok = grantResults.length > 0;
        for (int r : grantResults) ok = ok && r == PackageManager.PERMISSION_GRANTED;
        if (ok) {
            ensureBluetoothOn();
        } else {
            statusView.setText("需要「附近的设备」权限才能当键盘，请到系统设置里手动授权");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_IMAGE) {
            handlePickedImage(resultCode, data);
            return;
        }
        if (requestCode == REQ_ENABLE_BT) {
            if (resultCode == RESULT_OK) ensureBluetoothOn();
            else statusView.setText("蓝牙没有打开，无法注册键盘");
        } else if (requestCode == REQ_DISCOVERABLE) {
            if (resultCode > 0) {
                statusView.setText("手机可见 " + resultCode + " 秒，请到电脑蓝牙里添加设备");
            }
        }
    }

    @Override
    protected void onPause() {
        if (keyboardView != null) keyboardView.releaseAll();
        if (trackpadView != null) trackpadView.reset();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 回到前台时：如果之前连过电脑，主动把 HID 连接拉起来（Windows 不会自己连）
        if (started && hid != null && keyArea != null) {
            keyArea.postDelayed(() -> hid.reconnectNow(), 600);
        }
    }

    // ── 全屏与安全区 ──────────────────────────────────────

    /**
     * 键盘要尽量吃满屏幕：隐藏系统状态栏/导航栏（沉浸式），
     * 同时用窗口边距兜底 —— 状态栏临时滑出、刘海/挖孔、手势条都不会压住顶栏。
     */
    private void setupImmersive(LinearLayout root) {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        } else {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
        applyImmersive();

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int left = 0, top = 0, right = 0, bottom = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
                DisplayCutout cutout = insets.getDisplayCutout();
                if (cutout != null) {
                    left = Math.max(left, cutout.getSafeInsetLeft());
                    top = Math.max(top, cutout.getSafeInsetTop());
                    right = Math.max(right, cutout.getSafeInsetRight());
                    bottom = Math.max(bottom, cutout.getSafeInsetBottom());
                }
            }
            if (v.getPaddingLeft() != left || v.getPaddingTop() != top
                    || v.getPaddingRight() != right || v.getPaddingBottom() != bottom) {
                v.setPadding(left, top, right, bottom);
            }
            return insets;
        });
    }

    private void applyImmersive() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersive();
    }

    @Override
    protected void onDestroy() {
        if (keyboardView != null) keyboardView.releaseAll();
        if (trackpadView != null) trackpadView.reset();
        if (gamepadView != null) gamepadView.reset();
        if (hid != null) {
            hid.stop();
        }
        super.onDestroy();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
