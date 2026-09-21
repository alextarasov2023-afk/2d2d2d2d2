package org.alexdlc.feature.impl.visual;

import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.feature.setting.BooleanSetting;
import org.alexdlc.feature.setting.ColorMode;
import org.alexdlc.feature.setting.ColorSetting;
import org.alexdlc.feature.setting.ModeSetting;
import org.alexdlc.feature.setting.NumberSetting;

public final class GlowHandsFeature extends Feature {
    public static final String COLOR_ITEM = "Item";
    public static final String COLOR_CUSTOM = "Custom";

    public final NumberSetting intensity = register(new NumberSetting(
            "Intensity",
            1.0D,
            0.1D,
            3.0D,
            0.01D,
            "x"
    ).configKey("render.glowhands.intensity"));
    public final NumberSetting radius = register(new NumberSetting(
            "Radius",
            26.0D,
            4.0D,
            80.0D,
            1.0D,
            "px"
    ).configKey("render.glowhands.radius"));
    public final BooleanSetting trail = register(new BooleanSetting(
            "Smooth Trail",
            true
    ).configKey("render.glowhands.trail"));
    public final NumberSetting trailLength = register(new NumberSetting(
            "Trail Length",
            0.6D,
            0.0D,
            1.0D,
            0.01D,
            ""
    ).configKey("render.glowhands.trailLength")
            .visibleWhen(() -> this.trail.getValue()));
    public final ModeSetting colorMode = register(new ModeSetting(
            "Color Mode",
            COLOR_ITEM,
            COLOR_ITEM,
            COLOR_CUSTOM
    ).configKey("render.glowhands.colorMode"));
    public final ColorSetting color = register(new ColorSetting(
            "Glow Color",
            0xFF8FD9FF
    ).configKey("render.glowhands.color")
            .visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM)));
    public final BooleanSetting bothHands = register(new BooleanSetting(
            "Both Hands",
            true
    ).configKey("render.glowhands.bothHands"));

    public GlowHandsFeature() {
        super("GlowHands", "Hands emit a strong glow that follows the held item color, with a smooth trail", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }

    public boolean usesItemColor() {
        return this.colorMode.is(COLOR_ITEM);
    }

    public int customColor() {
        return this.color.getValue();
    }
}
