package org.alexdlc.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import org.alexdlc.context.MinecraftContext;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.lifecycle.FeatureToggleEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.utils.ColorUtil;
import org.alexdlc.utils.render.Theme;
import org.alexdlc.utils.render.gui.MsdfFont;
import org.alexdlc.utils.render.gui.Render2DUtil;
import org.alexdlc.utils.render.gui.UiFontStyle;
import org.alexdlc.utils.render.gui.UiFonts;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.alexdlc.utils.render.Textures;

public final class NotificationsElement extends HudElement {
    private static final long LIFETIME_MS = 4000L;
    private static final long APPEAR_MS = 200L;
    private static final long FADE_MS = 300L;
    private static final int MAX_NOTES = 5;
    private static final int EXPIRING_TICKS = 20 * 20;
    private static final float STACK_GAP = 8.0F;
    private static final float TEXT_SIZE = 10.0F;
    private static final float TEXT_SEGMENT_GAP = 3.0F;
    private static final float SLIDE_PX = 8.0F;

    private static final String[] ROMAN = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};

    private static final Identifier[] CATEGORY_ICONS = {
            Textures.Icons.SWORDS,
            Textures.Icons.PERSON_STANDING,
            Textures.Icons.EYE,
            Textures.Icons.USER_ROUND,
            Textures.Icons.BOXES,
            Textures.Icons.BRAIN
    };

    private record Style(float height, float radius, float padding, float gap, float iconSize) {
    }

    private static final Style MINI = new Style(28.0F, 8.0F, 8.0F, 4.0F, 12.0F);

    private record Segment(String text, int color) {
    }

    private record Note(Identifier icon, int iconTint, boolean managedIcon, List<Segment> segments, long createdAt) {
    }

    private static final Deque<Note> NOTES = new ArrayDeque<>();

    private record EffectState(int duration, int amplifier, boolean expiryNotified) {
    }

    private final Map<Identifier, EffectState> trackedEffects = new HashMap<>();
    private final List<Note> visible = new ArrayList<>();
    private final Style style = MINI;

    public NotificationsElement() {
        super("notifications", "Notifications");
    }

    @Override
    protected boolean centerHorizontally() {
        return true;
    }

    @Override
    protected float defaultY(float unit) {
        return 420.0F * unit;
    }

    public static final class ToggleListener {
        @EventTarget
        public void onFeatureToggle(FeatureToggleEvent event) {
            featureToggled(event.getFeature(), event.isEnabled());
        }
    }

    private static void featureToggled(Feature feature, boolean enabled) {
        Minecraft mc = MinecraftContext.mc;
        if (mc == null || mc.level == null) {
            return;
        }
        push(new Note(
                categoryIcon(feature.getCategory()),
                enabled ? Theme.getAccent() : Theme.Colors.TEXT_GHOST,
                false,
                List.of(
                        new Segment("The", Theme.Colors.TEXT_TEXT),
                        new Segment(feature.getName(), Theme.Colors.TEXT_TITLE),
                        new Segment(enabled ? "feature has been enabled" : "feature has been disabled",
                                Theme.Colors.TEXT_TEXT)
                ),
                System.currentTimeMillis()
        ));
    }

    private static void push(Note note) {
        NOTES.addLast(note);
        while (NOTES.size() > MAX_NOTES) {
            NOTES.removeFirst();
        }
    }

    @Override
    protected void layout(Minecraft mc, float unit) {
        trackEffects(mc);

        long now = System.currentTimeMillis();
        NOTES.removeIf(note -> now - note.createdAt() >= LIFETIME_MS);

        this.visible.clear();
        this.visible.addAll(NOTES);
        if (this.visible.isEmpty() && showcase(mc)) {
            this.visible.add(exampleNote(now));
        }
        if (this.visible.isEmpty()) {
            this.width = 0.0F;
            this.height = 0.0F;
            return;
        }

        MsdfFont font = UiFonts.sfPro(UiFontStyle.MEDIUM.weight());
        float maxWidth = 0.0F;
        float stackHeight = 0.0F;
        for (Note note : this.visible) {
            maxWidth = Math.max(maxWidth, pillWidth(font, note, unit));
            stackHeight += (this.style.height() + STACK_GAP) * unit * envelope(note, now);
        }
        this.width = maxWidth;
        this.height = Math.max(1.0F, stackHeight - STACK_GAP * unit);
    }

    @Override
    protected void draw(Minecraft mc, float unit) {
        MsdfFont font = UiFonts.sfPro(UiFontStyle.MEDIUM.weight());
        long now = System.currentTimeMillis();
        float guiWidth = mc.getWindow().getGuiScaledWidth();
        float pillY = this.y;
        for (Note note : this.visible) {
            float envelope = envelope(note, now);
            if (envelope > 0.01F) {
                drawPill(font, note, guiWidth, pillY, unit, now, envelope);
            }

            pillY += (this.style.height() + STACK_GAP) * unit * envelope;
        }
    }

    private static float envelope(Note note, long now) {
        long age = now - note.createdAt();
        float appear = Math.clamp(age / (float) APPEAR_MS, 0.0F, 1.0F);
        float disappear = Math.clamp((LIFETIME_MS - age) / (float) FADE_MS, 0.0F, 1.0F);
        float value = Math.min(appear, disappear);
        return value * value * (3.0F - 2.0F * value);
    }

    private void drawPill(MsdfFont font, Note note, float guiWidth, float pillY, float unit, long now, float alpha) {
        float pillHeight = this.style.height() * unit;
        float pillWidth = pillWidth(font, note, unit);

        float pillX = (guiWidth - pillWidth) / 2.0F;
        long age = now - note.createdAt();
        float appear = Math.clamp(age / (float) APPEAR_MS, 0.0F, 1.0F);
        pillY -= (1.0F - appear * appear * (3.0F - 2.0F * appear)) * SLIDE_PX * unit;

        float centerY = pillY + pillHeight / 2.0F;
        float textSize = TEXT_SIZE * unit;
        float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();

        Render2DUtil.rect(pillX, pillY, pillWidth, pillHeight)
                .glass(alpha, 0.5F * unit, 8.0F * unit)
                .radius(this.style.radius() * unit)
                .draw();

        float iconSize = this.style.iconSize() * unit;
        float gap = this.style.gap() * unit;
        float cursor = pillX + this.style.padding() * unit;

        var icon = Render2DUtil.texture(cursor, centerY - iconSize / 2.0F, iconSize, iconSize, note.icon());
        if (note.managedIcon()) {
            icon.managed();
            icon.color(ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha));
        } else {
            icon.color(ColorUtil.multiplyAlpha(note.iconTint(), alpha));
        }
        icon.draw();
        cursor += iconSize + gap;

        Render2DUtil.rect(cursor, centerY - iconSize / 2.0F, Math.max(0.5F, 0.5F * unit), iconSize)
                .color(ColorUtil.multiplyAlpha(DIVIDER_COLOR, alpha))
                .draw();
        cursor += Math.max(0.5F, 0.5F * unit) + gap;

        float textY = font.centeredTextY(centerY, textSize);
        for (int index = 0; index < note.segments().size(); index++) {
            Segment segment = note.segments().get(index);
            if (index > 0) {
                cursor += TEXT_SEGMENT_GAP * unit;
            }
            Render2DUtil.text(cursor, textY, textSize, segment.text())
                    .style(UiFontStyle.MEDIUM)
                    .color(ColorUtil.multiplyAlpha(segment.color(), alpha))
                    .draw();
            cursor += font.measureWidth(segment.text(), textSize, letterSpacing);
        }
    }

    private float pillWidth(MsdfFont font, Note note, float unit) {
        float textSize = TEXT_SIZE * unit;
        float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
        float textWidth = 0.0F;
        for (Segment segment : note.segments()) {
            textWidth += font.measureWidth(segment.text(), textSize, letterSpacing);
        }
        textWidth += Math.max(0, note.segments().size() - 1) * TEXT_SEGMENT_GAP * unit;
        return (this.style.padding() * 2.0F + this.style.iconSize() + this.style.gap() * 2.0F) * unit
                + Math.max(0.5F, 0.5F * unit)
                + textWidth;
    }

    private void trackEffects(Minecraft mc) {
        if (mc.player == null) {
            this.trackedEffects.clear();
            return;
        }

        Map<Identifier, EffectState> current = new HashMap<>();
        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            Identifier effectId = effect.getEffect().unwrapKey().orElseThrow().identifier();
            int duration = effect.getDuration();
            int amplifier = effect.getAmplifier();
            EffectState previous = this.trackedEffects.get(effectId);

            boolean applied = previous == null
                    || amplifier != previous.amplifier()

                    || (!effect.isInfiniteDuration() && duration > previous.duration() + 200);
            boolean expiryNotified = previous != null && previous.expiryNotified();
            if (applied) {
                pushEffect(effect, "effect applied for", duration);
                expiryNotified = false;
            } else if (!expiryNotified && !effect.isInfiniteDuration() && duration <= EXPIRING_TICKS) {
                pushEffect(effect, "effect expires in", duration);
                expiryNotified = true;
            }
            current.put(effectId, new EffectState(duration, amplifier, expiryNotified));
        }
        this.trackedEffects.clear();
        this.trackedEffects.putAll(current);
    }

    private static void pushEffect(MobEffectInstance effect, String middle, int durationTicks) {
        Identifier effectId = effect.getEffect().unwrapKey().orElseThrow().identifier();
        Identifier icon = effectId.withPath(path -> "textures/mob_effect/" + path + ".png");
        boolean harmful = effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL;
        String name = effect.getEffect().value().getDisplayName().getString() + roman(effect.getAmplifier());
        String duration = effect.isInfiniteDuration() ? "\u221E" : formatTicks(durationTicks);
        push(new Note(
                icon,
                0,
                true,
                List.of(
                        new Segment(name, harmful ? Theme.Colors.SYSTEM_RED : Theme.Colors.TRAFFIC_MAXIMIZE),
                        new Segment(middle, Theme.Colors.TEXT_TEXT),
                        new Segment(duration, Theme.Colors.TEXT_TITLE)
                ),
                System.currentTimeMillis()
        ));
    }

    private static String roman(int amplifier) {
        return amplifier > 0 && amplifier < ROMAN.length ? ROMAN[amplifier] : amplifier >= ROMAN.length ? " " + (amplifier + 1) : "";
    }

    private static String formatTicks(int ticks) {
        int seconds = ticks / 20;
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    private Note exampleNote(long now) {
        return new Note(categoryIcon(FeatureCategory.VISUAL), Theme.getAccent(), false, List.of(
                new Segment("The", Theme.Colors.TEXT_TEXT),
                new Segment("ExampleFeature", Theme.Colors.TEXT_TITLE),
                new Segment("feature has been enabled", Theme.Colors.TEXT_TEXT)
        ), now - LIFETIME_MS / 2L);
    }

    private static Identifier categoryIcon(FeatureCategory category) {
        int index = category.ordinal();
        return CATEGORY_ICONS[index >= 0 && index < CATEGORY_ICONS.length ? index : 0];
    }
}
