package org.alexdlc.utils.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/**
 * Legacy name-substitution hook, now a pass-through: the real session
 * nickname is always shown everywhere instead of a fake display name.
 */
public final class NameProtectUtil {

    private NameProtectUtil() {
    }

    public static String protect(String text) {
        return text;
    }

    public static Component protect(Component component) {
        return component;
    }

    public static FormattedText protect(FormattedText text) {
        return text;
    }

    public static FormattedCharSequence protect(FormattedCharSequence sequence) {
        return sequence;
    }
}
