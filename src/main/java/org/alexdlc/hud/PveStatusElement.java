package org.alexdlc.hud;

import net.minecraft.client.Minecraft;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.FeatureManager;
import org.alexdlc.feature.impl.pve.AutoMineFeature;
import org.alexdlc.feature.impl.pve.PveManagerFeature;
import org.alexdlc.menu.i18n.MenuText;
import org.alexdlc.utils.ColorUtil;
import org.alexdlc.utils.render.Textures;
import org.alexdlc.utils.render.Theme;
import org.alexdlc.utils.render.gui.MsdfFont;
import org.alexdlc.utils.render.gui.Render2DUtil;
import org.alexdlc.utils.render.gui.UiFontStyle;
import org.alexdlc.utils.render.gui.UiFonts;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PveStatusElement extends HudElement {
    private static final float PADDING = 8.0F;
    private static final float ICON_SIZE = 14.0F;
    private static final float TITLE_SIZE = 11.0F;
    private static final float ROW_SIZE = 9.0F;
    private static final float ROW_HEIGHT = 13.0F;
    private static final float MIN_WIDTH = 168.0F;

    private record Row(String label, String value, boolean accent) {
    }

    private final List<Row> rows = new ArrayList<>();

    public PveStatusElement() {
        super("pve_status", "PvE State");
    }

    @Override
    protected float defaultY(float unit) {
        return 170.0F * unit;
    }

    @Override
    protected boolean preservePositionOnContentResize() {
        return true;
    }

    @Override
    protected void layout(Minecraft mc, float unit) {
        rebuildRows();
        MsdfFont font = UiFonts.sfProDisplay();
        float rowSize = ROW_SIZE * unit;
        float spacing = rowSize * UiFontStyle.MEDIUM.letterSpacingEm();
        float contentWidth = MIN_WIDTH * unit;
        for (Row row : this.rows) {
            float measured = font.measureWidth(
                    row.label() + "  " + row.value(),
                    rowSize,
                    spacing
            );
            contentWidth = Math.max(contentWidth, measured + PADDING * 2.0F * unit);
        }
        this.width = contentWidth;
        this.height = (PADDING * 2.0F + 18.0F
                + this.rows.size() * ROW_HEIGHT) * unit;
    }

    @Override
    protected void draw(Minecraft mc, float unit) {
        float alpha = appearAlpha();
        Render2DUtil.rect(this.x, this.y, this.width, this.height)
                .glass(alpha, 0.5F * unit, 8.0F * unit)
                .radius(8.0F * unit)
                .draw();

        MsdfFont font = UiFonts.sfProDisplay();
        float iconSize = ICON_SIZE * unit;
        float titleSize = TITLE_SIZE * unit;
        float headerCenterY = this.y + (PADDING + 7.0F) * unit;
        Render2DUtil.texture(
                        this.x + PADDING * unit,
                        headerCenterY - iconSize / 2.0F,
                        iconSize,
                        iconSize,
                        Textures.Icons.OPTION
                )
                .color(ColorUtil.multiplyAlpha(Theme.getAccent(), alpha))
                .draw();
        Render2DUtil.text(
                        this.x + (PADDING + ICON_SIZE + 6.0F) * unit,
                        font.centeredTextY(headerCenterY, titleSize),
                        titleSize,
                        MenuText.ui("PvE State")
                )
                .style(UiFontStyle.SEMIBOLD)
                .color(ColorUtil.multiplyAlpha(Theme.Colors.TEXT_TITLE, alpha))
                .draw();

        float cursorY = this.y + (PADDING + 18.0F) * unit;
        float rowSize = ROW_SIZE * unit;
        for (Row row : this.rows) {
            float centerY = cursorY + ROW_HEIGHT * unit / 2.0F;
            Render2DUtil.text(
                            this.x + PADDING * unit,
                            font.centeredTextY(centerY, rowSize),
                            rowSize,
                            row.label()
                    )
                    .style(UiFontStyle.MEDIUM)
                    .color(ColorUtil.multiplyAlpha(
                            row.accent() ? Theme.getAccent() : Theme.Colors.TEXT_TEXT,
                            alpha
                    ))
                    .draw();
            float valueWidth = font.measureWidth(
                    row.value(),
                    rowSize,
                    rowSize * UiFontStyle.MEDIUM.letterSpacingEm()
            );
            Render2DUtil.text(
                            this.x + this.width - PADDING * unit - valueWidth,
                            font.centeredTextY(centerY, rowSize),
                            rowSize,
                            row.value()
                    )
                    .style(UiFontStyle.MEDIUM)
                    .color(ColorUtil.multiplyAlpha(Theme.Colors.SECONDARY_DARK, alpha))
                    .draw();
            cursorY += ROW_HEIGHT * unit;
        }
    }

    private void rebuildRows() {
        this.rows.clear();
        AutoMineFeature autoMine =
                FeatureManager.INSTANCE.getFeature(AutoMineFeature.class);
        if (autoMine == null) {
            this.rows.add(new Row("AutoMine", MenuText.ui("In development"), false));
        } else {
            AutoMineFeature.Status status = autoMine.currentStatus();
            this.rows.add(new Row(
                    "AutoMine",
                    status.active()
                            ? displayPhase(status.phase())
                            : MenuText.ui("Disabled"),
                    status.active()
            ));
            this.rows.add(new Row(
                    MenuText.ui("Mined ores"),
                    Integer.toString(status.minedOres()),
                    false
            ));
            this.rows.add(new Row(
                    MenuText.ui("Known targets"),
                    Integer.toString(status.knownTargets()),
                    false
            ));
            this.rows.add(new Row(
                    MenuText.ui("Elapsed"),
                    formatDuration(status.elapsedMillis()),
                    false
            ));
            this.rows.add(new Row(
                    MenuText.ui("Mine profile"),
                    status.profile(),
                    false
            ));
            this.rows.add(new Row(
                    MenuText.ui("Tool profile"),
                    status.toolProfile(),
                    false
            ));
        }

        for (Feature feature :
                FeatureManager.INSTANCE.getFeatures(FeatureCategory.PVE)) {
            if (feature == autoMine
                    || feature == PveManagerFeature.INSTANCE
                    || !feature.isEnabled()) {
                continue;
            }
            this.rows.add(new Row(
                    feature.getName(),
                    MenuText.ui("In development"),
                    false
            ));
        }
    }

    private static String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1_000L);
        long hours = totalSeconds / 3_600L;
        long minutes = totalSeconds % 3_600L / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format(
                    Locale.ROOT,
                    "%02d:%02d:%02d",
                    hours,
                    minutes,
                    seconds
            );
        }
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private static String displayPhase(String phase) {
        if (phase != null && phase.startsWith("Paused:")) {
            String reason = phase.substring("Paused:".length()).trim();
            return MenuText.ui("Paused") + ": " + MenuText.ui(reason);
        }
        return MenuText.ui(phase);
    }
}
