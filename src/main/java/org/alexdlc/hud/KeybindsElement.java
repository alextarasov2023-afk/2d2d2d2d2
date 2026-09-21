package org.alexdlc.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.alexdlc.event.EventManager;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.lifecycle.FeatureToggleEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureManager;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.menu.i18n.MenuText;
import org.alexdlc.utils.ColorUtil;
import org.alexdlc.utils.render.Theme;
import org.alexdlc.utils.render.gui.MsdfFont;
import org.alexdlc.utils.render.gui.Render2DUtil;
import org.alexdlc.utils.render.gui.TextAlign;
import org.alexdlc.utils.render.gui.UiFontStyle;
import org.alexdlc.utils.render.gui.UiFonts;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import org.alexdlc.utils.render.Textures;

public final class KeybindsElement extends HudElement {

    private static final float HEADER_ICON_SIZE = 16.0F;
    private static final float HEADER_HEIGHT = 20.0F;
    private static final float TITLE_SIZE = 12.0F;
    private static final float ROW_HEIGHT = 20.0F;
    private static final float ROW_TEXT_SIZE = 10.0F;
    private static final float CHIP_RADIUS = 4.0F;
    private static final float CHIP_PADDING_X = 8.0F;
    private static final float CHIP_MIN_WIDTH = 22.0F;
    private static final float MINI_NAME_MAX_WIDTH = 64.0F;

    private record Style(float minWidth, float radius, float paddingX, float paddingY, boolean header, float rowGap) {
    }

    private static final Style MINI = new Style(112.0F, 8.0F, 8.0F, 6.0F, false, 4.0F);

    private record Row(String name, String bind, boolean enabled) {
    }

    private final List<Row> rows = new ArrayList<>();
    private final Style style = MINI;

    private boolean rowsDirty = true;
    private int lastFingerprint;
    private float lastUnit = Float.NaN;

    public KeybindsElement() {
        super("keybinds", "Keybinds");

        EventManager.subscribe(this);
    }

    @EventTarget
    public void onFeatureToggle(FeatureToggleEvent event) {
        this.rowsDirty = true;
    }

    @Override
    protected float defaultY(float unit) {
        return 120.0F * unit;
    }

    @Override
    protected void layout(Minecraft mc, float unit) {

        int fingerprint = fingerprint(mc);
        if (this.rowsDirty || fingerprint != this.lastFingerprint || unit != this.lastUnit) {
            this.rowsDirty = false;
            this.lastFingerprint = fingerprint;
            this.lastUnit = unit;
            collectRows(mc, UiFonts.sfProDisplay(), unit);
        }
        if (this.rows.isEmpty()) {
            this.width = 0.0F;
            this.height = 0.0F;
            return;
        }

        MsdfFont font = UiFonts.sfProDisplay();
        float rowTextSize = ROW_TEXT_SIZE * unit;
        float letterSpacing = rowTextSize * UiFontStyle.MEDIUM.letterSpacingEm();

        float contentWidth = (this.style.minWidth() - this.style.paddingX() * 2.0F) * unit;
        for (Row row : this.rows) {
            float rowWidth = font.measureWidth(row.name(), rowTextSize, letterSpacing)
                    + this.style.rowGap() * unit
                    + chipWidth(font, row, unit);
            contentWidth = Math.max(contentWidth, rowWidth);
        }

        this.width = contentWidth + this.style.paddingX() * 2.0F * unit;
        float headerHeight = this.style.header() ? (HEADER_HEIGHT + 8.0F + 1.0F + 8.0F) * unit : 0.0F;
        this.height = this.style.paddingY() * 2.0F * unit
                + headerHeight
                + this.rows.size() * ROW_HEIGHT * unit
                + (this.rows.size() - 1) * this.style.rowGap() * unit;
    }

    @Override
    protected void draw(Minecraft mc, float unit) {
        if (this.rows.isEmpty()) {
            return;
        }

        float alpha = appearAlpha();
        Render2DUtil.rect(this.x, this.y, this.width, this.height)
                .glass(alpha, 0.5F * unit, 8.0F * unit)
                .radius(this.style.radius() * unit)
                .draw();

        MsdfFont font = UiFonts.sfProDisplay();
        float contentX = this.x + this.style.paddingX() * unit;
        float contentRight = this.x + this.width - this.style.paddingX() * unit;
        float cursorY = this.y + this.style.paddingY() * unit;

        if (this.style.header()) {
            float headerCenterY = cursorY + HEADER_HEIGHT * unit / 2.0F;
            float iconSize = HEADER_ICON_SIZE * unit;
            Render2DUtil.texture(contentX, headerCenterY - iconSize / 2.0F, iconSize, iconSize, Textures.Icons.KEYBOARD)
                    .color(ColorUtil.multiplyAlpha(Theme.Colors.ICON_GHOST, alpha))
                    .draw();
            float titleSize = TITLE_SIZE * unit;
            Render2DUtil.text(contentX + iconSize + 8.0F * unit,
                            font.centeredTextY(headerCenterY, titleSize), titleSize,
                            MenuText.option("Keybinds"))
                    .style(UiFontStyle.MEDIUM)
                    .color(ColorUtil.multiplyAlpha(Theme.Colors.TEXT_TITLE, alpha))
                    .draw();
            cursorY += (HEADER_HEIGHT + 8.0F) * unit;

            Render2DUtil.rect(contentX, cursorY, contentRight - contentX, 1.0F * unit)
                    .color(ColorUtil.multiplyAlpha(Theme.Colors.OUTLINES_MEDIUM, alpha))
                    .draw();
            cursorY += (1.0F + 8.0F) * unit;
        }

        float rowTextSize = ROW_TEXT_SIZE * unit;
        for (Row row : this.rows) {
            float rowCenterY = cursorY + ROW_HEIGHT * unit / 2.0F;
            Render2DUtil.text(contentX, font.centeredTextY(rowCenterY, rowTextSize), rowTextSize, row.name())
                    .style(UiFontStyle.MEDIUM)
                    .color(ColorUtil.multiplyAlpha(Theme.Colors.TEXT_TEXT, alpha))
                    .draw();

            float chipWidth = chipWidth(font, row, unit);
            float chipX = contentRight - chipWidth;
            Render2DUtil.rect(chipX, cursorY, chipWidth, ROW_HEIGHT * unit)
                    .color(ColorUtil.multiplyAlpha(row.enabled() ? Theme.getAccent() : Theme.Colors.OUTLINES_MEDIUM, alpha))
                    .radius(CHIP_RADIUS * unit)
                    .draw();
            Render2DUtil.text(chipX + chipWidth / 2.0F,
                            font.centeredTextY(rowCenterY, rowTextSize), rowTextSize,
                            MenuText.bindCombination(row.bind()))
                    .style(UiFontStyle.MEDIUM)
                    .color(ColorUtil.multiplyAlpha(row.enabled() ? ColorUtil.WHITE : Theme.Colors.ICON, alpha))
                    .align(TextAlign.CENTER)
                    .draw();

            cursorY += (ROW_HEIGHT + this.style.rowGap()) * unit;
        }
    }

    private float chipWidth(MsdfFont font, Row row, float unit) {
        float textSize = ROW_TEXT_SIZE * unit;
        float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
        float textWidth = font.measureWidth(MenuText.bindCombination(row.bind()), textSize, letterSpacing);
        return Math.max(CHIP_MIN_WIDTH * unit, textWidth + CHIP_PADDING_X * 2.0F * unit);
    }

    private int fingerprint(Minecraft mc) {
        int fp = showcase(mc) ? 1 : 0;
        for (Feature feature : FeatureManager.INSTANCE.getFeatures()) {
            fp = fp * 31 + feature.getName().hashCode();
            List<Integer> bindCodes = feature.getBinds();
            fp = fp * 31 + bindCodes.hashCode();

            for (int i = 0; i < bindCodes.size(); i++) {
                fp = fp * 31 + (feature.isBindVisibleAt(i) ? 1 : 0);
            }
            fp = fp * 31 + (feature.isEnabled() ? 1 : 0);
        }
        return fp;
    }

    private void collectRows(Minecraft mc, MsdfFont font, float unit) {
        this.rows.clear();
        for (Feature feature : FeatureManager.INSTANCE.getFeatures()) {
            StringJoiner binds = new StringJoiner(" + ");
            List<Integer> bindCodes = feature.getBinds();
            for (int i = 0; i < bindCodes.size(); i++) {
                int bindCode = bindCodes.get(i);
                if (bindCode != BindSetting.UNBOUND && feature.isBindVisibleAt(i)) {
                    binds.add(BindSetting.describe(bindCode));
                }
            }
            if (binds.length() > 0) {
                this.rows.add(new Row(displayName(font, feature.getName(), unit), binds.toString(), feature.isEnabled()));
            }
        }
        if (this.rows.isEmpty() && showcase(mc)) {
            this.rows.add(new Row(displayName(font, "TriggerBot", unit), "F", false));
            this.rows.add(new Row(displayName(font, "ESP", unit), "Space", true));
            this.rows.add(new Row(displayName(font, "FullBright", unit), "Z + X", false));
        }
    }

    private String displayName(MsdfFont font, String name, float unit) {
        if (this.style.header()) {
            return name;
        }
        float maxWidth = MINI_NAME_MAX_WIDTH * unit;
        float textSize = ROW_TEXT_SIZE * unit;
        float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
        if (font.measureWidth(name, textSize, letterSpacing) <= maxWidth) {
            return name;
        }
        for (int length = name.length() - 1; length > 0; length--) {
            String truncated = name.substring(0, length) + "...";
            if (font.measureWidth(truncated, textSize, letterSpacing) <= maxWidth) {
                return truncated;
            }
        }
        return name.substring(0, 1) + "...";
    }
}
