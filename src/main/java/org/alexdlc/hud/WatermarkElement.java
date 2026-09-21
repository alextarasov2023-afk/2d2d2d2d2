package org.alexdlc.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import org.alexdlc.utils.ColorUtil;
import org.alexdlc.utils.render.Theme;
import org.alexdlc.utils.render.gui.MsdfFont;
import org.alexdlc.utils.render.gui.PlayerHead;
import org.alexdlc.utils.render.gui.Render2DUtil;
import org.alexdlc.utils.render.gui.UiFontStyle;
import org.alexdlc.utils.render.gui.UiFonts;

import java.util.ArrayList;
import java.util.List;
import org.alexdlc.utils.render.Textures;

public final class WatermarkElement extends HudElement {

    private static final int LOGO_BACKGROUND = ColorUtil.rgb(37, 37, 39);

    private static final float MINI_SCALE_DIVISOR = 1.175F;
    private static final float LOGO_ASPECT = 19.3711F / 20.8607F;
    private static final float ICON_SIZE = 16.0F;
    private static final float ICON_TEXT_GAP = 8.0F;
    private static final float DIVIDER_GAP = 12.0F;
    private static final float TEXT_SIZE = 12.0F;

    private record Style(
            float height,
            float radius,
            float padding,
            float logoSize,
            float logoIconSize,
            float headSize,
            float contentGap,
            float dividerHeight
    ) {
    }

    private static final Style MINI = new Style(32.0F, 8.0F, 8.0F, 32.0F, 16.0F, 18.0F, 8.0F, 14.0F);

    private record Section(Identifier icon, float iconSize, String text) {
    }

    private final List<Section> sections = new ArrayList<>(4);
    private final Style style = MINI;

    public WatermarkElement() {
        super("watermark", "Watermark");
    }

    @Override
    protected boolean preservePositionOnContentResize() {
        return true;
    }

    @Override
    protected void layout(Minecraft mc, float unit) {
        unit = scaledUnit(unit);
        this.sections.clear();
        this.sections.add(new Section(null, this.style.headSize(), mc.getUser().getName()));
        this.sections.add(new Section(Textures.Icons.GAMEPAD, ICON_SIZE, serverAddress(mc)));
        this.sections.add(new Section(Textures.Icons.HARD_DRIVE, ICON_SIZE, mc.getFps() + " FPS"));
        this.sections.add(new Section(Textures.Icons.CHEVRONS_LEFT_RIGHT_ELLIPSIS, ICON_SIZE, latency(mc) + "ms"));

        MsdfFont font = UiFonts.sfProDisplay();
        float textSize = TEXT_SIZE * unit;
        float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();

        float width = (this.style.logoSize() + this.style.contentGap()) * unit;
        for (int i = 0; i < this.sections.size(); i++) {
            if (i > 0) {
                width += (DIVIDER_GAP * 2.0F + 1.0F) * unit;
            }
            Section section = this.sections.get(i);
            width += (section.iconSize() + ICON_TEXT_GAP) * unit;
            width += font.measureWidth(section.text(), textSize, letterSpacing);
        }
        this.width = width + this.style.padding() * unit;
        this.height = this.style.height() * unit;
    }

    @Override
    protected void draw(Minecraft mc, float unit) {
        unit = scaledUnit(unit);
        float alpha = appearAlpha();
        Render2DUtil.rect(this.x, this.y, this.width, this.height)
                .glass(alpha, 0.5F * unit, 8.0F * unit)
                .radius(this.style.radius() * unit)
                .draw();

        drawLogo(unit, alpha);

        MsdfFont font = UiFonts.sfProDisplay();
        float textSize = TEXT_SIZE * unit;
        float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
        float centerY = this.y + this.height / 2.0F;
        float cursor = this.x + (this.style.logoSize() + this.style.contentGap()) * unit;

        for (int i = 0; i < this.sections.size(); i++) {
            if (i > 0) {
                cursor = divider(cursor, centerY, this.style.dividerHeight(), DIVIDER_GAP, unit, alpha);
            }

            Section section = this.sections.get(i);
            float iconSize = section.iconSize() * unit;
            float iconY = centerY - iconSize / 2.0F;
            if (section.icon() == null) {
                drawPlayerHead(mc, cursor, iconY, iconSize, alpha);
            } else {
                Render2DUtil.texture(cursor, iconY, iconSize, iconSize, section.icon())
                        .color(ColorUtil.multiplyAlpha(Theme.Colors.TEXT_GHOST, alpha))
                        .draw();
            }
            cursor += iconSize + ICON_TEXT_GAP * unit;

            Render2DUtil.text(cursor, font.centeredTextY(centerY, textSize), textSize, section.text())
                    .style(UiFontStyle.MEDIUM)
                    .color(ColorUtil.multiplyAlpha(Theme.Colors.PRIMARY_BRIGHT, alpha))
                    .draw();
            cursor += font.measureWidth(section.text(), textSize, letterSpacing);
        }
    }

    private void drawLogo(float unit, float alpha) {
        float size = this.style.logoSize() * unit;
        Render2DUtil.rect(this.x, this.y, size, size)
                .color(ColorUtil.multiplyAlpha(LOGO_BACKGROUND, alpha))
                .radius(this.style.radius() * unit)
                .draw();
        float iconHeight = this.style.logoIconSize() * unit;
        float iconWidth = iconHeight * LOGO_ASPECT;
        float iconX = this.x + (size - iconWidth) / 2.0F;
        float iconY = this.y + (size - iconHeight) / 2.0F;
        Render2DUtil.texture(iconX, iconY, iconWidth, iconHeight, Textures.Logos.BOLT)
                .color(ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha))
                .draw();
    }

    private void drawPlayerHead(Minecraft mc, float x, float y, float size, float alpha) {
        if (mc.player == null) {
            return;
        }
        PlayerHead.draw(x, y, size, mc.player.getSkin().body().texturePath(),
                size * 0.2F, ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha));
    }

    private static String serverAddress(Minecraft mc) {
        return mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "Singleplayer";
    }

    private static int latency(Minecraft mc) {
        if (mc.getConnection() == null || mc.player == null) {
            return 0;
        }
        PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return info != null ? info.getLatency() : 0;
    }

    private float scaledUnit(float unit) {
        return unit / MINI_SCALE_DIVISOR;
    }
}
