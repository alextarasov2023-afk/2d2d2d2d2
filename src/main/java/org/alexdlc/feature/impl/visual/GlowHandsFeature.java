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
    public static final String MODE_NEON = "Neon";
    public static final String MODE_FLAME = "Flame";

    public final ModeSetting mode = register(new ModeSetting(
            "Mode",
            MODE_NEON,
            MODE_NEON,
            MODE_FLAME
    ).configKey("render.glowhands.mode"));

    public final ModeSetting colorMode = register(ColorMode.setting()
            .configKey("render.glowhands.colorMode"));

    public final ColorSetting color = register(new ColorSetting(
            "Color",
            0xFF7986CB
    ).configKey("render.glowhands.color")
            .visibleWhen(() -> ColorMode.isCustom(this.colorMode)));

    public final NumberSetting glowRadius = register(new NumberSetting(
            "Glow Radius",
            18.0D,
            1.0D,
            50.0D,
            1.0D,
            "px"
    ).configKey("render.glowhands.radius"));

    public final NumberSetting glowStrength = register(new NumberSetting(
            "Glow Strength",
            1.0D,
            0.1D,
            3.0D,
            0.05D,
            "x"
    ).configKey("render.glowhands.strength"));

    public final NumberSetting flameSpeed = register(new NumberSetting(
            "Flame Speed",
            1.2D,
            0.1D,
            5.0D,
            0.1D,
            "x"
    ).configKey("render.glowhands.flameSpeed").visibleWhen(() -> this.mode.is(MODE_FLAME)));

    public final NumberSetting flameTrail = register(new NumberSetting(
            "Flame Trail",
            0.85D,
            0.0D,
            1.0D,
            0.01D,
            ""
    ).configKey("render.glowhands.flameTrail").visibleWhen(() -> this.mode.is(MODE_FLAME)));

    public final BooleanSetting bothHands = register(new BooleanSetting(
            "Both Hands",
            true
    ).configKey("render.glowhands.bothHands"));

    public GlowHandsFeature() {
        super("GlowHands", "Renders an ethereal outer glow or flame aura around your hands", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }

    public int resolvedColor() {
        return ColorMode.resolve(this.colorMode, this.color);
    }
}
