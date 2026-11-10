package com.zhangshang.keyboard;

import java.util.ArrayList;
import java.util.List;

/** 编辑快捷键时可选按键表：字母、数字、符号、功能键与编辑键。 */
public final class MacroKeys {
    public static final String[] LABELS;
    public static final int[] USAGES;

    static {
        List<String> labels = new ArrayList<>();
        List<Integer> usages = new ArrayList<>();

        for (char c = 'A'; c <= 'Z'; c++) {
            labels.add(String.valueOf(c));
            usages.add(HidKeys.usageFor(c));
        }
        for (char c = '1'; c <= '9'; c++) {
            labels.add(String.valueOf(c));
            usages.add(HidKeys.usageFor(c));
        }
        labels.add("0");
        usages.add(HidKeys.usageFor('0'));

        String symbols = "`-=[]\\;',./";
        for (int i = 0; i < symbols.length(); i++) {
            char c = symbols.charAt(i);
            labels.add(String.valueOf(c));
            usages.add(HidKeys.usageFor(c));
        }

        String[] special = {
                "Esc", "Tab", "Enter", "Space", "Backspace", "Delete", "Insert",
                "Home", "End", "PageUp", "PageDown",
                "Up", "Down", "Left", "Right", "PrintScreen"
        };
        int[] specialUsage = {
                HidKeys.ESC, HidKeys.TAB, HidKeys.ENTER, HidKeys.SPACE, HidKeys.BACKSPACE,
                HidKeys.DELETE, HidKeys.INSERT, HidKeys.HOME, HidKeys.END,
                HidKeys.PAGE_UP, HidKeys.PAGE_DOWN,
                HidKeys.UP, HidKeys.DOWN, HidKeys.LEFT, HidKeys.RIGHT, HidKeys.PRINT_SCREEN
        };
        for (int i = 0; i < special.length; i++) {
            labels.add(special[i]);
            usages.add(specialUsage[i]);
        }

        for (int i = 1; i <= 12; i++) {
            labels.add("F" + i);
            usages.add(HidKeys.F1 + i - 1);
        }

        LABELS = labels.toArray(new String[0]);
        USAGES = new int[usages.size()];
        for (int i = 0; i < USAGES.length; i++) USAGES[i] = usages.get(i);
    }

    private MacroKeys() {}

    public static int indexOfUsage(int usage) {
        for (int i = 0; i < USAGES.length; i++) {
            if (USAGES[i] == usage) return i;
        }
        return 0;
    }

    public static String labelFor(int usage) {
        return LABELS[indexOfUsage(usage)];
    }
}
