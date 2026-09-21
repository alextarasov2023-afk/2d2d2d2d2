package org.alexdlc.feature.impl.visual;

import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.feature.setting.BooleanSetting;
import org.alexdlc.feature.setting.ColorMode;
import org.alexdlc.feature.setting.ColorSetting;
import org.alexdlc.feature.setting.ModeSetting;
import org.alexdlc.feature.setting.NumberSetting;

public final class OutlineHandsFeature extends Feature {
    public final ModeSetting colorMode = register(ColorMode.setting()
            .configKey("render.outlinehands.colorMode"));

    public final ColorSetting color = register(new ColorSetting(
            "Color",
            0xFF7986CB
    ).configKey("render.outlinehands.color")
            .visibleWhen(() -> ColorMode.isCustom(this.colorMode)));

    public final NumberSetting outlineThickness = register(new NumberSetting(
            "Outline Thickness",
            2.0D,
            0.5D,
            6.0D,
            0.1D,
            "px"
    ).configKey("render.outlinehands.thickness"));

    public final NumberSetting opacity = register(new NumberSetting(
            "Opacity",
            1.0D,
            0.0D,
            1.0D,
            0.01D,
            ""
    ).configKey("render.outlinehands.opacity"));

    public final BooleanSetting bothHands = register(new BooleanSetting(
            "Both Hands",
            true
    ).configKey("render.outlinehands.bothHands"));

    public OutlineHandsFeature() {
        super("OutlineHands", "Renders a crisp colored outline around your first-person hands", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }

    public int resolvedColor() {
        return ColorMode.resolve(this.colorMode, this.color);
    }
}
