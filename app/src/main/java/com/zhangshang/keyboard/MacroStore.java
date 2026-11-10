package com.zhangshang.keyboard;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 快捷键的本地存储（SharedPreferences + JSON）。 */
public final class MacroStore {
    private static final String PREFS = "pocket_keyboard";
    private static final String KEY_MACROS = "macros";

    private MacroStore() {}

    public static List<Macro> load(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = sp.getString(KEY_MACROS, null);
        if (raw == null) return defaults();
        try {
            JSONArray arr = new JSONArray(raw);
            List<Macro> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new Macro(o.optString("name"), o.optInt("mods"), o.optInt("usage")));
            }
            if (list.isEmpty()) return defaults();
            return list;
        } catch (Exception e) {
            return defaults();
        }
    }

    public static void save(Context context, List<Macro> macros) {
        JSONArray arr = new JSONArray();
        for (Macro m : macros) {
            JSONObject o = new JSONObject();
            try {
                o.put("name", m.name);
                o.put("mods", m.mods);
                o.put("usage", m.usage);
            } catch (Exception ignored) {
            }
            arr.put(o);
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_MACROS, arr.toString()).apply();
    }

    /** 内置默认快捷键。 */
    public static List<Macro> defaults() {
        List<Macro> list = new ArrayList<>();
        list.add(new Macro("Ctrl+Alt+Del", 0x05, HidKeys.DELETE));
        list.add(new Macro("任务管理器", 0x03, HidKeys.ESC));
        list.add(new Macro("Alt+Tab", 0x04, HidKeys.TAB));
        list.add(new Macro("Alt+F4", 0x04, HidKeys.F4));
        list.add(new Macro("Win+L", 0x08, HidKeys.usageFor('L')));
        list.add(new Macro("Win+D", 0x08, HidKeys.usageFor('D')));
        list.add(new Macro("截图", 0x0A, HidKeys.usageFor('S')));
        return list;
    }
}
