package org.alexdlc.hud;

import net.minecraft.client.Minecraft;
import org.alexdlc.feature.FeatureManager;
import org.alexdlc.feature.impl.pve.MineHelperFeature;
import org.alexdlc.menu.i18n.MenuText;
import org.alexdlc.pve.mining.MineTimer;
import org.alexdlc.utils.ColorUtil;
import org.alexdlc.utils.render.Theme;
import org.alexdlc.utils.render.gui.MsdfFont;
import org.alexdlc.utils.render.gui.Render2DUtil;
import org.alexdlc.utils.render.gui.UiFontStyle;
import org.alexdlc.utils.render.gui.UiFonts;

public final class MineTimerElement extends HudElement {
    private static final float PADDING_X = 9.0F;
    private static final float PADDING_Y = 7.0F;
    private static final float TEXT_SIZE = 9.0F;
    private static final float LINE_HEIGHT = 12.0F;

    private String mineText;
    private String timeText;

    public MineTimerElement() {
        super("mine_timer", "Mine Timer");
    }

    @Override
    protected float defaultX(float unit) {
        return 280.0F * unit;
    }

    @Override
    protected float defaultY(float unit) {
        return 24.0F * unit;
    }

    @Override
    protected boolean preservePositionOnContentResize() {
        return true;
    }

    @Override
    protected void layout(Minecraft mc, float unit) {
        MineHelperFeature helper =
                FeatureManager.INSTANCE.getFeature(MineHelperFeature.class);
        MineTimer timer = helper == null
                ? null
                : helper.getCurrentTimer().orElse(null);
        boolean configured = helper != null && helper.isMineTimerSelected();
        if ((!configured || timer == null) && !showcase(mc)) {
            this.width = 0.0F;
            this.height = 0.0F;
            return;
        }

        this.mineText = MenuText.ui("Next mine") + ": "
                + (timer == null ? MenuText.ui("Diamond") : timer.nextType());
        this.timeText = MenuText.ui("Time left") + ": "
                + (timer == null
                ? "01:30"
                : timer.formattedTime(System.currentTimeMillis()));

        MsdfFont font = UiFonts.sfProDisplay();
        float textSize = TEXT_SIZE * unit;
        float spacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
        this.width = Math.max(
                font.measureWidth(this.mineText, textSize, spacing),
                font.measureWidth(this.timeText, textSize, spacing)
        ) + PADDING_X * 2.0F * unit;
        this.height = (PADDING_Y * 2.0F + LINE_HEIGHT * 2.0F) * unit;
    }

    @Override
    protected void draw(Minecraft mc, float unit) {
        float alpha = appearAlpha();
        Render2DUtil.rect(this.x, this.y, this.width, this.height)
                .glass(alpha, 0.5F * unit, 8.0F * unit)
                .radius(8.0F * unit)
                .draw();

        MsdfFont font = UiFonts.sfProDisplay();
        float textSize = TEXT_SIZE * unit;
        float firstCenterY = this.y + (PADDING_Y + LINE_HEIGHT / 2.0F) * unit;
        Render2DUtil.text(
                        this.x + PADDING_X * unit,
                        font.centeredTextY(firstCenterY, textSize),
                        textSize,
                        this.mineText
                )
                .style(UiFontStyle.MEDIUM)
                .color(ColorUtil.multiplyAlpha(Theme.getAccent(), alpha))
                .draw();
        Render2DUtil.text(
                        this.x + PADDING_X * unit,
                        font.centeredTextY(firstCenterY + LINE_HEIGHT * unit, textSize),
                        textSize,
                        this.timeText
                )
                .style(UiFontStyle.MEDIUM)
                .color(ColorUtil.multiplyAlpha(Theme.Colors.TEXT_TEXT, alpha))
                .draw();
    }
}
