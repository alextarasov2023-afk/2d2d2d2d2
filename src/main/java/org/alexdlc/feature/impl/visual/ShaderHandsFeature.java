package org.alexdlc.feature.impl.visual;

import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.feature.setting.BooleanSetting;
import org.alexdlc.feature.setting.ColorMode;
import org.alexdlc.feature.setting.ColorSetting;
import org.alexdlc.feature.setting.ModeSetting;
import org.alexdlc.feature.setting.NumberSetting;

public final class ShaderHandsFeature extends Feature {
    public static final String SOLID = "Solid";
    public static final String PLASMA = "Plasma";
    public static final String NEBULA = "Nebula";
    public static final String PRISMATIC = "Prismatic";
    public static final String GLASS = "Glass";

    public final ModeSetting fillType = register(new ModeSetting(
            "Fill Type",
            PLASMA,
            SOLID,
            PLASMA,
            NEBULA,
            PRISMATIC,
            GLASS
    ).configKey("render.shaderhands.type"));

    public final ModeSetting colorMode = register(ColorMode.setting()
            .configKey("render.shaderhands.colorMode"));

    public final ColorSetting color = register(new ColorSetting(
            "Color",
            0xFF7986CB
    ).configKey("render.shaderhands.color")
            .visibleWhen(() -> ColorMode.isCustom(this.colorMode)));

    public final NumberSetting opacity = register(new NumberSetting(
            "Opacity",
            0.65D,
            0.0D,
            1.0D,
            0.01D,
            ""
    ).configKey("render.shaderhands.opacity"));

    public final NumberSetting shaderSpeed = register(new NumberSetting(
            "Shader Speed",
            1.0D,
            0.1D,
            5.0D,
            0.1D,
            "x"
    ).configKey("render.shaderhands.speed").visibleWhen(() -> !this.fillType.is(SOLID) && !this.fillType.is(GLASS)));

    public final NumberSetting glassBlur = register(new NumberSetting(
            "Glass Blur",
            12.0D,
            0.0D,
            60.0D,
            1.0D,
            "px"
    ).configKey("render.shaderhands.glassBlur").visibleWhen(() -> this.fillType.is(GLASS)));

    public final BooleanSetting glassMirror = register(new BooleanSetting(
            "Glass Mirror",
            true
    ).configKey("render.shaderhands.glassMirror").visibleWhen(() -> this.fillType.is(GLASS)));

    public final BooleanSetting bothHands = register(new BooleanSetting(
            "Both Hands",
            true
    ).configKey("render.shaderhands.bothHands"));

    public ShaderHandsFeature() {
        super("ShaderHands", "Fills your first-person hands with stunning animated shader effects", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }

    public int resolvedColor() {
        return ColorMode.resolve(this.colorMode, this.color);
    }
}
