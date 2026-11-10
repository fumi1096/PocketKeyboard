package com.zhangshang.keyboard;

import android.content.Context;
import android.content.SharedPreferences;

/** 键盘 / 触控板两套外观的本地存储。 */
public final class ThemeStore {
    private static final String PREFS = "pocket_keyboard";
    private static final String KEY_KB = "theme_area_kb";
    private static final String KEY_TP = "theme_area_tp";

    private ThemeStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static AreaTheme loadKeyboard(Context context) {
        return AreaTheme.fromJson(prefs(context).getString(KEY_KB, null), AreaTheme.keyboardDefault());
    }

    public static AreaTheme loadTrackpad(Context context) {
        return AreaTheme.fromJson(prefs(context).getString(KEY_TP, null), AreaTheme.trackpadDefault());
    }

    public static void saveKeyboard(Context context, AreaTheme theme) {
        prefs(context).edit().putString(KEY_KB, theme.toJson()).apply();
    }

    public static void saveTrackpad(Context context, AreaTheme theme) {
        prefs(context).edit().putString(KEY_TP, theme.toJson()).apply();
    }
}
