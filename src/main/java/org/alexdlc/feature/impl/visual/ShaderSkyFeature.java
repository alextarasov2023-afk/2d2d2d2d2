package org.alexdlc.feature.impl.visual;

import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.FeatureManager;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.feature.setting.ColorMode;
import org.alexdlc.feature.setting.ColorSetting;
import org.alexdlc.feature.setting.ModeSetting;
import org.alexdlc.feature.setting.NumberSetting;

public final class ShaderSkyFeature extends Feature {
    public static final String SKY_DEEP_SPACE = "Deep Space";
    public static final String SKY_NEBULA = "Nebula";
    public static final String SKY_PLASMA = "Plasma";
    public static final String SKY_AURORA = "Aurora";
    public static final String SKY_GALAXY = "Galaxy";
    public static final String SKY_SUNSET = "Sunset";

    public final ModeSetting shader = register(new ModeSetting(
            "Shader",
            SKY_DEEP_SPACE,
            SKY_DEEP_SPACE,
            SKY_NEBULA,
            SKY_PLASMA,
            SKY_AURORA,
            SKY_GALAXY,
            SKY_SUNSET
    ).configKey("render.worldtweaks.skyEffect"));
    public final ModeSetting colorMode = register(ColorMode.setting()
            .configKey("render.worldtweaks.skyColorMode"));
    public final ColorSetting color1 = register(new ColorSetting(
            "Primary Color",
            0xFF0A1026
    ).configKey("render.worldtweaks.skyColor1")
            .visibleWhen(() -> ColorMode.isCustom(this.colorMode)));
    public final ColorSetting color2 = register(new ColorSetting(
            "Accent Color",
            0xFF6FE3FF
    ).configKey("render.worldtweaks.skyColor2")
            .visibleWhen(() -> ColorMode.isCustom(this.colorMode)));
    public final NumberSetting speed = register(new NumberSetting(
            "Speed",
            1.0D,
            0.1D,
            4.0D,
            0.05D,
            "x"
    ).configKey("render.worldtweaks.skySpeed"));
    public final NumberSetting intensity = register(new NumberSetting(
            "Intensity",
            1.0D,
            0.2D,
            2.5D,
            0.01D,
            "x"
    ).configKey("render.worldtweaks.skyIntensity"));

    public ShaderSkyFeature() {
        super("ShaderSky", "Replaces the sky with a beautiful animated shader", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }

    public static ShaderSkyFeature getEnabled() {
        return FeatureManager.INSTANCE.getEnabled(ShaderSkyFeature.class);
    }

    public int resolvedColor1() {
        if (ColorMode.isSync(this.colorMode)) {
            return 0xFF0A1026;
        }
        return this.color1.getValue();
    }

    public int resolvedColor2() {
        if (ColorMode.isSync(this.colorMode)) {
            return 0xFF6FE3FF;
        }
        return this.color2.getValue();
    }
}
