package org.alexdlc.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.alexdlc.utils.ColorUtil;
import org.alexdlc.utils.render.Theme;
import org.alexdlc.utils.render.gui.MsdfFont;
import org.alexdlc.utils.render.gui.Render2DUtil;
import org.alexdlc.utils.render.gui.UiFontStyle;
import org.alexdlc.utils.render.gui.UiFonts;
import org.alexdlc.utils.render.Textures;

public final class CoordsElement extends HudElement {

    private static final float ICON_SIZE = 16.0F;
    private static final float TEXT_SIZE = 10.0F;

    private record Style(
            float height,
            float radius,
            float padding,
            boolean showIcon,
            float labelValueGap,
            float dividerGap,
            float dividerHeight
    ) {
    }

    private static final Style MINI = new Style(28.0F, 8.0F, 8.0F, false, 4.0F, 8.0F, 12.0F);

    private static final String[] AXES = {"X", "Y", "Z"};

    private final String[] values = new String[3];
    private final Style style = MINI;

    public CoordsElement() {
        super("coords", "Coordinates");
    }

    @Override
    protected float defaultY(float unit) {
        return 72.0F * unit;
    }

    @Override
    protected void layout(Minecraft mc, float unit) {
        this.values[0] = coordinate(mc.player.getX());
        this.values[1] = coordinate(mc.player.getY());
        this.values[2] = coordinate(mc.player.getZ());

        MsdfFont font = UiFonts.sfProDisplay();
        float textSize = TEXT_SIZE * unit;
        float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();

        float width = this.style.padding() * unit;
        if (this.style.showIcon()) {
            width += ICON_SIZE * unit;
        }
        for (int i = 0; i < AXES.length; i++) {
            if (hasDivider(i)) {
                width += (this.style.dividerGap() * 2.0F + 1.0F) * unit;
            }
            width += font.measureWidth(AXES[i], textSize, letterSpacing);
            width += this.style.labelValueGap() * unit;
            width += font.measureWidth(this.values[i], textSize, letterSpacing);
        }
        this.width = width + this.style.padding() * unit;
        this.height = this.style.height() * unit;
    }

    @Override
    protected void draw(Minecraft mc, float unit) {
        float alpha = appearAlpha();
        Render2DUtil.rect(this.x, this.y, this.width, this.height)
                .glass(alpha, 0.5F * unit, 8.0F * unit)
                .radius(this.style.radius() * unit)
                .draw();

        MsdfFont font = UiFonts.sfProDisplay();
        float textSize = TEXT_SIZE * unit;
        float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
        float centerY = this.y + this.height / 2.0F;
        float textY = font.centeredTextY(centerY, textSize);

        float cursor = this.x + this.style.padding() * unit;
        if (this.style.showIcon()) {
            float iconSize = ICON_SIZE * unit;
            Render2DUtil.texture(cursor, centerY - iconSize / 2.0F, iconSize, iconSize, Textures.Icons.MOVE_3D)
                    .color(ColorUtil.multiplyAlpha(Theme.Colors.ICON_GHOST, alpha))
                    .draw();
            cursor += iconSize;
        }

        for (int i = 0; i < AXES.length; i++) {
            if (hasDivider(i)) {
                cursor = divider(cursor, centerY, this.style.dividerHeight(), this.style.dividerGap(), unit, alpha);
            }

            Render2DUtil.text(cursor, textY, textSize, AXES[i])
                    .style(UiFontStyle.MEDIUM)
                    .color(ColorUtil.multiplyAlpha(Theme.Colors.ICON, alpha))
                    .draw();
            cursor += font.measureWidth(AXES[i], textSize, letterSpacing) + this.style.labelValueGap() * unit;

            Render2DUtil.text(cursor, textY, textSize, this.values[i])
                    .style(UiFontStyle.MEDIUM)
                    .color(ColorUtil.multiplyAlpha(Theme.Colors.TEXT_TEXT, alpha))
                    .draw();
            cursor += font.measureWidth(this.values[i], textSize, letterSpacing);
        }
    }

    private boolean hasDivider(int index) {
        return index > 0 || this.style.showIcon();
    }

    private static String coordinate(double value) {
        return Integer.toString((int) Math.floor(value));
    }
}
