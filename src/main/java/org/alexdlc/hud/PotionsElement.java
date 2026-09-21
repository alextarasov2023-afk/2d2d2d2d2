package org.alexdlc.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import org.alexdlc.utils.ColorUtil;
import org.alexdlc.utils.render.Theme;
import org.alexdlc.utils.render.gui.MsdfFont;
import org.alexdlc.utils.render.gui.Render2DUtil;
import org.alexdlc.utils.render.gui.TextAlign;
import org.alexdlc.utils.render.gui.UiFontStyle;
import org.alexdlc.utils.render.gui.UiFonts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PotionsElement extends HudElement {
    private static final int BAR_TRACK_COLOR = ColorUtil.rgba(255, 255, 255, 20);
    private static final int BAR_GREEN = Theme.Colors.TRAFFIC_MAXIMIZE;
    private static final int BAR_YELLOW = Theme.Colors.TRAFFIC_MINIMIZE;

    private static final float TEXT_SIZE = 10.0F;
    private static final float BAR_HEIGHT = 3.0F;

    private record Style(
            float radius,
            float padding,
            float dividerGap,
            float cardWidth,
            float cardHeight,
            float iconSize,
            float iconTextGap,
            float textHeight,
            float textBarGap,
            float barWidth
    ) {
    }

    private static final Style MINI = new Style(8.0F, 8.0F, 4.0F, 31.0F, 32.0F, 14.0F, 2.0F, 11.0F, 2.0F, 16.0F);

    private record Card(Identifier icon, String time, boolean harmful, float fraction) {
    }

    private final List<Card> cards = new ArrayList<>();
    private final Map<Identifier, Integer> maxDurations = new HashMap<>();
    private final Style style = MINI;

    public PotionsElement() {
        super("potions", "Potions");
    }

    @Override
    protected float defaultY(float unit) {
        return 260.0F * unit;
    }

    @Override
    protected void layout(Minecraft mc, float unit) {
        collectCards(mc);
        if (this.cards.isEmpty()) {
            this.width = 0.0F;
            this.height = 0.0F;
            return;
        }

        this.width = (this.style.padding() * 2.0F
                + this.cards.size() * this.style.cardWidth()
                + (this.cards.size() - 1) * (this.style.dividerGap() * 2.0F + 1.0F)) * unit;
        this.height = (this.style.padding() * 2.0F + this.style.cardHeight()) * unit;
    }

    @Override
    protected void draw(Minecraft mc, float unit) {
        if (this.cards.isEmpty()) {
            return;
        }

        float alpha = appearAlpha();
        Render2DUtil.rect(this.x, this.y, this.width, this.height)
                .glass(alpha, 0.5F * unit, 8.0F * unit)
                .radius(this.style.radius() * unit)
                .draw();

        MsdfFont font = UiFonts.sfProDisplay();
        float textSize = TEXT_SIZE * unit;
        float cardTop = this.y + this.style.padding() * unit;
        float cursor = this.x + this.style.padding() * unit;

        for (int i = 0; i < this.cards.size(); i++) {
            Card card = this.cards.get(i);
            if (i > 0) {
                cursor = divider(cursor, cardTop + this.style.cardHeight() * unit / 2.0F,
                        this.style.cardHeight(), this.style.dividerGap(), unit, alpha);
            }

            float cardCenterX = cursor + this.style.cardWidth() * unit / 2.0F;

            float iconSize = this.style.iconSize() * unit;
            Render2DUtil.texture(cardCenterX - iconSize / 2.0F, cardTop, iconSize, iconSize, card.icon())
                    .managed()
                    .color(ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha))
                    .draw();

            float textCenterY = cardTop + (this.style.iconSize() + this.style.iconTextGap() + this.style.textHeight() / 2.0F) * unit;
            Render2DUtil.text(cardCenterX, font.centeredTextY(textCenterY, textSize), textSize, card.time())
                    .style(UiFontStyle.MEDIUM)
                    .color(ColorUtil.multiplyAlpha(card.harmful() ? Theme.Colors.SYSTEM_RED : Theme.Colors.TEXT_TITLE, alpha))
                    .align(TextAlign.CENTER)
                    .draw();

            float barY = cardTop + (this.style.cardHeight() - BAR_HEIGHT) * unit;
            float barX = cardCenterX - this.style.barWidth() * unit / 2.0F;
            float barRadius = BAR_HEIGHT * unit / 2.0F;
            Render2DUtil.rect(barX, barY, this.style.barWidth() * unit, BAR_HEIGHT * unit)
                    .color(ColorUtil.multiplyAlpha(BAR_TRACK_COLOR, alpha))
                    .radius(barRadius)
                    .draw();
            float fillWidth = this.style.barWidth() * unit * card.fraction();
            if (fillWidth >= BAR_HEIGHT * unit) {
                Render2DUtil.rect(barX, barY, fillWidth, BAR_HEIGHT * unit)
                        .color(ColorUtil.multiplyAlpha(barColor(card), alpha))
                        .radius(barRadius)
                        .draw();
            }

            cursor += this.style.cardWidth() * unit;
        }
    }

    private static int barColor(Card card) {
        if (card.harmful()) {
            return Theme.Colors.SYSTEM_RED;
        }
        if (card.fraction() > 0.5F) {
            return BAR_GREEN;
        }
        if (card.fraction() > 0.25F) {
            return BAR_YELLOW;
        }
        return Theme.Colors.SYSTEM_RED;
    }

    private void collectCards(Minecraft mc) {
        this.cards.clear();
        Map<Identifier, Integer> seen = new HashMap<>();
        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            Identifier effectId = effect.getEffect().unwrapKey().orElseThrow().identifier();
            Identifier icon = effectId.withPath(path -> "textures/mob_effect/" + path + ".png");
            boolean harmful = effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL;

            float fraction;
            String time;
            if (effect.isInfiniteDuration()) {
                fraction = 1.0F;
                time = "\u221E";
            } else {
                int duration = effect.getDuration();
                int max = Math.max(duration, this.maxDurations.getOrDefault(effectId, 0));
                seen.put(effectId, max);
                fraction = max > 0 ? (float) duration / max : 0.0F;
                int seconds = duration / 20;
                time = seconds / 60 + ":" + String.format("%02d", seconds % 60);
            }
            this.cards.add(new Card(icon, time, harmful, fraction));
        }

        this.maxDurations.clear();
        this.maxDurations.putAll(seen);

        if (this.cards.isEmpty() && showcase(mc)) {
            this.cards.add(new Card(effectIcon("fire_resistance"), "1:52", false, 0.9F));
            this.cards.add(new Card(effectIcon("speed"), "0:47", false, 0.4F));
            this.cards.add(new Card(effectIcon("hunger"), "0:30", true, 1.0F));
        }
    }

    private static Identifier effectIcon(String name) {
        return Identifier.withDefaultNamespace("textures/mob_effect/" + name + ".png");
    }
}
